(ns beepboop.ansi
  (:require
    [clojure.tools.logging :as log]))


(defn parse-code
  [code]
  (cond
    (= code "A") {:type :arrow :direction :up}
    (= code "B") {:type :arrow :direction :down}
    (= code "C") {:type :arrow :direction :right}
    (= code "D") {:type :arrow :direction :left}
    :else nil))


(defn make-parser
  []
  (let [buffer (atom "")]
    (fn [c event-cb passthrough-cb]
      (swap! buffer #(str % c))
      (if (re-matches #"\x1b($|\[[^@-~]*)" @buffer)
        () ; Incomplete escape sequence
        (do (if-let [match (re-matches #"\x1b\[([0-?]*[ -/]*[@-~])" @buffer)]
              (some-> (parse-code (get match 1)) event-cb)
              (doseq [c @buffer] (passthrough-cb c)))
            (reset! buffer ""))))))
