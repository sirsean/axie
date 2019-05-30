(ns axie.aws
  (:require
    [axie.api :as axie]
    [amazonica.aws.s3 :as s3]
    [clj-time.core :as tc]
    [clj-time.format :as tf]
    [cheshire.core :as json]
    [camel-snake-kebab.core :refer [->kebab-case-keyword]]
    [byte-streams :as bs]
    [manifold.deferred :as md])
  (:gen-class
    :methods [^:static [start [] String]]))

(defn expired?
  [until]
  (tc/before? (tf/parse-local-date until) (tc/today)))

(defn -start
  []
  (println "starting battles")
  (let [customers (-> (s3/get-object "axie-customers" "customers.json")
                      :input-stream
                      slurp
                      (json/parse-string ->kebab-case-keyword))]
    (println "customers:" (count customers))
    (loop [[{:keys [name eth-addr token until max-teams] :as customer} & more] customers]
      (when (some? customer)
        (let [max-teams (cond
                          (nil? until) max-teams
                          (expired? until) 3
                          :else max-teams)]
          (try
            (println "starting" name max-teams eth-addr token)
            (let [num-started @(axie/start-battles-for {:eth-addr eth-addr
                                                        :token token
                                                        :max-teams max-teams})]
              (println "started" eth-addr name num-started))
            (catch Exception e
              (println "failed" name eth-addr)
              (println e)))
          (println "skipping" eth-addr name))
        (recur more)))
    (println "all done!")))
