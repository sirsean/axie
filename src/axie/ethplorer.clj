(ns axie.ethplorer
  (:require
    [axie.api :refer [fetch-json]]
    ))

(defn get-tx-info
  [txid]
  (fetch-json
    (format "http://api.ethplorer.io/getTxInfo/%s?apiKey=%s"
            txid "freekey")))
