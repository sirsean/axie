(ns axie.components.server
  (:require
    [clojure.tools.logging :as log]
    [byte-streams :as bs]
    [mount.core :refer [defstate]]
    [omniconf.core :as cfg]
    [manifold.deferred :as md]
    [aleph.http :as http]
    [cheshire.generate]
    [compojure.core :as compojure :refer [ANY POST GET DELETE]]
    [ring.util.http-response :as http-response]
    [ring.middleware.json :refer [wrap-json-response]]
    [ring.middleware.resource :refer [wrap-resource]]
    [ring.middleware.logger :refer [wrap-with-logger]]
    [ring.middleware.cors :refer [wrap-cors]]
    [axie.api :refer [body->json]]
    [axie.components.axie-db :as axie-db]
    [axie.components.card-rankings :as card-rankings]
    [axie.components.cards :as cards]
    [axie.components.config]
    [axie.middleware.web3 :refer [wrap-web3-auth]]
    [axie.account :as account]
    [axie.auto-battle :as auto-battle]
    [axie.family-tree :as family-tree]
    [axie.payment :as payment]
    ))

(extend-protocol cheshire.generate/JSONable
  org.joda.time.LocalDate
  (to-json [dt gen]
    (cheshire.generate/write-string gen (str dt))))

(defn json-response
  [body]
  (-> body
      http-response/ok
      (http-response/header "Content-Type" "application/json")))

(def api-routes
  (compojure/routes
    (GET "/cards" _
         (json-response (cards/get-cards)))
    (GET "/card-rankings" _
         (json-response (card-rankings/get-rankings)))
    (POST "/card-rankings/:winner/:loser" [winner loser]
          (json-response (card-rankings/vote winner loser)))
    (GET "/prices/auto-battle" _
         (json-response (auto-battle/price-list)))
    (GET "/prices/family-tree" _
         (json-response (family-tree/price-list)))
    (GET "/ping" []
         (json-response {:ok true}))))

(def auth-routes
  (compojure/routes
    (GET "/account" {:keys [addr]}
         (if-some [acct (account/fetch addr)]
           (-> acct
               (assoc :family-tree-views (family-tree/fetch-num-views addr))
               json-response)
           (http-response/not-found)))
    (GET "/account/auto-battle" {:keys [addr]}
         (if-some [acct (auto-battle/fetch-customer addr)]
           (json-response acct)
           (http-response/not-found)))
    (POST "/register" {:keys [addr]}
          (do
            (account/set-attrs addr {:registered true})
            (-> addr
                account/fetch
               (assoc :family-tree-views (family-tree/fetch-num-views addr))
                json-response)))
    (POST "/pay" {:keys [addr] :as req}
          (let [{:keys [product txid] :as pymt} (body->json req)]
            (if (and (some? txid) (some? product))
              (let [payment-id (payment/add-payment
                                 (merge pymt
                                        {:status :pending
                                         :addr addr}))]
                (json-response {:id payment-id
                                :txid txid}))
              (http-response/unprocessable-entity))))
    (POST "/auto-battle/signup" {:keys [addr] :as req}
          (let [{:keys [token]} (body->json req)]
            (auto-battle/signup {:addr addr
                                 :token token})
            (json-response (auto-battle/fetch-customer addr))))
    (DELETE "/auto-battle/deactivate" {:keys [addr]}
            (auto-battle/delete addr)
            (json-response {}))
    (GET "/family-tree/views" {:keys [addr]}
         (-> addr
             family-tree/fetch-views
             json-response))
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
          (wrap-cors :access-control-allow-origin #".*"
                     :access-control-allow-methods [:get :post :put :delete])
          wrap-json-response)
      (-> auth-routes
          wrap-web3-auth
          wrap-with-logger
          (wrap-cors :access-control-allow-origin #".*"
                     :access-control-allow-methods [:get :post :put :delete])
          wrap-json-response))))

(defstate server
  :start (do
           (log/infof "serving on port %s" (cfg/get :port))
           (http/start-server handler {:port (cfg/get :port)}))
  :stop (.close server))
