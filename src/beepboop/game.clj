(ns beepboop.game
  (:require
    [beepboop.draw :as draw]
    [beepboop.util :as util]
    [clojure.tools.logging :as log]))


(def ms-between-ticks 50)
(def drag 0.1)
(def grav 1)
(def map-width 500)
(def map-height 100)
(def granade-explosion-size 2.5)


(defn make-grid
  []
  (vec (concat (repeat (quot map-height 2) (vec (repeat map-width nil)))
               (repeat (quot map-height 2) (vec (repeat map-width {:type :ground}))))))


(defn make-game
  []
  {:objects (atom [])
   :grid (atom (make-grid))
   :offset (map quot [map-width map-height] [2 2])
   :next-tick-target (atom (util/current-ms))
   :listeners (atom (set []))})


(defn create-object
  [{:keys [objects] :as _game} type pos vel]
  (let [obj (atom {:type type
                   :pos pos
                   :vel vel
                   :moving true
                   :alive true
                   :cleanup-time nil})]
    (swap! objects #(conj % obj))
    obj))


(defn create-player
  [game pos]
  (create-object game :player pos [0 0]))


(defn create-granade
  [game thrower-pos vel]
  (create-object game
                 :granade
                 (map + thrower-pos (map * vel [0.5 0.5]))
                 vel))


(defn register-listener
  [{:keys [listeners] :as _game} listener]
  (swap! listeners #(conj % listener)))


(defn unregister-listener
  [{:keys [listeners] :as _game} listener]
  (swap! listeners #(disj % listener)))


(defn get-tile
  [grid [offset-x offset-y] [x y]]
  (get (get grid (int (+ y offset-y))) (int (+ x offset-x))))


(defn apply-grav
  [vel dt]
  (map + vel [0 (* grav dt)]))


(defn apply-drag
  [vel dt]
  (map * vel [(- 1 (* drag dt)) 1]))


(defn tick
  [{:keys [grid offset objects listeners] :as _game} dt]
  (let [explosions (atom [])]
    (doseq [obj @objects]
      (swap! obj (fn [{:keys [type pos vel moving] :as obj}]
                   (let [obj (if moving
                               (let [pos (map + pos vel)
                                     vel (apply-grav (apply-drag vel dt) dt)
                                     moving (not (get-tile @grid offset pos))
                                     type (if (and (= type :granade) (not moving))
                                            (do (swap! explosions #(conj % [pos granade-explosion-size]))
                                                :explosion)
                                            type)]
                                 (assoc obj :type type :pos pos :vel vel :moving moving))
                               obj)]
                     obj))))
    (doseq [[pos size] @explosions]
      (doseq [obj @objects]
        (when (<= (util/dist-between pos (get @obj :pos)) size)
          (swap! obj #(assoc % :alive false))))))
  (doseq [{:keys [after-tick]} @listeners]
    (after-tick)))


(defn maybe-tick
  [{:keys [next-tick-target] :as game}]
  (let [current (util/current-ms)
        time-til-tick (- @next-tick-target current)]
    (if (< time-til-tick 2)
      (do (reset! next-tick-target (+ @next-tick-target ms-between-ticks))
          (tick game (/ ms-between-ticks 1000))
          (let [current (util/current-ms)]
            (when (< @next-tick-target current)
              (log/info "Fell behind our clock, drifting to resync"))
            (reset! next-tick-target (+ current ms-between-ticks))
            (- @next-tick-target current)))
      (- @next-tick-target current))))


(defn render
  [{:keys [grid offset objects] :as _game} canvas center]
  (let [canvas-size (draw/get-size canvas)
        top-left (map - center (map quot canvas-size [2 2]))
        pos-to-canvas (fn [pos] (map - pos top-left))]
    (draw/pixels canvas
                 [0 0]
                 (draw/get-size canvas)
                 (fn [pos]
                   (let [{:keys [type]} (get-tile @grid offset (map + top-left pos))]
                     (case type
                       :ground "█"
                       nil " "))))
    (doseq [obj @objects]
      (let [{:keys [type pos]} @obj]
        (case type
          :player (draw/text canvas (pos-to-canvas pos) "T")
          :granade (draw/text canvas (pos-to-canvas pos) "*")
          :explosion (draw/pixels
                       canvas
                       (map - (pos-to-canvas pos) [5 5])
                       [10 10]
                       (fn [pos]
                         (if (< (util/dist-between pos [5 5]) granade-explosion-size) "#" nil))))))))
