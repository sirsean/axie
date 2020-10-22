(ns axie.components.auto-battle
  (:require
    [byte-streams :as bs]
    [clojure.tools.logging :as log]
    [mount.core :refer [defstate] :as mount]
    [tea-time.core :as tt]
    [axie.components.timer :refer [timer]]
    [axie.api :as axie]
    [axie.auto-battle :as ab]
    ))

(defn start-customer
  [customer]
  ;; TODO: temporarily unlimited for all, no expiration
  (let [max-teams nil #_(ab/->max-teams customer)]
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

(declare start-all-battles)

(defstate auto-battle
  :start (tt/every! 120 start-all-battles)
  :stop (tt/cancel! auto-battle))

(defn running?
  []
  (some? ((mount/running-states) (str #'auto-battle))))

(defn start-all-battles
  []
  (log/info "start-all-battles")
  #_(loop [[customer & more] (ab/fetch-customers)]
    (when (and (running?) (some? customer))
      (start-customer customer)
      (recur more))))
