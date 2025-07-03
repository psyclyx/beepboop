(ns beepboop.connection
  (:require
    [beepboop.server :as server]
    [clojure.string :as str]
    [clojure.tools.logging :as log]))


(defn handle-packet
  [client-channel data]
  (let [trimmed (str/trim data)]
    (println trimmed)
    (server/send-response client-channel (str ">" trimmed "<"))))
