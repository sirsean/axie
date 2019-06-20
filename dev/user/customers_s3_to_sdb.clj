(ns user.customers-s3-to-sdb
  (:require
    [clojure.string :as string]
    [amazonica.aws.s3 :as s3]
    [cheshire.core :as json]
    [camel-snake-kebab.core :refer [->kebab-case-keyword]]
    [axie.auto-battle :as auto-battle]
    [axie.sdb :as sdb]
    ))

(defn s3-customers
  []
  (-> (s3/get-object "axie-customers" "customers.json")
      :input-stream
      slurp
      (json/parse-string ->kebab-case-keyword)))

(defn s3->sdb
  []
  (loop [[customer & more] (s3-customers)]
    (when (some? customer)
      (let [{:keys [name eth-addr token until max-teams]} customer]
        (sdb/set-attrs (auto-battle/domain-name)
                       (string/lower-case eth-addr)
                       {:name name
                        :token token
                        :max-teams max-teams
                        :until until}))
      (recur more))))
