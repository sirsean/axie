(ns axie.core
  (:require
    [aleph.http :as http]
    [byte-streams :as bs]
    [camel-snake-kebab.core :refer [->kebab-case-keyword ->camelCaseString]]
    [cheshire.core :as json]
    [cli-matic.core :as cli]
    [clojure.string :as string]
    [clojure.pprint :refer [pprint print-table]]
    [manifold.deferred :as md]
    [omniconf.core :as cfg])
  (:gen-class))

(cfg/define
  {:eth-addr {:description "My ETH Address"
              :type :string
              :required true}
   :token {:description "Bearer token, from local storage at `axieinfinity.<eth-addr>.auth`"
           :type :string
           :required false}})

(cfg/populate-from-file ".axie.conf")

;; TODO
;; determine market price for an axie:
;; - match on: stats, #breeds, parts(?), purity
;; - find out how many matches, what prices they're listing for
;; - what makes a good option for sale?
;; search for good/interesting/powerful parts
;; - find out which parts are good
;; - how can we assign a value to a part, or collection of parts?
;; balanced stats
;; - rather than just sorting by the max of a single stat, can we find a balance across multiple/all stats?

(def high-attack-parts
  #{"Eggshell" "Golden Shell" "Babylonia" "Candy Babylonia" "5H04L-5T4R" "Cuckoo" "Teal Shell" "Wing Horn" "Clamshell"
    "Geisha" "Piranha" "Hungry Bird" "Risky Fish" "Lam Handsome" "Lam" "Little Owl"
    "Tri Feather" "Blue Moon" "Kingfisher" "Perch" "Raven" "Goldfish" "Anemone" "Cupid" "Furball"
    "Post Fight" "The Last One" "Tadpole" "Cloud" "Nimo" "Ranchu" "Iguana" "Navaga"})

(def high-defense-parts
  #{"Humorless" "Serious" "Silence Whisper" "Herbivore" "Zigzag" "Square Teeth" "Dango" "Razor Bite" "Tiny Turtle"
    "Tiny Dino" "Carrot" "Namek Carrot" "Hot Butt" "Thorny Caterpillar" "Cattail" "Hatsune" "Potato Leaf"
    "Beech" "Yorishiro" "Rose Bud" "Strawberry Shortcake" "Leaf Bug" "Bamboo Shoot" "Golden Bamboo Shoot"
    "Pumpkin" "Red Ear" "Crystal Hermit" "Hermit" "Sponge" "1ND14N-5T4R" "Snail Shell" "Starry Shell"})

(defn search-keys
  [a]
  (select-keys a [:id :class :name :stats :price :purity :attack :defense :atk+def :total-exp]))

(defn mine-keys
  [a]
  (select-keys a [:id :class :name :stats :purity :attack :defense :atk+def :activity-point :total-exp]))

(defn calc-price
  [axie]
  (some-> axie
          :auction
          :buy-now-price
          bigdec
          (* 1e-18M)))

(defn attach-price
  [axie]
  (assoc axie :price (calc-price axie)))

(defn calc-purity
  [{:keys [class] :as axie}]
  (->> axie
       :parts
       (map :class)
       (filter (partial = class))
       count))

(defn attach-purity
  [axie]
  (assoc axie :purity (calc-purity axie)))

(defn calc-attack
  [axie]
  (->> axie
       :parts
       (mapcat :moves)
       (map :attack)
       (apply +)))

(defn attach-attack
  [axie]
  (assoc axie :attack (calc-attack axie)))

(defn calc-defense
  [axie]
  (->> axie
       :parts
       (mapcat :moves)
       (map :defense)
       (apply +)))

(defn attach-defense
  [axie]
  (assoc axie :defense (calc-defense axie)))

(defn attach-atk+def
  [{:keys [attack defense] :as axie}]
  (assoc axie :atk+def (+ attack defense)))

(defn attach-total-exp
  [{:keys [exp pending-exp] :as axie}]
  (assoc axie :total-exp (+ (or exp 0) (or pending-exp 0))))

(defn adjust-axie
  [axie]
  (-> axie
      attach-attack
      attach-defense
      attach-atk+def
      attach-price
      attach-purity
      attach-total-exp))

(defn adjust-axies
  [axies]
  (map adjust-axie axies))

(defn mm
  [c1 c2]
  (map * c1 c2))

(defn ->order
  [o]
  (get {:asc 1
        :desc -1}
       o
       o))

(defn sort-axies
  [& args]
  (let [coll (last args)
        sorting (drop-last 1 args)]
    (sort-by
      (comp vec
            (partial mm (->> sorting (map second) (map ->order)))
            (->> sorting (map first) (apply juxt)))
      coll)))

(defn fuzzy-match-stats
  [s1 s2]
  (->> s1
       keys
       (reduce (fn [ds k]
                 (assoc ds k (Math/abs (- (get s1 k) (get s2 k)))))
               {})))

(defn match-within
  [m n]
  (->> m vals (every? (partial >= n))))

(defn body->json
  [response]
  (-> response
      :body
      bs/to-reader
      (json/parse-stream ->kebab-case-keyword)))

(defn fetch-page
  [offset]
  (md/chain
    (http/get (format "https://axieinfinity.com/api/v2/axies?breedable&lang=en&offset=%d&sale=1&sorting=lowest_price" offset))
    body->json))

(defn fetch-all
  [& {:keys [max-price]
      :or {max-price 0.1M}}]
  (md/chain
    (fetch-page 0)
    (fn [{:keys [total-axies axies]}]
      (md/loop [all (adjust-axies axies)
                offset (count axies)]
        (if (or (<= total-axies offset)
                (< max-price (->> all (map :price) (apply max))))
          all
          (md/chain
            (fetch-page offset)
            (fn [{:keys [axies]}]
              (md/recur (concat all (adjust-axies axies))
                        (+ offset (count axies))))))))))

