(ns beepboop.draw)


(defn make-canvas
  []
  {:size (atom [0 0])
   :contents (atom [])
   :cursor-position (atom [0 0])})


(defn get-contents
  [{:keys [contents] :as _canvas}]
  @contents)


(defn set-cursor-position
  [{:keys [size cursor-position] :as _canvas} [x y]]
  (let [[width height] @size]
    (reset! cursor-position [(max 0 (min width x))
                             (max 0 (min height y))])))


(defn get-cursor-position
  [{:keys [cursor-position] :as _canvas}]
  @cursor-position)


(defn set-size
  [{:keys [size contents cursor-position] :as canvas} [width height]]
  (println "setting size to" [width height])
  (if (not= size [width height])
    (do (reset! size [width height])
        (reset! contents (vec (repeat height
                                      (apply str (repeat width " ")))))
        (set-cursor-position canvas @cursor-position))
    ()))


(defn get-size
  [{:keys [size] :as _canvas}]
  @size)


(defn shape
  [{:keys [contents] :as _canvas} [x y] new-lines]
  (let [new-lines-vec (vec new-lines)]
    (swap! contents
           (fn [current-lines]
             (vec (map-indexed
                    (fn [i current-line]
                      (if-let [new-line (get new-lines-vec (- i y))]
                        (apply str (map-indexed
                                     (fn [j current-char]
                                       (let [new-char (get new-line (- j x))]
                                         (if (or (= new-char "\t") (= new-char nil))
                                           current-char
                                           new-char)))
                                     current-line))
                        current-line))
                    current-lines))))))


(defn rect
  [canvas fill pos [width height]]
  (shape
    canvas
    pos
    (repeat height
            (apply str (repeat width fill)))))


(defn box
  [canvas pos [width height]]
  (shape
    canvas
    pos
    (concat [(str "╭" (apply str (repeat (- width 2) "─")) "╮")]
            (repeat (- height 2)
                    (str "│" (apply str (repeat (- width 2) " ")) "│"))
            [(str "╰" (apply str (repeat (- width 2) "─")) "╯")])))
