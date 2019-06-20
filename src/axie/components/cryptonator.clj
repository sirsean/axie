(ns axie.components.cryptonator
  (:require
    [clojure.tools.logging :as log]
    [mount.core :refer [defstate]]
    [clojure.string :as string]
    [manifold.deferred :as md]
    [tea-time.core :as tt]
    [axie.api :refer [fetch-json]]
    [axie.components.timer :refer [timer]]
    ))

(defn fetch-ticker
  [ticker]
  (md/chain
    (fetch-json
      (format "https://api.cryptonator.com/api/ticker/%s"
              (string/lower-case ticker)))
    :ticker))

(declare refresh-ticker)

(defstate cryptonator
  :start {:eth (atom nil)
          :timer (tt/every! 60 refresh-ticker)}
  :stop (tt/cancel! (:timer cryptonator)))

(defn refresh-ticker
  []
  @(md/chain
     (fetch-ticker "eth-usd")
     (fn [ticker]
       (log/info ticker)
       (reset! (:eth cryptonator) ticker))))

(defn get-price
  [coin]
  (some-> cryptonator
          coin
          deref
          :price
          bigdec))
