(ns axie.core
  (:require
    [mount.core :as mount]
    [mount-up.core :as mu]
    [axie.api :refer :all]
    [axie.cmd :as cmd]
    [axie.config]
    [axie.components.axie-db]
    [axie.components.cryptonator]
    [axie.components.server]
    [axie.components.payment-processor]
    [cli-matic.core :as cli]
    [clojure.pprint :refer [pprint print-table]]
    [manifold.deferred :as md]
    [omniconf.core :as cfg])
  (:gen-class))

(cfg/populate-from-file ".axie.conf")

(mu/on-upndown :info mu/log :before)

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

(defn -main
  [& args]
  (cli/run-cmd args cmd/config))
