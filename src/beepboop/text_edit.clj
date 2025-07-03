(ns beepboop.text-edit
  (:require
    [beepboop.draw :as draw]))


(declare handle-event)
(declare render)


(defn text-edit-view
  [label position width on-enter]
  {:label (atom label)
   :position (atom position)
   :width (atom width)
   :state (atom ["" 0])
   :on-enter on-enter
   :handle-event handle-event
   :render render})


(defn handle-event
  [{:keys [state on-enter] :as _text-edit} {:keys [type] :as event}]
  (case type
    :input (let [char (get event :char)]
             (swap! state (fn [[value cursor]]
                            [(str (subs value 0 cursor) char (subs value cursor))
                             (+ cursor 1)])))
    :press (case (get event :key)
             :arrow-left (swap! state (fn [[value cursor]] [value (max (- cursor 1) 0)]))
             :arrow-right (swap! state (fn [[value cursor]] [value (min (+ cursor 1) (count value))]))
             :backspace (swap! state (fn [[value cursor]]
                                       (if (> cursor 0)
                                         [(str (subs value 0 (- cursor 1)) (subs value cursor))
                                          (- cursor 1)]
                                         [value cursor])))
             :enter (do (on-enter (get @state 0))
                        (reset! state ["" 0]))
             ())
    ()))


(defn render
  [{:keys [label position width state] :as _text-edit} canvas]
  (let [[x y] @position
        [value cursor] @state
        inner-width (- @width 4)]
    (draw/box canvas [x y] [@width 3])
    (draw/shape canvas [(+ x 2) y] [(str " " @label " ")])
    (draw/shape canvas [(+ x 2) (+ y 1)] [(if (<= (count value) inner-width)
                                            value
                                            (subs value 0 inner-width))])
    (draw/set-cursor-position canvas [(+ x 2 cursor) (+ y 1)])))
