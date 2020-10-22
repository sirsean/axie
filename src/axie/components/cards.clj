(ns axie.components.cards
  (:require
    [clojure.tools.logging :as log]
    [mount.core :refer [defstate]]
    [tea-time.core :as tt]
    [axie.api :refer [fetch-json]]
    ))

(defn fetch-cards
  []
  (fetch-json "https://storage.googleapis.com/axie-cdn/game/cards/card-abilities.json"))

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
