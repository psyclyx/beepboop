(ns beepboop.main
  (:require
    [beepboop.system :as system]
    [clojure.tools.logging :as log]
    [donut.system :as donut]))


(Thread/setDefaultUncaughtExceptionHandler
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException
      [_ thread ex]
      (log/errorf ex "Uncaught exception on thread %s" (.getName thread)))))


(defn -main
  [& _args]
  (let [system (donut/system ::system/dev)
        running-system (donut/signal system ::donut/start)
        runtime (Runtime/getRuntime)
        stop ^Runnable #(donut/signal running-system ::donut/stop)]
    (doto runtime
      (.addShutdownHook (Thread. stop)))))
