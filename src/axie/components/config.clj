(ns axie.components.config
  (:require
    [mount.core :refer [defstate]]
    [omniconf.core :as cfg]))

(cfg/define
  {:env {:description "Current server environment"
         :type :keyword
         :required true}
   :port {:description "What port to run the server on"
          :type :number
          :required true}})

(defn load-config
  []
  (cfg/populate-from-file ".axie.conf"))

(defstate config
  :start (load-config))
