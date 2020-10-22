(ns axie.components.card-rankings
  (:require
    [clojure.tools.logging :as log]
    [byte-streams :as bs]
    [mount.core :refer [defstate]]
    [tea-time.core :as tt]
    [axie.api :refer [fetch-json]]
    [axie.components.cards :as cards]
    [amazonica.aws.s3 :as s3]
    [cheshire.core :as json]
    [camel-snake-kebab.core :refer [->kebab-case-keyword]]
    [vvvvalvalval.supdate.api :refer [supdate]]
    ))

(defn fetch-ratings
  []
  (log/info :fetch-ratings)
  (try
    (-> (s3/get-object "axie-customers" "card-ratings.json")
        :input-stream
        slurp
        (json/parse-string ->kebab-case-keyword))
    (catch Exception e
      {})))

(defn save-ratings
  [ratings]
  (log/info :save-ratings)
  (s3/put-object :bucket-name  "axie-customers"
                 :key          "card-ratings.json"
                 :input-stream (-> ratings
                                   json/generate-string
                                   bs/to-input-stream)))

(defn default-ratings
  [cards]
  (zipmap
    (keys cards)
    (repeat 1000)))

(declare flush-ratings)

(defstate card-rankings
  :start (let [ratings (fetch-ratings)]
           {:timer (tt/every! 600 flush-ratings)
            :ratings (atom (merge (default-ratings (cards/get-cards))
                                  ratings))})
  :stop (let [ratings @(:ratings card-rankings)]
          (save-ratings ratings)
          (tt/cancel! (:timer card-rankings))
          {}))

(defn flush-ratings
  []
  (when-some [ratings (:ratings card-rankings)]
    (save-ratings @ratings)))

(def k 20)

(defn prob
  [r1 r2]
  (/ 1M (+ 1M (Math/pow 10 (/ (- r1 r2) 400M)))))

(defn new-rating
  [r p win?]
  (+ r (* k (- (if win? 1 0) p))))

(defn record-vote
  [ratings winner-key loser-key]
  (let [winner-key (keyword winner-key)
        loser-key (keyword loser-key)
        winner-rating (get ratings winner-key)
        loser-rating (get ratings loser-key)
        winner-prob (prob winner-rating loser-rating)
        loser-prob (prob loser-rating winner-rating)]
    (supdate ratings
             {winner-key #(new-rating % winner-prob true)
              loser-key  #(new-rating % loser-prob false)})))

(defn vote
  [winner-key loser-key]
  (swap! (:ratings card-rankings) record-vote winner-key loser-key))

(defn get-rankings
  []
  (->> card-rankings
       :ratings
       deref
       (sort-by (fn [[card-key rating]]
                  [(- rating) card-key]))
       (map-indexed (fn [i [card-key rating]]
                      (-> (get (cards/get-cards) card-key)
                          (assoc :id     card-key
                                 :rank   (inc i)
                                 :rating rating))))))
