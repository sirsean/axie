(ns axie.aws
  (:require
    [axie.api :as axie]
    [axie.config]
    [byte-streams :as bs]
    [manifold.deferred :as md]
    [omniconf.core :as cfg])
  (:gen-class
    :methods [^:static [start [] String]]))

(cfg/populate-from-env)

(defn -start
  []
  (println "starting battles" (cfg/get :eth-addr))
  @(-> (md/chain
         (axie/start-battles)
         println)
       (md/catch (fn [e]
                   (when-some [body (:body (ex-data e))]
                     (println (bs/to-string body)))
                   (throw e)))))
