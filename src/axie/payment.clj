(ns axie.payment
  (:require
    [amazonica.aws.simpledb :as simpledb]
    [axie.sdb :as sdb]
    [clj-uuid :as uuid]
    [vvvvalvalval.supdate.api :refer [supdate]]
    ))

(defn item->record
  [item]
  (-> item
      sdb/item->record
      (supdate {:amount #(some-> % bigdec)})))

(defn add-payment
  [{:keys [product status txid addr]}]
  (let [id (uuid/v4)]
    (simpledb/put-attributes
      :domain-name "payments"
      :item-name id
      :attributes (sdb/map->attrs {:product product
                                   :status status
                                   :txid txid
                                   :addr addr}))
    id))

(defn set-attrs
  [id record]
  (simpledb/put-attributes
    :domain-name "payments"
    :item-name id
    :attributes (sdb/map->attrs record)))

(defn set-status
  [id status]
  (set-attrs id {:status status}))

(defn set-amount
  [id amount]
  (set-attrs id {:amount amount}))

(defn fetch-pending
  []
  (->> (simpledb/select
         :select-expression
         "select * from `payments` where `status`='pending'"
         :consistent-read true)
       :items
       (map item->record)))
