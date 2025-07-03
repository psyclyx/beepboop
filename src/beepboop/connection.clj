(ns beepboop.connection
  (:require
    [beepboop.server :as server]
    [clojure.string :as str]
    [clojure.tools.logging :as log]))


(declare handle-packet)
(declare handle-disconnect)


(def telnet-iac 0xFF)
(def telnet-will 0xFB)
(def telnet-do 0xFD)
(def telnet-dont 0xFE)
(def telnet-echo 0x01)
(def telnet-suppress-go-ahead 0x03)
(def telnet-linemode 0x22)


(defn handle-connect
  [channel]
  (log/info "New connection")
  (server/send-message channel [telnet-iac telnet-will telnet-echo
                                telnet-iac telnet-do telnet-suppress-go-ahead
                                telnet-iac telnet-will telnet-suppress-go-ahead
                                telnet-iac telnet-dont telnet-linemode])
  (server/send-message channel "Connected!\n")
  {:channel channel
   :handle-packet handle-packet
   :handle-disconnect handle-disconnect})


(defn handle-disconnect
  [_connection]
  (log/info "Connection closed"))


(defn handle-packet
  [{:keys [channel] :as _connection} data]
  (let [trimmed (str/trim data)]
    (log/info "Got packet:" trimmed)
    (server/send-message channel (str ">" trimmed "<"))))
