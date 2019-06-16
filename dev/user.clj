(ns user
  (:require
    [clojure.tools.namespace.repl :as repl]
    [mount.core :as mount]
    [mount-up.core :as mu]
    [amazonica.aws.simpledb :as simpledb]
    [axie.cmd :as cmd]
    [axie.components.config :as config]
    [axie.components.axie-db :as axie-db]
    [axie.components.cryptonator]
    [axie.components.server]
    [axie.components.payment-processor]
    ))

(mu/on-upndown :info mu/log :before)

(defn reset
  []
  (mount/stop)
  (repl/refresh :after 'mount/start))
