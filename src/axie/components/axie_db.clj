(ns axie.components.axie-db
  (:require
    [mount.core :refer [defstate]]
    [manifold.deferred :as md]
    [axie.api :as api]
    ))

(defstate axie-db
  :start (atom {}))

(defn from-cache
  [axie-id]
  (get @axie-db axie-id))

(defn to-cache!
  [{:keys [id] :as axie}]
  (swap! axie-db assoc id axie)
  axie)

(defn fetch
  [id]
  (let [axie (from-cache id)]
    (if (and (some? axie)
             (api/adult? axie))
      (md/success-deferred axie)
      (md/chain
        (api/fetch-axie id)
        to-cache!))))
