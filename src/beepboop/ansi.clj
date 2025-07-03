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
    (fn [c meta-cb data-cb]
      (swap! buffer #(str % c))
      (if (re-matches #"\x1b($|\[[^@-~]*)" @buffer)
        () ; Incomplete escape sequence
        (do (if-let [match (re-matches #"\x1b\[([0-?]*[ -/]*[@-~])" @buffer)]
              (some-> (parse-code (get match 1)) meta-cb)
              (doseq [c @buffer] (data-cb c)))
            (reset! buffer ""))))))
