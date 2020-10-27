(ns axie.components.cards
  (:require
    [clojure.string :as string]
    [clojure.tools.logging :as log]
    [manifold.deferred :as md]
    [mount.core :refer [defstate]]
    [tea-time.core :as tt]
    [axie.api :refer [fetch-json]]
    ))

(defn fetch-card-abilities
  []
  (fetch-json "https://storage.googleapis.com/axie-cdn/game/cards/card-abilities.json"))

(defn fetch-body-parts
  []
  (fetch-json "https://axieinfinity.com/api/v2/body-parts"))

(defn adjust-body-part-name
  [body-part-name]
  (case body-part-name
    "Thorny Cattepilar" "Thorny Caterpillar"
    "Laggingggggg"      "Laggin"
    body-part-name))

(defn body-parts->map
  [body-parts]
  (->> body-parts
       (mapcat (fn [{:keys [name] :as body-part}]
                 (->> (string/split name #",")
                      (map string/trim)
                      (map adjust-body-part-name)
                      (map (fn [n]
                             [n (select-keys body-part [:part-id :type])])))))
       (into {})))

(defn fetch-cards
  []
  (md/chain
    (md/zip
      (fetch-card-abilities)
      (fetch-body-parts))
    (fn [[card-abilities body-parts]]
      (let [name->part (body-parts->map body-parts)]
        (->> card-abilities
             (map (fn [[id {:keys [part-name] :as card}]]
                    [id (merge card (get name->part part-name))]))
             (into {}))))))

(declare refresh-cards)

(defstate cards
  :start {:cards (atom @(fetch-cards))
          :timer (tt/every! 3600 refresh-cards)}
  :stop (do
            (tt/cancel! (:timer cards))
            {}))

(defn refresh-cards
  []
  (reset! (:cards cards) @(fetch-cards)))

(defn get-cards
  []
  @(:cards cards))
