(ns axie.payment
  (:require
    [amazonica.aws.simpledb :as simpledb]
    [axie.sdb :as sdb]
    [clj-uuid :as uuid]
    [omniconf.core :as cfg]
    [vvvvalvalval.supdate.api :refer [supdate]]
    ))

(defn domain-name
  []
  (format "axie.%s.payments" (name (cfg/get :env))))

(defn item->record
  [item]
  (-> item
      sdb/item->record
      (supdate {:amount #(some-> % bigdec)})))

(defn add-payment
  [{:keys [product status txid addr]}]
  (let [id (uuid/v4)]
    (simpledb/put-attributes
      :domain-name (domain-name)
      :item-name id
      :attributes (sdb/map->attrs {:product product
                                   :status status
                                   :txid txid
                                   :addr addr}))
    id))

(defn set-attrs
  [id record]
  (simpledb/put-attributes
    :domain-name (domain-name)
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
         (format "select * from `%s` where `status`='pending'"
                 (domain-name))
         :consistent-read true)
       :items
       (map item->record)))

(defn tx-success?
  [txid]
  (-> (format "select count(*) from `%s` where `txid`='%s' and `status`='success'" (domain-name) txid)
      (sdb/select-count true)
      pos?))
