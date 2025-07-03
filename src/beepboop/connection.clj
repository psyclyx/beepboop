(ns beepboop.connection
  (:require
    [beepboop.ansi :as ansi]
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
   :parse-telnet (telnet/make-parser)
   :parse-ansi (ansi/make-parser)
   :screen-size (atom [80 80])
   :cursor-position (atom [0 0])})


(defn handle-disconnect
  [_connection]
  (log/info "Connection closed"))


(defn set-cursor-position
  [{:keys [screen-size cursor-position] :as _connection} [x y]]
  (let [[width height] @screen-size]
    (reset! cursor-position [(max 0 (min width x))
                             (max 0 (min height y))])))


(defn handle-meta
  [{:keys [screen-size cursor-position] :as connection} {:keys [type] :as meta}]
  (case type
    :screen-size (do (reset! screen-size (get meta :size))
                     (log/info "Screen size set to" @screen-size)
                     (set-cursor-position connection @cursor-position)
                     (draw-all connection))
    :arrow (let [[x y] @cursor-position]
             (set-cursor-position connection
                                  (case (get meta :direction)
                                    :left   [(- x 1) y]
                                    :right  [(+ x 1) y]
                                    :up     [x (- y 1)]
                                    :down   [x (+ y 1)]))
             (draw-all connection))
    (log/info "Unhandled meta info" type)))


(defn handle-input
  [connection byte]
  (cond
    (= byte 13) (draw-all connection))
  (log/info "Got byte" byte))


(defn handle-packet
  [{:keys [parse-telnet parse-ansi] :as connection} input-bytes]
  (let [recv-input #(handle-input connection %)
        recv-meta #(handle-meta connection %)
        recv-ansi #(parse-ansi (char %) recv-meta recv-input)]
    (doseq [byte input-bytes]
      (parse-telnet byte recv-meta recv-ansi))))


(defn send-frame
  [{:keys [channel] :as _connection} canvas [cursor-x cursor-y]]
  (log/info "Sending frame")
  ;; TODO: save previous canvas and only send diff
  (server/send-message channel (str ansi-clear
                                    ansi-reset-cursor
                                    (str/join "\n\r" canvas)
                                    "\033[" (+ cursor-y 1) ";" (+ cursor-x 1) "H")))


(defn draw-all
  [{:keys [screen-size cursor-position] :as connection}]
  (let [[width height] @screen-size]
    (send-frame connection
                (concat [(str "╭" (apply str (repeat (- width 2) "─")) "╮")]
                        (repeat (- height 2)
                                (str "│" (apply str (repeat (- width 2) " ")) "│"))
                        [(str "╰" (apply str (repeat (- width 2) "─")) "╯")])
                @cursor-position)))
