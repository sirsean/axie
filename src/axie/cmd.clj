(ns axie.cmd
  (:require
    [axie.api :as api]
    [cli-matic.core :as cli]
    [clojure.pprint :refer [pprint print-table]]
    [manifold.deferred :as md]))

(defn teams
  [& _]
  @(md/chain
     (api/fetch-teams)
     print-table))

(defn matches
  [& _]
  @(md/chain
     (api/fetch-matches)
     print-table))

(defn start
  [& _]
  @(md/chain
     (api/start-battles)
     println))

(defn unassigned
  [& _]
  @(md/chain
     (api/unassigned-axies)
     print-table))

(def config
  {:app {:command "axie"
         :description "Work with your axies from Axie Infinity."
         :version "0.1.0"}
   :commands [{:command "teams"
               :description "Show all your teams and whether they're ready to battle."
               :runs teams}
              {:command "matches"
               :description "Show your recent match history."
               :runs matches}
              {:command "start"
               :description "Send all the teams that are ready off to battle."
               :runs start}
              {:command "unassigned"
               :description "Which of your axies aren't on a team?"
               :runs unassigned}]})
