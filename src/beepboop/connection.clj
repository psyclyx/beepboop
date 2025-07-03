(ns beepboop.connection
  (:require
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [beepboop.server :as server]))


(defn handle-packet
  [client-channel data]
  (let [trimmed (str/trim data)]
    (println trimmed)
    (server/send-response client-channel (str ">" trimmed "<"))))
