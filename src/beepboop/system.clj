(ns beepboop.system
  (:require
    [aero.core :as aero]
    [beepboop.connection :as connection]
    [beepboop.server :as server]
    [clojure.java.io :as io]
    [clojure.tools.logging :as log]
    [donut.system :as donut]
    [donut.system.validation :refer [validation-plugin]]
    [psyclyx.pastry :as pastry]))


(defn -log-fn
  [prefix]
  (fn [{::donut/keys [component-id]}]
    (log/info prefix component-id)))


(def LogLifetimes
  {::donut/pre-start {::log (-log-fn "Starting")}
   ::donut/post-start {::log (-log-fn "Started")}
   ::donut/pre-stop {::log (-log-fn "Stopping")}
   ::donut/post-stop {::log (-log-fn "Stopped")}})


(def system
  {::donut/plugins [pastry/pastry-plugin validation-plugin]
   ::donut/base LogLifetimes
   ::donut/defs {:env {} ; to be replaced by config
                 :app {:server {::pastry/type ::server/tcp
                                ::donut/config {:connection-handler connection/handle-connect
                                                :bind {:port 9090}}}}}})


(defn -read-config
  [profile]
  (aero/read-config (io/file "config.edn")
                    {:profile profile}))


(defmethod donut/named-system ::base [_] system)


(defmethod donut/named-system ::dev [_]
  (donut/system ::base
                {[:env] (-read-config :dev)}))
