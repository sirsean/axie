(ns axie.components.payment-processor
  (:require
    [clojure.tools.logging :as log]
    [clojure.string :as string]
    [clj-time.core :as tc]
    [mount.core :refer [defstate]]
    [tea-time.core :as tt]
    [axie.components.timer :refer [timer]]
    [axie.account :as account]
    [axie.auto-battle :as auto-battle]
    [axie.payment :as payment]
    [axie.family-tree :as family-tree]
    [axie.ethplorer :as ethplorer]
    ))

(def pay-addr
  "0x560EBafD8dB62cbdB44B50539d65b48072b98277")

(defn addr=
  [a b]
  (= (string/lower-case a) (string/lower-case b)))

(defn known-product?
  [product]
  (contains?
    #{"family-tree" "auto-battle"}
    product))

(defn valid?
  [{:keys [success confirmations]}]
  (and success
       (pos? confirmations)))

(defmulti process-payment
  (fn [p tx]
    (:product p)))

(defmethod process-payment "family-tree"
  [p tx]
  (let [tier (family-tree/closest-tier (bigdec (:value tx)))
        {:keys [family-tree-paid]} (account/fetch (:addr p))]
    (log/infof "family-tree tier (%s)" tier)
    (account/set-attrs (:addr p)
                       {:family-tree-paid (+ family-tree-paid
                                             (:views tier))})))

(defmethod process-payment "auto-battle"
  [p tx]
  (let [amount (bigdec (:value tx))
        max-teams (some-> p :max-teams auto-battle/blank->nil bigint)
        days (-> max-teams
                 auto-battle/tier-by-max-teams
                 (auto-battle/calc-days amount))
        until (tc/plus (tc/today) (tc/days days))]
    (log/infof "auto-battle (max-teams %s) (days %s) (until %s)" max-teams days until)
    (auto-battle/upgrade (:addr p)
                         {:max-teams max-teams
                          :until until})))

(defn process-pending
  []
  #_(log/info "process-pending")
  (doseq [p (payment/fetch-pending)]
    (log/infof "payment (%s)" p)
    (let [tx @(ethplorer/get-tx-info (:txid p))]
      (log/infof "tx (%s)" tx)
      (cond
        (not (addr= pay-addr (:to tx)))
        (payment/set-status (:id p) :invalid-to)

        (not (addr= (:addr p) (:from tx)))
        (payment/set-status (:id p) :invalid-from)

        (not (known-product? (:product p)))
        (payment/set-status (:id p) :invalid-product)

        (payment/tx-success? (:txid p))
        (payment/set-status (:id p) :invalid-doublepay)

        (valid? tx)
        (try
          (process-payment p tx)
          (payment/set-attrs (:id p)
                             {:status :success
                              :amount (:value tx)})
          (catch Exception e
            (log/errorf e "failed to process payment %s" (:id p))))))))

(defstate payment-processor
  :start (tt/every! 10 process-pending)
  :stop (tt/cancel! payment-processor))
