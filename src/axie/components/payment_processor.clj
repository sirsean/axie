(ns axie.components.payment-processor
  (:require
    [clojure.tools.logging :as log]
    [clojure.string :as string]
    [mount.core :refer [defstate]]
    [tea-time.core :as tt]
    [axie.components.timer :refer [timer]]
    [axie.account :as account]
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
    #{"family-tree"}
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

(defn process-pending
  []
  (log/info "process-pending")
  (doseq [p (payment/fetch-pending)
          :let [tx @(ethplorer/get-tx-info (:txid p))]]
    (log/infof "payment (%s) tx (%s)" p tx)
    (cond
      (not (addr= pay-addr (:to tx)))
      (payment/set-status (:id p) :invalid-to)

      (not (addr= (:addr p) (:from tx)))
      (payment/set-status (:id p) :invalid-from)

      (not (known-product? (:product p)))
      (payment/set-status (:id p) :invalid-product)

      (valid? tx)
      (do
        (process-payment p tx)
        (payment/set-attrs (:id p)
                           {:status :success
                            :amount (:value tx)})))))

(defstate payment-processor
  :start (tt/every! 10 process-pending)
  :stop (tt/cancel! payment-processor))
