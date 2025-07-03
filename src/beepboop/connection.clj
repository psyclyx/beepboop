(ns beepboop.connection
  (:require
    [beepboop.server :as server]
    [beepboop.telnet :as telnet]
    [clojure.string :as str]
    [clojure.tools.logging :as log]))


(declare handle-packet)
(declare handle-disconnect)
(declare draw-all)


(def ansi-clear         "\033[2J")
(def ansi-reset-cursor  "\033[H")


(defn handle-connect
  [channel]
  (log/info "New connection")
  (telnet/initialize channel)
  {:channel channel
   :handle-packet handle-packet
   :handle-disconnect handle-disconnect
   :filter-telnet (telnet/make-parser)
   :screen-width (atom 80)
   :screen-height (atom 80)})


(defn handle-disconnect
  [_connection]
  (log/info "Connection closed"))


(defn handle-meta
  [{:keys [screen-width screen-height] :as connection} {:keys [type] :as meta}]
  (case type
    :screen-size (do (reset! screen-width (get meta :width))
                     (reset! screen-height (get meta :height))
                     (log/info "Screen size set to" @screen-width @screen-height)
                     (draw-all connection))
    (log/info "Unhandled meta info" type)))


(defn handle-input
  [connection byte]
  (cond
    (= byte 13) (draw-all connection))
  (log/info "Got byte" byte))


(defn handle-packet
  [{:keys [filter-telnet] :as connection} input-bytes]
  (doseq [byte input-bytes]
    (filter-telnet byte #(handle-meta connection %) #(handle-input connection %))))


(defn send-frame
  [{:keys [channel] :as _connection} canvas]
  (log/info "Sending frame")
  ;; TODO: save previous canvas and only send diff
  (server/send-message channel (str ansi-clear
                                    ansi-reset-cursor
                                    (str/join "\n\r" canvas))))


(defn draw-all
  [{:keys [screen-width screen-height] :as connection}]
  (send-frame connection (concat [(str "." (apply str (repeat (- @screen-width 2) "-")) ".")]
                                 (repeat (- @screen-height 2)
                                         (str "|" (apply str (repeat (- @screen-width 2) " ")) "|"))
                                 [(str "'" (apply str (repeat (- @screen-width 2) "-")) "'")])))
