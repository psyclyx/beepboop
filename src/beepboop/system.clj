(ns beepboop.system
  (:require
    [aero.core :as aero]
    [clojure.java.io :as io]
    [donut.system :as donut]
    [donut.system.validation :refer [validation-plugin]]
    [psyclyx.pastry :as pastry]))


(def system
  {::donut/plugins [pastry/pastry-plugin validation-plugin]
   ::donut/defs {:env {} ; to be replaced by config
                 }})


(defn -read-config
  [profile]
  (aero/read-config (io/file "config.edn")
                    {:profile profile}))


(defmethod donut/named-system ::base [_] system)


(defmethod donut/named-system ::dev [_]
  (donut/system ::base
                {[:env] (-read-config :dev)}))
