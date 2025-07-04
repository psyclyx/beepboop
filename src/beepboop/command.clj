(ns beepboop.command
  (:require
    [beepboop.game :as game]
    [beepboop.util :as util]
    [clojure.tools.logging :as log]))


(defn handle-command
  [{:keys [handle-disconnect message] :as connection} player game cmd]
  (cond
    (= cmd "exit") (handle-disconnect connection)
    (= cmd "fire") (if (get @player :alive)
                     (game/create-grenade game
                                          (get @player :pos)
                                          (util/polar-to-vec (util/rand-range -20 -160)
                                                             (util/rand-range 1 2)))
                     (reset! message "You cannot shoot\nwhen you are dead"))
    :else (log/info "Invalid command:" cmd)))
