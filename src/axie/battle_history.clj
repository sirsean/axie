(ns axie.battle-history
  (:require
    [clojure.string :as string]
    [clojure.tools.logging :as log]
    [amazonica.aws.simpledb :as simpledb]
    [omniconf.core :as cfg]
    [axie.sdb :as sdb]
    [vvvvalvalval.supdate.api :refer [supdate]]
    ))

(defn domain-name
  []
  (format "axie.%s.battle-history" (name (cfg/get :env))))

(defn max-battle-record
  [item]
  (-> item
      sdb/item->record
      ((partial merge {:battle-id 0}))
      (supdate {:battle-id (fnil bigint 0)})))

(defn fetch-max-battle-id
  []
  (-> (simpledb/get-attributes
        :domain-name (domain-name)
        :item-name "max-battle-id"
        :consistent-read true)
      max-battle-record
      :battle-id))

(defn set-max-battle-id
  [battle-id]
  (sdb/set-attrs (domain-name)
                 "max-battle-id"
                 {:battle-id battle-id}))

(defn ids->team-id
  [ids]
  (string/join "-" ids))

(defn battle->teams
  [battle]
  (->> battle
       :exp-updates
       (group-by :increased-exp)
       (sort-by key)
       (map second)
       (map (partial map :axie-id))
       (map sort)
       (map ids->team-id)
       (zipmap [:loser  :winner])))

(defn set-team-record
  [team-id record]
  (sdb/set-attrs (domain-name)
                 team-id
                 (select-keys record [:wins :losses])))

(defn item->team-record
  [item]
  (-> item
      sdb/item->record
      ((partial merge {:wins 0 :losses 0}))
      (supdate {:wins (fnil bigint 0)
                :losses (fnil bigint 0)})))

(defn fetch-team-record
  [team-id]
  (-> (simpledb/get-attributes
        :domain-name (domain-name)
        :item-name team-id
        :consistent-read true)
      (assoc :name team-id)
      item->team-record))
