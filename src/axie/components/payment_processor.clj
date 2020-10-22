(ns axie.components.payment-processor
  (:require
    [clojure.tools.logging :as log]
    [clojure.string :as string]
    [clj-time.core :as tc]
    [mount.core :refer [defstate]]
    [tea-time.core :as tt]
    [axie.components.timer :refer [timer]]
    [axie.account :as account]
    [axie.payment :as payment]
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
    #{}
    product))

(defn valid?
  [{:keys [success confirmations]}]
  (and success
       (pos? confirmations)))

(defmulti process-payment
  (fn [p tx]
    (:product p)))

(defmethod process-payment :default
  [p tx]
  (log/info "unknown payment" tx))

(defn process-pending
  []
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
