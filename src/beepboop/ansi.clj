(ns beepboop.ansi)


(defn parse-code
  [code]
  (cond
    (= code "A") {:type :arrow :direction :up}
    (= code "B") {:type :arrow :direction :down}
    (= code "C") {:type :arrow :direction :right}
    (= code "D") {:type :arrow :direction :left}
    :else nil))


(defn bytes-to-chars-filter
  [sink]
  (fn [{:keys [type byte] :as event}]
    (if (= type :input-byte)
      (sink {:type :input :char (char byte)})
      (sink event))))


(defn escape-codes-filter
  [sink]
  (let [buffer (atom "")]
    (fn [{:keys [type char] :as event}]
      (if (= type :input)
        (do (swap! buffer #(str % char))
            (if (re-matches #"\x1b($|\[[^@-~]*)" @buffer)
              () ; Incomplete escape sequence
              (do (if-let [match (re-matches #"\x1b\[([0-?]*[ -/]*[@-~])" @buffer)]
                    (some-> (parse-code (get match 1)) sink)
                    (doseq [c @buffer] (sink {:type :input :char c})))
                  (reset! buffer ""))))
        (sink event)))))
