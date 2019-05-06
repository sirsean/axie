(ns axie.api
  (:require
    [aleph.http :as http]
    [byte-streams :as bs]
    [camel-snake-kebab.core :refer [->kebab-case-keyword ->camelCaseString]]
    [cheshire.core :as json]
    [clojure.string :as string]
    [manifold.deferred :as md]
    [omniconf.core :as cfg])
  (:gen-class))

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
  (select-keys a [:id :class :name :stats :price :purity :attack :defense :atk+def :total-exp :breed-count]))

(defn mine-keys
  [a]
  (select-keys a [:id :class :name :stats :purity :attack :defense :atk+def :activity-point :total-exp :breed-count]))

(defn adult?
  [{:keys [stage]}]
  (= stage 4))

(defn calc-price
  [axie auction-key]
  (some-> axie
          :auction
          auction-key
          bigdec
          (* 1e-18M)))

(defn attach-price
  [axie]
  (let [buy-now (calc-price axie :buy-now-price)
        suggested (calc-price axie :suggested-price)]
    (assoc axie
           :price buy-now
           :suggested-price suggested
           :price-diff (- (or suggested 0) (or buy-now 0)))))

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
  (assoc axie :atk+def (+ (or attack 0) (or defense 0))))

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

(defn fetch-json
  [url & [opts]]
  (md/chain
    (http/get url (or opts {}))
    body->json))

(defn fetch-page
  [offset]
  (fetch-json (format "https://axieinfinity.com/api/v2/axies?breedable&lang=en&offset=%d&sale=1&sorting=lowest_price" offset)))

(defn total->chapters
  [total]
  (->> total range (partition-all 12) (map first) rest (partition-all 20)))

(defn fetch-pages
  [fetch-page-fn]
  (md/chain
    (fetch-page-fn 0)
    (fn [{:keys [total-axies axies]}]
      (md/loop [all (adjust-axies axies)
                chapters (total->chapters total-axies)]
        (let [[page & chapters] chapters]
          (if-not page
            all
            (md/chain
              (apply md/zip (map fetch-page-fn page))
              (fn [results]
                (md/recur
                  (->> results
                       (mapcat :axies)
                       adjust-axies
                       (concat all))
                  chapters)))))))))

(defn fetch-all
  []
  (fetch-pages fetch-page))

(defn fetch-addr-page
  [address offset]
  (fetch-json (format "https://axieinfinity.com/api/v2/addresses/%s/axies?a=1&offset=%d"
                      address offset)))

(defn fetch-addr
  [address]
  (fetch-pages (partial fetch-addr-page address)))

(defn fetch-activity-points
  [ids]
  (md/chain
    (fetch-json (format "https://api.axieinfinity.com/v1/battle/battle/activity-point?%s"
                        (->> ids
                             (filter some?)
                             (map (partial format "axieId=%s"))
                             (string/join "&"))))
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
    (fetch-json (format "https://api.axieinfinity.com/v1/battle/teams/?address=%s&offset=0&count=47&no_limit=1"
                        (cfg/get :eth-addr)))
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
           (filter adult?)
           (map mine-keys)))))

(defn breedable-axies
  []
  (md/chain
    (md/chain
      (fetch-my-axies)
      #(->> %
            (filter :breedable)
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
    (fetch-json "https://api.axieinfinity.com/v1/battle/history/matches"
                {:headers {"Authorization" (format "Bearer %s" (cfg/get :token))}})
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
    (fetch-json (format "https://axieinfinity.com/api/v2/axies/%d?lang=en" id))
    adjust-axie))

(defn format-decimals
  [k n coll]
  (let [format-template (format "%%.%df" n)]
    (map (fn [x] (update x k (partial format format-template))) coll)))

(defn fetch-leaderboard
  []
  (md/chain
    (fetch-json (format "https://api.axieinfinity.com/v1/battle/history/leaderboard?address=%s"
                        (cfg/get :eth-addr)))
    (partial map (fn [{:keys [wins losses] :as row}]
                   (assoc row :percentage (float (/ wins (+ wins losses))))))))

(defn my-rank
  []
  (md/chain
    (fetch-leaderboard)
    #(->> %
          (filter (fn [{:keys [address]}]
                    (= (string/lower-case address)
                       (string/lower-case (cfg/get :eth-addr))))))))
