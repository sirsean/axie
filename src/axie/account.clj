(ns axie.account
  (:require
    [amazonica.aws.simpledb :as simpledb]
    [axie.sdb :as sdb]
    [vvvvalvalval.supdate.api :refer [supdate]]
    ))

(def defaults
  {:family-tree-paid 0})

(defn item->record
  [item]
  (-> item
      sdb/item->record
      ((partial merge defaults))
      (supdate {:family-tree-paid bigint})))

(defn fetch
  [addr]
  (let [attrs (simpledb/get-attributes
                :domain-name "accounts"
                :item-name addr)]
    (when (seq (:attributes attrs))
      (-> attrs
          (assoc :name addr)
          item->record))))

(defn set-attrs
  [addr record]
  (simpledb/put-attributes
    :domain-name "accounts"
    :item-name addr
    :attributes (sdb/map->attrs record)))