(defn fetch-addr-page
  [address offset]
  (md/chain
    (http/get (format "https://axieinfinity.com/api/v2/addresses/%s/axies?a=1&offset=%d"
                      address offset))
    body->json))

(defn fetch-addr
  [address]
  (md/chain
    (fetch-addr-page address 0)
    (fn [{:keys [total-axies axies]}]
      (md/loop [all (adjust-axies axies)
                offset (count axies)]
        (if (<= total-axies offset)
          all
          (md/chain
            (fetch-addr-page address offset)
            (fn [{:keys [axies]}]
              (md/recur (concat all (adjust-axies axies))
                        (+ offset (count axies))))))))))

(defn fetch-activity-points
  [ids]
  (md/chain
    (http/get (format "https://api.axieinfinity.com/v1/battle/battle/activity-point?%s"
                      (->> ids
                           (map (partial format "axieId=%s"))
                           (string/join "&"))))
    body->json
    (partial reduce
             (fn [m {:keys [axie-id activity-point]}]
               (assoc m axie-id activity-point))
             {})))

(defn attach-activity-points
  [axies]
  (md/chain
    (fetch-activity-points (map :id axies))
    (fn [id->points]
      (map
        (fn [{:keys [id] :as a}]
          (assoc a :activity-point (id->points id)))
        axies))))

(defn fetch-my-axies
  []
  (md/chain
    (fetch-addr (cfg/get :eth-addr))
    attach-activity-points))

(defn team-can-battle?
  [{:keys [team-members]}]
  (every? (comp (partial <= 240) :activity-point) team-members))

(defn team-ready-in
  [{:keys [team-members]}]
  (->> team-members
       (map :activity-point)
       (apply min)
       (- 240)))

(defn fetch-teams
  [& [show-all-fields]]
  (md/chain
    (http/get (format "https://api.axieinfinity.com/v1/battle/teams/?address=%s&offset=0&count=47&no_limit=1"
                      (cfg/get :eth-addr)))
    body->json
    :teams
    (fn [teams]
      (md/chain
        (fetch-activity-points (->> teams (mapcat :team-members) (map :axie-id)))
        (fn [id->points]
          (map
            (fn [team]
              (update team :team-members (partial map (fn [{:keys [axie-id] :as a}]
                                                        (assoc a :activity-point (id->points axie-id))))))
            teams))))
    (partial map (fn [t]
                   (assoc t
                          :ready? (team-can-battle? t)
                          :ready-in (team-ready-in t))))
    (partial sort-by :ready-in)
    (fn [teams]
      (if show-all-fields
        teams
        (map #(select-keys % [:team-id :name :ready? :ready-in]) teams)))))

(defn axies-on-teams
  []
  (md/chain
    (fetch-teams true)
    (partial mapcat :team-members)
    (partial map :axie-id)
    (partial set)))

(defn unassigned-axies
  []
  (md/chain
    (md/zip
      (fetch-my-axies)
      (axies-on-teams))
    (fn [[mine assigned?]]
      (->> mine
           (remove (comp assigned? :id))
           (map mine-keys)))))

(defn start-battle
  [team-id]
  (md/chain
    (http/post "https://api.axieinfinity.com/v1/battle/battle/queue"
               {:body (json/generate-string
                        {:team-id team-id}
                        {:key-fn ->camelCaseString})
                :headers {"Authorization" (format "Bearer %s" (cfg/get :token))
                          "Content-Type" "application/json"}})
    :body
    bs/to-string
    (partial = "success")))

(defn start-battles
  []
  (md/chain
    (fetch-teams)
    (partial filter :ready?)
    (fn [teams]
      (->> teams (map (comp start-battle :team-id)) (apply md/zip)))
    count))

(defn fetch-matches
  []
  (md/chain
    (http/get "https://api.axieinfinity.com/v1/battle/history/matches"
              {:headers {"Authorization" (format "Bearer %s" (cfg/get :token))}})
    body->json
    :matches
    (partial map (fn [{:keys [id winner loser]}]
                   {:id id
                    :winner (:team-name winner)
                    :winner-user (:name winner)
                    :winner-rating (:delta-rating winner)
                    :loser (:team-name loser)
                    :loser-user (:name loser)
                    :loser-rating (:delta-rating loser)
                    :you-win? (= (string/lower-case (cfg/get :eth-addr))
                                 (string/lower-case (:address winner)))}))))

(defn fetch-axie
  [id]
  (md/chain
    (http/get (format "https://axieinfinity.com/api/v2/axies/%d?lang=en" id))
    body->json
    adjust-axie))

(defn cmd-teams
  [_]
  @(md/chain
     (fetch-teams)
     print-table))

(defn cmd-matches
  [_]
  @(md/chain
     (fetch-matches)
     print-table))

(defn cmd-start
  [_]
  @(md/chain
     (start-battles)
     println))

(defn cmd-unassigned
  [_]
  @(md/chain
     (unassigned-axies)
     print-table))

(def cli-config
  {:app {:command "axie"
         :description "Work with your axies from Axie Infinity."
         :version "0.1.0"}
   :commands [{:command "teams"
               :description "Show all your teams and whether they're ready to battle."
               :runs cmd-teams}
              {:command "matches"
               :description "Show your recent match history."
               :runs cmd-matches}
              {:command "start"
               :description "Send all the teams that are ready off to battle."
               :runs cmd-start}
              {:command "unassigned"
               :description "Which of your axies aren't on a team?"
               :runs cmd-unassigned}]})

(defn -main
  [& args]
  (cli/run-cmd args cli-config))
