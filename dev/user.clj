(ns user
  (:require
    [clojure.tools.namespace.repl :as repl]
    [clojure.java.io :as io]
    [mount.core :as mount]
    [mount-up.core :as mu]
    [axie.cmd :as cmd]
    [axie.components.axie-db]
    [axie.components.cryptonator]
    [axie.components.server]
    [axie.components.payment-processor]
    ))

(mu/on-upndown :info mu/log :before)

(defn reset
  []
  (mount/stop)
  (repl/refresh :after 'mount/start))
