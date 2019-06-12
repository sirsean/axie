(ns axie.sdb
  (:require
    [amazonica.aws.simpledb :as simpledb]
    [plumbing.core :as p]
    ))

(def all-domains
  ["accounts"
   "family-tree-views"
   "payments"])

(defn attrs->map
  [attrs]
  (p/for-map [{:keys [name value]} attrs]
             (keyword name) value))

(defn map->attrs
  [m]
  (map (fn [[k v]]
         {:name (name k)
          :value v
          :replace true})
       m))

(defn item->record
  [{:keys [name attributes]}]
  (merge
    (attrs->map attributes)
    {:id name}))

(defn select-count
  [expr consistent?]
  (-> (simpledb/select :select-expression expr
                       :consistent-read consistent?)
      :items
      first
      :attributes
      first
      :value
      bigint))
