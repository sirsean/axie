(ns axie.components.timer
  (:require
    [mount.core :refer [defstate]]
    [tea-time.core :as tt]
    ))

(defstate timer
  :start (tt/start!)
  :stop (tt/stop!))
