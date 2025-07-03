(ns beepboop.connection
  (:require
    [beepboop.server :as server]
    [clojure.string :as str]
    [clojure.tools.logging :as log]))


(declare handle-packet)
(declare handle-disconnect)


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


;; See https://github.com/seanmiddleditch/libtelnet/blob/5f5ecee776b9bdaa4e981e5f807079a9c79d633e/libtelnet.c#L973
(defn make-telnet-filter
  []
  (let [state (atom :data)]
    (fn [byte passthrough]
      (case @state
        :data (cond
                (= byte telnet-iac) (reset! state :iac)
                :else (passthrough byte))
        :iac (cond
               (= byte telnet-iac) (do (passthrough telnet-iac) ; escaped IAC
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
        :sb (cond
              (= byte telnet-iac) (reset! state :sb-iac))
        :sb-iac (cond
                  (= byte telnet-se) (do (log/info "Telnet SB")
                                         (reset! state :data))
                  :else (reset! state :sb))))))


(defn handle-connect
  [channel]
  (log/info "New connection")
  (server/send-message channel [telnet-iac telnet-will telnet-echo
                                telnet-iac telnet-do telnet-suppress-go-ahead
                                telnet-iac telnet-will telnet-suppress-go-ahead
                                telnet-iac telnet-dont telnet-linemode])
  (server/send-message channel "Connected!")
  {:channel channel
   :handle-packet handle-packet
   :handle-disconnect handle-disconnect
   :filter-telnet (make-telnet-filter)})


(defn handle-disconnect
  [_connection]
  (log/info "Connection closed"))


(defn handle-input
  [_connection byte]
  (log/info "Got byte" byte))


(defn handle-packet
  [{:keys [filter-telnet] :as connection} input-bytes]
  (doseq [byte input-bytes]
    (filter-telnet byte #(handle-input connection %))))


;; (server/send-message channel (str ">" trimmed "<"))))
