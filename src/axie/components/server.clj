(ns axie.components.server
  (:require
    [mount.core :refer [defstate]]
    [manifold.deferred :as md]
    [aleph.http :as http]
    [compojure.core :as compojure :refer [ANY POST GET]]
    [ring.util.http-response :as http-response]
    [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
    [ring.middleware.resource :refer [wrap-resource]]
    [ring.middleware.logger :refer [wrap-with-logger]]
    [axie.components.axie-db :as axie-db]
    [axie.middleware.web3 :refer [wrap-web3-auth]]
    [axie.account :as account]
    [axie.family-tree :as family-tree]
    [axie.payment :as payment]
    ))

(defn json-response
  [body]
  (-> body
      http-response/ok
      (http-response/header "Content-Type" "application/json")))

(def api-routes
  (compojure/routes
    (GET "/prices/family-tree" _
         (json-response (family-tree/price-list)))
    (GET "/ping" []
         (json-response {:ok true}))))

(def auth-routes
  (compojure/routes
    (GET "/account" {:keys [addr]}
         (-> addr
             account/fetch
             (assoc :family-tree-views (family-tree/fetch-num-views addr))
             json-response))
    (POST "/pay" {{:keys [product txid]} :body
                  addr :addr}
          (let [payment-id (payment/add-payment {:product product
                                                 :status :pending
                                                 :txid txid
                                                 :addr addr})]
                (json-response {:id payment-id
                                :txid txid})))
    (GET "/family-tree/:axie-id" {{:keys [axie-id]} :params
                                  addr :addr}
         ;; have they already viewed it, or do they have enough paid views left?
         (if (or (family-tree/viewed? addr axie-id)
                 (let [{:keys [family-tree-paid]} (account/fetch addr)
                       num-views (family-tree/fetch-num-views addr)]
                   (< num-views family-tree-paid)))
           (do
             (family-tree/view! addr axie-id)
             (md/chain
               (axie-db/fetch axie-id)
               family-tree/family-tree
               json-response))
           (http-response/payment-required)))))

(def handler
  (compojure/routes
    (compojure/context
      "/api" []
      (-> api-routes
          wrap-with-logger
          wrap-json-response
          (wrap-json-body {:keywords? true}))
      (-> auth-routes
          wrap-with-logger
          wrap-web3-auth
          wrap-json-response
          (wrap-json-body {:keywords? true})))))

(defstate server
  :start (http/start-server handler {:port 4321})
  :stop (.close server))
