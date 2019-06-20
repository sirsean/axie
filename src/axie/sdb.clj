(ns axie.sdb
  (:require
    [amazonica.aws.simpledb :as simpledb]
    [plumbing.core :as p]
    ))

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

(defn set-attrs
  [domain-name id record]
  (simpledb/put-attributes
    :domain-name domain-name
    :item-name id
    :attributes (map->attrs record)))

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

(defn select-all
  [expr consistent?]
  (loop [all []
         result (simpledb/select :select-expression expr
                                 :consistent-read consistent?)]
    (let [{:keys [next-token items]} result
          all (into all items)]
      (if-not (some? next-token)
        all
        (recur all (simpledb/select :select-expression expr
                                    :consistent-read consistent?
                                    :next-token next-token))))))
