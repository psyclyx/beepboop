(ns beepboop.server-test
  (:require
    [beepboop.server :as server]
    [clojure.test :refer [deftest is]]))


;; Kinda useless, but gives the test runner something to run at first...
(deftest socket-address
  (let [socket-addr (server/socket-address {:host "123.45.67.89"
                                            :port 1234})]
    (is (= 1234 (.getPort socket-addr))
        "socket address port should match bind port")
    (is (= "123.45.67.89" (.getHostString socket-addr))
        "socket address host string should match bind host")))
