(ns axie.account
  (:require
    [amazonica.aws.simpledb :as simpledb]
    [axie.sdb :as sdb]
    [omniconf.core :as cfg]
    [vvvvalvalval.supdate.api :refer [supdate]]
    ))

(defn domain-name
  []
  (format "axie.%s.accounts" (name (cfg/get :env))))

(def defaults
  {:family-tree-paid 3})

(defn item->record
  [item]
  (-> item
      sdb/item->record
      ((partial merge defaults))
      (supdate {:family-tree-paid bigint})))

(defn fetch
  [addr]
  (let [attrs (simpledb/get-attributes
                :domain-name (domain-name)
                :item-name addr)]
    (when (seq (:attributes attrs))
      (-> attrs
          (assoc :name addr)
          item->record))))

(defn set-attrs
  [addr record]
  (simpledb/put-attributes
    :domain-name (domain-name)
    :item-name addr
    :attributes (sdb/map->attrs record)))
