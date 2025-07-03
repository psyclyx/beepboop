(ns beepboop.telnet
  (:require
    [beepboop.server :as server]
    [clojure.tools.logging :as log]))


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


(defn get-16-bit-int-from-bytes
  [bytes index]
  (bit-or (bit-shift-left (bit-and (aget bytes index) 0xFF) 8)
          (bit-and (aget bytes (+ index 1)) 0xFF)))


(defn parse-sb
  [type bytes]
  (case type
    :sb-naws {:type :screen-size
              :size [(get-16-bit-int-from-bytes bytes 0)
                     (get-16-bit-int-from-bytes bytes 2)]}
    (do (log/info "Telnet SB" type)
        nil)))


;; See https://github.com/seanmiddleditch/libtelnet/blob/5f5ecee776b9bdaa4e981e5f807079a9c79d633e/libtelnet.c#L973
(defn make-parser
  []
  (let [state (atom :data)
        sb-type (atom nil)
        sb-buffer (atom nil)]
    (fn [byte event-cb passthrough-cb]
      (case @state
        :data (cond
                (= byte telnet-iac) (reset! state :iac)
                :else (passthrough-cb byte))
        :iac (cond
               (= byte telnet-iac) (do (passthrough-cb telnet-iac) ; escaped IAC
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
                  (= byte telnet-se) (do (some-> (parse-sb @sb-type @sb-buffer) event-cb)
                                         (reset! sb-type nil)
                                         (reset! sb-buffer nil)
                                         (reset! state :data))
                  :else (do (swap! sb-buffer #(byte-array (concat % [byte])))
                            (reset! state :sb-data)))))))


(defn initialize
  [channel]
  (server/send-message channel [telnet-iac telnet-will telnet-echo
                                telnet-iac telnet-do telnet-suppress-go-ahead
                                telnet-iac telnet-will telnet-suppress-go-ahead
                                telnet-iac telnet-dont telnet-linemode
                                telnet-iac telnet-do telnet-naws]))
