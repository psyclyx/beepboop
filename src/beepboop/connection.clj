(ns beepboop.connection
  (:require
    [beepboop.ansi :as ansi]
    [beepboop.draw :as draw]
    [beepboop.server :as server]
    [beepboop.telnet :as telnet]
    [clojure.string :as str]
    [clojure.tools.logging :as log]))


(declare handle-packet)
(declare handle-disconnect)
(declare handle-event)
(declare render)


(def ansi-clear         "\033[2J")
(def ansi-reset-cursor  "\033[H")


(defn handle-connect
  [channel]
  (log/info "New connection")
  (telnet/initialize channel)
  (let [connection (atom nil)]
    (reset! connection {:channel channel
                        :handle-packet handle-packet
                        :handle-disconnect handle-disconnect
                        :event-sink (telnet/command-filter
                                      (ansi/bytes-to-chars-filter
                                        (ansi/escape-codes-filter
                                          #(handle-event @connection %))))
                        :canvas (draw/make-canvas)})
    @connection))


(defn handle-disconnect
  [_connection]
  (log/info "Connection closed"))


(defn handle-event
  [{:keys [canvas] :as connection} {:keys [type] :as event}]
  (case type
    :input (log/info "Got char" (get event :char))
    :screen-size (do (draw/set-size canvas (get event :size))
                     (render connection))
    :arrow (let [[x y] (draw/get-cursor-position canvas)]
             (draw/set-cursor-position canvas
                                       (case (get event :direction)
                                         :left   [(- x 1) y]
                                         :right  [(+ x 1) y]
                                         :up     [x (- y 1)]
                                         :down   [x (+ y 1)]))
             (render connection))
    (log/info "Unhandled event" type)))


(defn handle-packet
  [{:keys [:event-sink] :as _connection} input-bytes]
  (doseq [byte input-bytes]
    (event-sink {:type :input-byte :byte byte})))


(defn send-frame
  [{:keys [channel canvas] :as _connection}]
  (log/info "Sending frame")
  (let [[cursor-x cursor-y] (draw/get-cursor-position canvas)]
    ;; TODO: save previous canvas and only send diff
    (server/send-message channel (str ansi-clear
                                      ansi-reset-cursor
                                      (str/join "\n\r" (draw/get-contents canvas))
                                      "\033[" (+ cursor-y 1) ";" (+ cursor-x 1) "H"))))


(defn render
  [{:keys [canvas] :as connection}]
  (let [[width height] (draw/get-size canvas)]
    (draw/box canvas [0 0] [width height])
    (draw/box canvas [5 3] [(quot width 2) 3])
    (draw/rect canvas "█" [7 4] [(- (quot width 2) 4) 1])
    (draw/shape canvas [6 3] [" Welcome "]))
  (send-frame connection))
