(ns beepboop.draw
  (:require
    [clojure.string :as str]))


(def transparent \tab)


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


(defn pixels
  [{:keys [contents size] :as _canvas} pos dim func]
  (let [[x y] (map int pos)
        [width height] (map int dim)]
    (if (and (< x (get @size 0))
             (< y (get @size 1))
             (> (+ x width) 0)
             (> (+ y height) 0))
      (swap! contents
             (fn [current-lines]
               (vec (map-indexed
                      (fn [i current-line]
                        (if (and (>= i y) (< i (+ y height)))
                          (apply str (map-indexed
                                       (fn [j current-char]
                                         (if (and (>= j x) (< j (+ x width)))
                                           (if-let [new-char (func [(- j x) (- i y)])]
                                             new-char
                                             current-char)
                                           current-char))
                                       current-line))
                          current-line))
                      current-lines))))
      ())))


(defn array
  [canvas pos new-lines]
  (let [new-lines-vec (vec new-lines)]
    (cond (seq new-lines-vec)
          (pixels canvas pos [(count (get new-lines-vec 0)) (count new-lines-vec)] (fn [[x y]]
                                                                                     (let [c (get (get new-lines-vec y) x)]
                                                                                       (if (= c transparent) nil c)))))))


(defn text
  [canvas pos text]
  (array canvas pos (str/split-lines text)))


(defn rect
  [canvas pos size fill]
  (pixels canvas pos size (fn [_pos] fill)))


(defn box
  [canvas pos [width height] fill]
  (array
    canvas
    pos
    (concat [(str "╭" (apply str (repeat (- width 2) "─")) "╮")]
            (repeat (- height 2)
                    (str "│" (apply str (repeat (- width 2) fill)) "│"))
            [(str "╰" (apply str (repeat (- width 2) "─")) "╯")])))
