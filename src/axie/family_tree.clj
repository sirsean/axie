(ns axie.family-tree
  (:require
    [axie.components.axie-db :as axie-db]
    [manifold.deferred :as md]
    [amazonica.aws.simpledb :as simpledb]
    [axie.sdb :as sdb]
    [axie.price :as price]
    ))

(def base-prices
  [{:views 10  :usd 1}
   {:views 100 :usd 5}
   {:views 500 :usd 15}])

(defn price-list
  []
  (->> base-prices
       price/attach-eth-prices))

(defn closest-tier
  [eth]
  (->> (price-list)
       (price/attach-eth-diff eth)
       (sort-by :eth-diff)
       first))

(defn family-axie
  [axie]
  (-> axie
      (select-keys [:id :name :image :title :class
                    :sire-id :matron-id :purity])))

(defn family-tree
  [{:keys [sire-id matron-id] :as axie}]
  (-> axie
      family-axie
      (cond->
        (and (some? sire-id) (not (zero? sire-id)))
        (assoc :sire (family-tree @(axie-db/fetch sire-id)))

        (and (some? matron-id) (not (zero? matron-id)))
        (assoc :matron (family-tree @(axie-db/fetch matron-id))))))

(defn viewed?
  [addr axie-id]
  (-> (format "select count(*) from `family-tree-views` where `addr`='%s' and `axie-id`='%s'"
              addr axie-id)
      (sdb/select-count true)
      pos?))

(defn view!
  [addr axie-id]
  (simpledb/put-attributes
    :domain-name "family-tree-views"
    :item-name (format "%s:%s" addr axie-id)
    :attributes [{:name "addr"
                  :value addr}
                 {:name "axie-id"
                  :value axie-id}]))

(defn fetch-num-views
  [addr]
  (sdb/select-count
        (format "select count(*) from `family-tree-views` where `addr`='%s'"
                addr)
        true))

(defn fetch-views
  [addr]
  (->> (simpledb/select
         :select-expression
         (format "select * from `family-tree-views` where `addr`='%s'"
                 addr))
       :items
       (map sdb/item->record)))
