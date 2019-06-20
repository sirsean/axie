(ns axie.components.auto-battle
  (:require
    [byte-streams :as bs]
    [clojure.tools.logging :as log]
    [mount.core :refer [defstate]]
    [tea-time.core :as tt]
    [axie.components.timer :refer [timer]]
    [axie.api :as axie]
    [axie.auto-battle :as ab]
    ))

(defn start-customer
  [customer]
  (let [max-teams (ab/->max-teams customer)]
    (try
      (let [num-started @(axie/start-battles-for
                           {:eth-addr (:id customer)
                            :token (:token customer)
                            :max-teams max-teams})]
        (log/infof "started %s %s" customer num-started))
      (catch Exception e
        (log/errorf e
                    "failed to start battles %s %s: %s"
                    (:name customer) (:id customer)
                    (some-> e ex-data :body bs/to-string))))))

(defn start-all-battles
  []
  (loop [[customer & more] (ab/fetch-customers)]
    (when (some? customer)
      (start-customer customer)
      (recur more))))

(defstate auto-battle
  :start (tt/every! 120 start-all-battles)
  :stop (tt/cancel! auto-battle))
