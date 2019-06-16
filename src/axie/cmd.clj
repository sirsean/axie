(ns axie.cmd
  (:require
    [axie.api :as api]
    [axie.sdb :as sdb]
    [axie.account :as account]
    [axie.family-tree :as family-tree]
    [axie.payment :as payment]
    [amazonica.aws.simpledb :as simpledb]
    [cli-matic.core :as cli]
    [clojure.pprint :refer [pprint print-table]]
    [manifold.deferred :as md]))

(defn create-domains
  [& _]
  (doseq [domain [(account/domain-name)
                  (family-tree/views-domain-name)
                  (payment/domain-name)]]
    (println "creating" domain)
    (simpledb/create-domain :domain-name domain)))

(defn teams
  [& _]
  @(md/chain
     (api/fetch-teams)
     print-table))

(defn team
  [{:keys [team-id]}]
  @(md/chain
     (api/fetch-team team-id)
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

(defn multi-assigned
  [& _]
  @(md/chain
     (api/multi-assigned-axies)
     pprint))

(defn breedable
  [& _]
  @(md/chain
     (api/breedable-axies)
     print-table))

(defn leaderboard
  [& _]
  @(md/chain
     (api/fetch-leaderboard)
     (partial api/format-decimals :percentage 3)
     print-table))

(defn rank
  [& _]
  @(md/chain
     (api/my-rank)
     (partial api/format-decimals :percentage 3)
     print-table))

(defn simulate
  [{:keys [attacker defender]}]
  @(md/chain
     (api/simulate-battle attacker defender)
     print-table))

(def config
  {:app {:command "axie"
         :description "Work with your axies from Axie Infinity."
         :version "0.1.0"}
   :commands [{:command "create-domains"
               :description "Create the necessary SimpleDB domains."
               :runs create-domains}
              {:command "teams"
               :description "Show all your teams and whether they're ready to battle."
               :runs teams}
              {:command "team"
               :description "Get some info on the axies on a team."
               :opts [{:option "team-id" :as "Team ID" :type :string :short 0}]
               :runs team}
              {:command "matches"
               :description "Show your recent match history."
               :runs matches}
              {:command "start"
               :description "Send all the teams that are ready off to battle."
               :runs start}
              {:command "unassigned"
               :description "Which of your axies aren't on a team?"
               :runs unassigned}
              {:command "multi-assigned"
               :description "Which axies are assigned to multiple teams?"
               :runs multi-assigned}
              {:command "breedable"
               :description "Which of your axies are currently breedable?"
               :runs breedable}
              {:command "leaderboard"
               :description "Who's at the top?"
               :runs leaderboard}
              {:command "rank"
               :description "Find your rank."
               :runs rank}
              {:command "simulate"
               :description "Calculate the damage done by each of an attacker's moves against a defender's."
               :opts [{:option "attacker" :as "Attacker Axie ID" :type :int :short 0}
                      {:option "defender" :as "Defender Axie ID" :type :int :short 1}]
               :runs simulate}]})
