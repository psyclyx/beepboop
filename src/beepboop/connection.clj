(ns beepboop.connection
  (:require
    [beepboop.server :as server]
    [clojure.string :as str]
    [clojure.tools.logging :as log]))


(declare handle-packet)
(declare handle-disconnect)
(declare draw-all)


(def telnet-iac               (unchecked-byte 0xFF))
(def telnet-will              (unchecked-byte 0xFB))
(def telnet-wont              (unchecked-byte 0xFC))
(def telnet-do                (unchecked-byte 0xFD))
(def telnet-dont              (unchecked-byte 0xFE))
(def telnet-echo              (unchecked-byte 0x01))
(def telnet-suppress-go-ahead (unchecked-byte 0x03))
(def telnet-linemode          (unchecked-byte 0x22))
(def telnet-sb                (unchecked-byte 0xFA))
(def telnet-se                (unchecked-byte 0xF0))
(def telnet-naws              (unchecked-byte 0x1F))


(def ansi-clear         "\033[2J")
(def ansi-reset-cursor  "\033[H")


;; See https://github.com/seanmiddleditch/libtelnet/blob/5f5ecee776b9bdaa4e981e5f807079a9c79d633e/libtelnet.c#L973
(defn make-telnet-filter
  []
  (let [state (atom :data)
        sb-type (atom nil)
        sb-buffer (atom nil)]
    (fn [byte meta-cb data-cb]
      (case @state
        :data (cond
                (= byte telnet-iac) (reset! state :iac)
                :else (data-cb byte))
        :iac (cond
               (= byte telnet-iac) (do (data-cb telnet-iac) ; escaped IAC
                                       (reset! state :data))
               (= byte telnet-sb) (reset! state :sb)
               (= byte telnet-do) (reset! state :do)
               (= byte telnet-dont) (reset! state :dont)
               (= byte telnet-will) (reset! state :will)
               (= byte telnet-wont) (reset! state :wont)
               :else (do (log/info "Telnet command " byte)
                         (reset! state :data)))
        (:do :dont :will :wont) (do (log/info "Telnet command" @state byte)
                                    (reset! state :data))
        :sb (do (cond
                  (= byte telnet-naws) (reset! sb-type :sb-naws)
                  :else (reset! sb-type byte))
                (reset! sb-buffer (byte-array []))
                (reset! state :sb-data))
        :sb-data (cond
                   (= byte telnet-iac) (reset! state :sb-iac)
                   :else (swap! sb-buffer #(byte-array (concat % [byte]))))
        :sb-iac (cond
                  (= byte telnet-se) (do (case @sb-type
                                           :sb-naws (meta-cb {:type :screen-size
                                                              :width (bit-or (bit-shift-left (bit-and (aget @sb-buffer 0) 0xFF) 8) (bit-and (aget @sb-buffer 1) 0xFF))
                                                              :height (bit-or (bit-shift-left (bit-and (aget @sb-buffer 2) 0xFF) 8) (bit-and (aget @sb-buffer 3) 0xFF))})
                                           (log/info "Telnet SB" @sb-type))
                                         (reset! state :data))
                  :else (do (swap! sb-buffer #(byte-array (concat % [byte])))
                            (reset! state :sb-data)))))))


(defn handle-connect
  [channel]
  (log/info "New connection")
  (server/send-message channel [telnet-iac telnet-will telnet-echo
                                telnet-iac telnet-do telnet-suppress-go-ahead
                                telnet-iac telnet-will telnet-suppress-go-ahead
                                telnet-iac telnet-dont telnet-linemode
                                telnet-iac telnet-do telnet-naws])
  {:channel channel
   :handle-packet handle-packet
   :handle-disconnect handle-disconnect
   :filter-telnet (make-telnet-filter)
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


(defn draw-all
  [{:keys [channel screen-width screen-height] :as _connection}]
  (log/info "Drawing")
  (server/send-message channel (str ansi-clear ansi-reset-cursor
                                    (apply str (repeat @screen-width "-")) "\n\r"
                                    (apply str (repeat (- @screen-height 2) (str "|" (apply str (repeat (- @screen-width 2) " ")) "|\n\r")))
                                    (apply str (repeat @screen-width "-")))))
