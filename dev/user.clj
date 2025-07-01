(ns user
  (:require
    [dev.nu.morse :as morse]
    [donut.system :as donut]
    [beepboop.system :as sys]
    [donut.system.repl :as donut.repl]
    [donut.system.repl.state :refer [system]]))


(defmethod donut/named-system ::donut/repl
  [_]
  (donut/system ::sys/dev))


(defn start
  []
  (donut.repl/start))


(defn stop
  []
  (donut.repl/stop))


(comment
  (morse/launch-in-proc)
  )
