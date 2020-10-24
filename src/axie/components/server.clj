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
    (GET "/card-rankings/:rating-type" [rating-type]
         (json-response (card-rankings/get-rankings (keyword rating-type))))
    (GET "/card-rankings/defense" _
         (json-response (card-rankings/get-rankings :defense)))
    (POST "/card-rankings/:rating-type/:winner/:loser" [rating-type winner loser]
          (json-response (card-rankings/vote (keyword rating-type) winner loser)))
    (GET "/ping" []
         (json-response {:ok true}))))

(def auth-routes
  (compojure/routes
    (GET "/account" {:keys [addr]}
         (if-some [acct (account/fetch addr)]
           (-> acct
               json-response)
           (http-response/not-found)))
    (POST "/register" {:keys [addr]}
          (do
            (account/set-attrs addr {:registered true})
            (-> addr
                account/fetch
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
    ))

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
