(ns beepboop.connection
  (:require
    [beepboop.ansi :as ansi]
    [beepboop.command :as command]
    [beepboop.draw :as draw]
    [beepboop.game :as game]
    [beepboop.server :as server]
    [beepboop.telnet :as telnet]
    [beepboop.text-edit :as text-edit]
    [beepboop.util :as util]
    [clojure.string :as str]
    [clojure.tools.logging :as log]))


(declare handle-packet)
(declare handle-disconnect)
(declare handle-event)
(declare render)


(def ansi-clear         "\033[2J")
(def ansi-reset-cursor  "\033[H")


(defn handle-connect
  [channel game]
  (log/info "New connection")
  (telnet/initialize channel)
  (let [connection (atom nil)
        player (game/create-player game [(util/rand-range -30 30) -5])]
    (reset! connection {:channel channel
                        :handle-packet handle-packet
                        :handle-disconnect handle-disconnect
                        :event-sink (telnet/command-filter
                                      (ansi/bytes-to-chars-filter
                                        (ansi/escape-codes-filter
                                          #(handle-event @connection %))))
                        :canvas (draw/make-canvas)
                        :game game
                        :after-tick #(render @connection)
                        :is-open (atom true)
                        :player player
                        :message (atom nil)
                        :edit-view (text-edit/text-edit-view
                                     "Command"
                                     [5 3] 50
                                     #(command/handle-command @connection player game %))})
    (game/register-listener game @connection)
    @connection))


(defn handle-disconnect
  [{:keys [channel game is-open] :as connection}]
  (server/send-message channel (str ansi-clear
                                    ansi-reset-cursor))
  (game/unregister-listener game connection)
  (reset! is-open false)
  (.close channel)
  (log/info "Connection closed"))


(defn handle-event
  [{:keys [canvas edit-view] :as connection} {:keys [type] :as event}]
  ;; (log/info "Event" event)
  (case type
    :screen-size (draw/set-size canvas (get event :size))
    ((get edit-view :handle-event) edit-view event))
  (render connection))


(defn handle-packet
  [{:keys [:event-sink] :as _connection} input-bytes]
  (doseq [byte input-bytes]
    (event-sink {:type :input-byte :byte byte})))


(defn send-frame
  [{:keys [channel canvas is-open] :as _connection}]
  (when @is-open
    (let [[cursor-x cursor-y] (draw/get-cursor-position canvas)]
      ;; TODO: save previous canvas and only send diff
      (server/send-message channel (str ansi-clear
                                        ansi-reset-cursor
                                        (str/join "\n\r" (draw/get-contents canvas))
                                        "\033[" (+ cursor-y 1) ";" (+ cursor-x 1) "H")))))


(defn render
  [{:keys [canvas edit-view player message game] :as connection}]
  (let [[width height] (draw/get-size canvas)]
    (game/render game canvas [0 0])
    ((get edit-view :render) edit-view canvas)
    (draw/box canvas [0 0] [width height] draw/transparent)
    (when (and (not (get @player :alive)) (not @message))
      (reset! message "You died :("))
    (when @message
      (draw/centered-message-box canvas @message))
    (send-frame connection)))
