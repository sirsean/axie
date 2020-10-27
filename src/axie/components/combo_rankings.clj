(ns axie.components.combo-rankings
  (:require
    [clojure.tools.logging :as log]
    [clojure.string :as string]
    [mount.core :refer [defstate]]
    [tea-time.core :as tt]
    [axie.elo :as elo]
    [axie.components.cards :as cards]
    ))

(declare flush-ratings)

(defstate combo-rankings
  :start {:timer (tt/every! 600 flush-ratings)
          :ratings (atom (elo/fetch-ratings "combo-ratings.json"))}
  :stop (do
          (flush-ratings)
          (when-some [timer (:timer combo-rankings)]
            (tt/cancel! timer))
          {}))

(defn flush-ratings
  []
  (when-some [ratings (:ratings combo-rankings)]
    (elo/save-ratings "combo-ratings.json" @ratings)))

(defn parse-combo-key
  [combo-key]
  (map keyword (-> combo-key
                   name
                   (string/split #"\."))))

(defn combo-key-exists?
  [combo-key]
  (let [combo-key (keyword combo-key)]
    (contains? @(:ratings combo-rankings) combo-key)))

(defn valid-combo-key?
  [combo-key]
  (let [combo-key (keyword combo-key)]
    (and (not (combo-key-exists? combo-key))
         (every? (partial contains? (cards/get-cards)) (parse-combo-key combo-key)))))

(defn add-combo
  [combo-key]
  ;; verify it's not already there
  ;; verify all parts of the combo-key are real cards
  (let [combo-key (keyword combo-key)]
    (if (valid-combo-key? combo-key)
      (swap! (:ratings combo-rankings) assoc combo-key 1000)
      @(:ratings combo-rankings))))

(defn vote
  [winner-key loser-key]
  (if (and (combo-key-exists? winner-key)
           (combo-key-exists? loser-key))
    (swap! (:ratings combo-rankings)
           elo/record-vote
           {} winner-key loser-key)
    @(:ratings combo-rankings)))

(defn get-rankings
  []
  (->> combo-rankings
       :ratings
       deref
       (sort-by (fn [[combo-key rating]]
                  [(- rating) combo-key]))
       (map-indexed (fn [i [combo-key rating]]
                      {:id     combo-key
                       :rank   (inc i)
                       :rating rating
                       :cards  (->> combo-key
                                    parse-combo-key
                                    (map (partial get (cards/get-cards))))}))))
