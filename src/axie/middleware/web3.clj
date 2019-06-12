(ns axie.middleware.web3
  (:require
    [clojure.tools.logging :as log]
    [ring.util.http-response :as http-response]
    [axie.web3 :as web3]
    ))

(defn wrap-web3-auth
  [handler]
  (fn [req]
    (let [addr (try
                 (some-> req
                         :headers
                         (get "authorization")
                         ((partial re-find #"Bearer (.+)"))
                         last
                         ((partial web3/ec-recover "axiescope")))
                 (catch Exception e
                   nil))]
      (log/infof "addr: %s" addr)
      (if (some? addr)
        (-> req
            (assoc :addr addr)
            handler)
        (http-response/unauthorized)))))
