(ns axie.elo
  (:require
    [clojure.tools.logging :as log]
    [byte-streams :as bs]
    [amazonica.aws.s3 :as s3]
    [cheshire.core :as json]
    [camel-snake-kebab.core :refer [->kebab-case-keyword]]
    [vvvvalvalval.supdate.api :refer [supdate]]
    ))

(defn fetch-ratings
  [filename]
  (log/info :fetch-ratings)
  (try
    (-> (s3/get-object "axie-customers" filename)
        :input-stream
        slurp
        (json/parse-string ->kebab-case-keyword))
    (catch Exception e
      {})))

(defn save-ratings
  [filename ratings]
  (log/info :save-ratings)
  (s3/put-object :bucket-name  "axie-customers"
                 :key          filename
                 :input-stream (-> ratings
                                   json/generate-string
                                   bs/to-input-stream)))

(def k 20)

(defn prob
  [r1 r2]
  (/ 1M (+ 1M (Math/pow 10 (/ (- r1 r2) 400M)))))

(defn new-rating
  [r p win?]
  (+ r (* k (- (if win? 1 0) p))))

(defn record-vote
  [ratings defaults winner-key loser-key]
  (let [winner-key (keyword winner-key)
        loser-key (keyword loser-key)
        winner-rating (get ratings winner-key)
        loser-rating (get ratings loser-key)
        winner-prob (prob winner-rating loser-rating)
        loser-prob (prob loser-rating winner-rating)]
    (-> (merge defaults ratings)
        (supdate {winner-key #(new-rating % winner-prob true)
                  loser-key  #(new-rating % loser-prob false)}))))
