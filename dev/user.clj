(ns user
  (:require
    [beepboop.system :as sys]
    [dev.nu.morse :as morse]
    [donut.system :as donut]
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


(defn refresh
  []
  (donut.repl/restart))


(comment
  (morse/launch-in-proc)
  (stop)

  )
