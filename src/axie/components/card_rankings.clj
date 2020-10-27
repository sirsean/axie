(ns axie.components.card-rankings
  (:require
    [clojure.tools.logging :as log]
    [mount.core :refer [defstate]]
    [tea-time.core :as tt]
    [axie.elo :as elo]
    [axie.components.cards :as cards]
    ))

(defn rating-type->filename
  [rating-type]
  (case rating-type
    :attack "card-ratings-attack.json"
    :defense "card-ratings-defense.json"
    "card-ratings.json"))

(defn default-ratings
  [cards]
  (zipmap
    (keys cards)
    (repeat 1000)))

(defn ratings-with-defaults
  [ratings]
  (merge (default-ratings (cards/get-cards))
         ratings))

(declare flush-ratings)

(defstate card-rankings
  :start {:timer (tt/every! 600 flush-ratings)
          :ratings (atom (ratings-with-defaults
                           (elo/fetch-ratings (rating-type->filename :all))))
          :attack-ratings (atom (ratings-with-defaults
                                  (elo/fetch-ratings (rating-type->filename :attack))))
          :defense-ratings (atom (ratings-with-defaults
                                   (elo/fetch-ratings (rating-type->filename :defense))))}
  :stop (do
          (elo/save-ratings (rating-type->filename :all)
                        @(:ratings card-rankings))
          (elo/save-ratings (rating-type->filename :attack)
                        @(:attack-ratings card-rankings))
          (elo/save-ratings (rating-type->filename :defense)
                        @(:defense-ratings card-rankings))
          (tt/cancel! (:timer card-rankings))
          {}))

(defn flush-ratings
  []
  (when-some [ratings (:ratings card-rankings)]
    (elo/save-ratings (rating-type->filename :all) @ratings))
  (when-some [attack-ratings (:attack-ratings card-rankings)]
    (elo/save-ratings (rating-type->filename :attack) @attack-ratings))
  (when-some [defense-ratings (:defense-ratings card-rankings)]
    (elo/save-ratings (rating-type->filename :defense) @defense-ratings)))

(defn rating-type->ratings-key
  [rating-type]
  (case rating-type
    :attack :attack-ratings
    :defense :defense-ratings
    :ratings))

(defn vote
  [rating-type winner-key loser-key]
  (let [rating-key (rating-type->ratings-key rating-type)]
    (swap! (get card-rankings rating-key)
           elo/record-vote
           (default-ratings (cards/get-cards)) winner-key loser-key)))

(defn get-rankings
  [rating-type]
  (->> card-rankings
       ((rating-type->ratings-key rating-type))
       deref
       (sort-by (fn [[card-key rating]]
                  [(- rating) card-key]))
       (map-indexed (fn [i [card-key rating]]
                      (-> (get (cards/get-cards) card-key)
                          (assoc :id     card-key
                                 :rank   (inc i)
                                 :rating rating))))))
