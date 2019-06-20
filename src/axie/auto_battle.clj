(ns axie.auto-battle
  (:require
    [clojure.tools.logging :as log]
    [clojure.string :as string]
    [clj-time.core :as tc]
    [clj-time.format :as tf]
    [manifold.deferred :as md]
    [amazonica.aws.simpledb :as simpledb]
    [omniconf.core :as cfg]
    [axie.sdb :as sdb]
    [axie.price :as price]
    [vvvvalvalval.supdate.api :refer [supdate]]
    )
  (:import
    (java.math BigDecimal)))

(defn domain-name
  []
  (format "axie.%s.auto-battles" (name (cfg/get :env))))

(defn format-date
  [ld]
  (tf/unparse-local-date (tf/formatter "yyyy-MM-dd") ld))

(defn parse-date
  [s]
  (tf/parse-local-date s))

(def base-prices
  [{:max-teams 3   :usd 0}
   {:max-teams 10  :usd 3}
   {:max-teams 30  :usd 6}
   {:max-teams 100 :usd 10}
   {:max-teams nil :usd 20}])

(defn price-list
  []
  (->> base-prices
       price/attach-eth-prices))

(defn tier-by-max-teams
  [max-teams]
  (->> (price-list)
       (filter #(= max-teams (:max-teams %)))
       first))

(defn calc-days
  [{:keys [eth]} amount]
  (let [per-day (.divide (bigdec eth)
                         30M
                         10
                         BigDecimal/ROUND_HALF_UP)]
    (.divide (bigdec amount)
             per-day
             0
             BigDecimal/ROUND_HALF_UP)))

(defn expired?
  [until]
  (tc/before? until (tc/today)))

(defn ->max-teams
  [{:keys [max-teams until]}]
  (cond
    (nil? until) max-teams
    (expired? until) 3
    :else max-teams))

(defn blank->nil
  [s]
  (when (not (string/blank? s))
    s))

(defn item->record
  [item]
  (-> item
      sdb/item->record
      (supdate {:max-teams #(some-> % blank->nil bigint)
                :until #(some-> % blank->nil parse-date)})))

(defn fetch-customers
  []
  (->> (sdb/select-all
         (format "select * from `%s`" (domain-name))
         true)
       (map item->record)))

(defn fetch-customer
  [addr]
  (let [result (simpledb/get-attributes
                 :domain-name (domain-name)
                 :item-name addr
                 :consistent-read true)]
    (when (seq (:attributes result))
      (-> result
          (assoc :name addr)
          item->record))))

(defn signup
  [{:keys [addr token]}]
  (when-not (fetch-customer addr)
    (sdb/set-attrs (domain-name)
                   addr
                   {:token token
                    :max-teams 3})))

(defn upgrade
  [addr {:keys [max-teams until]}]
  (sdb/set-attrs (domain-name)
                 addr
                 {:max-teams max-teams
                  :until (some-> until format-date)}))
