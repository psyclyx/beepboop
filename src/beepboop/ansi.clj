(ns beepboop.ansi)


(defn parse-code
  [code]
  (cond
    (= code "A") {:type :press :key :arrow-up}
    (= code "B") {:type :press :key :arrow-down}
    (= code "C") {:type :press :key :arrow-right}
    (= code "D") {:type :press :key :arrow-left}
    :else nil))


(defn bytes-to-chars-filter
  [sink]
  ;; TODO buffer input on error to handle non-ascii
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
                    (doseq [c @buffer] (cond
                                         (= (int c) 0) () ; Seems to happen after every enter??
                                         (= (int c) 127) (sink {:type :press :key :backspace})
                                         (= (int c) 9) (sink {:type :press :key :tab})
                                         (= (int c) 13) (sink {:type :press :key :enter})
                                         (= (int c) 10) (sink {:type :press :key :newline})
                                         :else (sink {:type :input :char c}))))
                  (reset! buffer ""))))
        (sink event)))))
