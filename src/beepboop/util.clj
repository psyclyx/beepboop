(ns beepboop.util
  (:require
    [clojure.math :as math]))


(defn current-ms
  []
  (quot (System/nanoTime) 1000000))


(defn rand-range
  [low high]
  (+ (rand (- high low)) low))


(defn polar-to-vec
  [deg length]
  (let [rad (math/to-radians deg)]
    [(* (math/cos rad) length) (* (math/sin rad) length)]))


(defn vec-length
  [[x y]]
  (math/sqrt (+ (* x x) (* y y))))


(defn dist-between
  [a b]
  (vec-length (map - a b)))
