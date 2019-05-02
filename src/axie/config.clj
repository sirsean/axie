(ns axie.config
  (:require
    [omniconf.core :as cfg]))

(cfg/define
  {:eth-addr {:description "My ETH Address"
              :type :string
              :required true}
   :token {:description "Bearer token, from local storage at `axieinfinity.<eth-addr>.auth`"
           :type :string
           :required false}})
