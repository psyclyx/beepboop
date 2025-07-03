(ns beepboop.connection
  (:require
    [beepboop.server :as server]
    [clojure.string :as str]
    [clojure.tools.logging :as log]))


(declare handle-packet)
(declare handle-disconnect)


(defn handle-connect
  [channel]
  (log/info "New connection")
  (server/send-response channel "Connected!\n")
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
    (server/send-response channel (str ">" trimmed "<"))))
