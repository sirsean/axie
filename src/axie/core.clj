(ns axie.core
  (:require
    [axie.api :refer :all]
    [axie.config]
    [cli-matic.core :as cli]
    [clojure.pprint :refer [pprint print-table]]
    [manifold.deferred :as md]
    [omniconf.core :as cfg])
  (:gen-class))


(cfg/populate-from-file ".axie.conf")

;; TODO
;; determine market price for an axie:
;; - match on: stats, #breeds, parts(?), purity
;; - find out how many matches, what prices they're listing for
;; - what makes a good option for sale?
;; search for good/interesting/powerful parts
;; - find out which parts are good
;; - how can we assign a value to a part, or collection of parts?
;; balanced stats
;; - rather than just sorting by the max of a single stat, can we find a balance across multiple/all stats?

(defn cmd-teams
  [_]
  @(md/chain
     (fetch-teams)
     print-table))

(defn cmd-matches
  [_]
  @(md/chain
     (fetch-matches)
     print-table))

(defn cmd-start
  [_]
  @(md/chain
     (start-battles)
     println))

(defn cmd-unassigned
  [_]
  @(md/chain
     (unassigned-axies)
     print-table))

(def cli-config
  {:app {:command "axie"
         :description "Work with your axies from Axie Infinity."
         :version "0.1.0"}
   :commands [{:command "teams"
               :description "Show all your teams and whether they're ready to battle."
               :runs cmd-teams}
              {:command "matches"
               :description "Show your recent match history."
               :runs cmd-matches}
              {:command "start"
               :description "Send all the teams that are ready off to battle."
               :runs cmd-start}
              {:command "unassigned"
               :description "Which of your axies aren't on a team?"
               :runs cmd-unassigned}]})

(defn -main
  [& args]
  (cli/run-cmd args cli-config))
