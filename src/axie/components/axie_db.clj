(ns axie.components.axie-db
  (:require
    [clojure.tools.logging :as log]
    [mount.core :refer [defstate]]
    [manifold.deferred :as md]
    [com.climate.claypoole :as cp]
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

(defn prewarm
  [max-id]
  (cp/with-shutdown! [pool (cp/threadpool 100)]
    (->> (range 1 max-id)
         (cp/upmap pool (fn [id]
                          @(-> (fetch id)
                              (md/catch
                                Exception
                                (fn [e]
                                  (log/errorf "failed to fetch axie %s: %s" id (.getMessage e)))))))
         doall
         count)))
