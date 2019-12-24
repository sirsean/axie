(ns axie.components.battle-history
  (:require
    [clojure.tools.logging :as log]
    [mount.core :refer [defstate] :as mount]
    [tea-time.core :as tt]
    [axie.api :as api]
    [axie.battle-history :as bh]
    [axie.components.timer :refer [timer]]
    [vvvvalvalval.supdate.api :refer [supdate]]
    ))

(defn increment-wins
  [team-id]
  (-> team-id
      bh/fetch-team-record
      (supdate {:wins inc})
      ((partial bh/set-team-record team-id))))

(defn increment-losses
  [team-id]
  (-> team-id
      bh/fetch-team-record
      (supdate {:losses inc})
      ((partial bh/set-team-record team-id))))

(declare load-battles)

#_(defstate battle-history
  :start (tt/every! 60 load-battles)
  :stop (tt/cancel! battle-history))

(defn running?
  []
  false
  #_(some? ((mount/running-states) (str #'battle-history))))

(defn load-battles
  []
  (let [max-battle-id (bh/fetch-max-battle-id)]
    (log/infof "previous max-battle-id: %s" max-battle-id)
    (let [next-max-battle-id
          (loop [battle-id (inc max-battle-id)]
            (if (running?)
              (let [battle @(api/fetch-battle battle-id)]
                (if (and (some? battle) (not= {:script nil} battle))
                  (let [{:keys [winner loser]} (bh/battle->teams battle)]
                    (log/infof "battle %s, winner: %s, loser: %s" battle-id winner loser)
                    (when (some? winner)
                      (increment-wins winner))
                    (when (some? loser)
                      (increment-losses loser))
                    (bh/set-max-battle-id battle-id)
                    (recur (inc battle-id)))
                  (dec battle-id)))
              (do
                (log/infof "stopping early!")
                (dec battle-id))))]
      (log/infof "new max-battle-id: %s" next-max-battle-id))))
