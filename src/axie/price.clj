(ns axie.price
  (:require
    [axie.components.cryptonator :as cryptonator]
    )
  (:import
    (java.math BigDecimal)))

(defn usd->eth
  [usd]
  (.divide (bigdec usd)
           (cryptonator/get-price :eth)
           10
           BigDecimal/ROUND_HALF_UP))

(defn eth->usd
  [eth]
  (* eth (cryptonator/get-price :eth)))

(defn attach-eth-prices
  [tiers]
  (map (fn [{:keys [usd] :as tier}]
         (assoc tier :eth (usd->eth usd)))
       tiers))

(defn attach-eth-diff
  [paid-eth tiers]
  (map (fn [{:keys [eth] :as tier}]
         (assoc tier :eth-diff
                (.abs (- eth (bigdec paid-eth)))))
       tiers))
