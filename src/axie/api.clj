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

(defn tank-move-score
  [move-name]
  (case move-name
    "Hot Butt" 4
    "Tiny Dino" 4
    "Pumpkin" 4
    "Red Ear" 4
    "Rose Bud" 4
    "Beech" 4
    "Zigzag" 4
    "Herbivore" 4
    "Carrot" 3
    "Cattail" 3
    "Thorny Caterpillar" 3
    "Hermit" 3
    "Sponge" 3
    "1ND14N-5T4R" 3
    "Snail Shell" 3
    "Shiitake" 3
    "Cerastes" 3
    "Cactus" 3
    "Leaf Bug" 3
    "Serious" 3
    "Silence Whisper" 3
    "Hatsune" 2
    "Bidens" 2
    "Bamboo Shoot" 2
    "Razor Bite" 2
    "Ant" 2
    "Mint" 2
    "Merry" 2
    "Toothless Bite" 2
    "Snake Jar" 2
    "Timber" 2
    "Incisor" 2
    "Tiny Turtle" 2
    "Fish Snack" 2
    "Watermelon" 2
    "Feather Fan" 2
    "Potato Leaf" 2
    "Wall Gecko" 2
    "Gila" 2
    "Grass Snake" 2
    "Koi" 1
    "Bone Sail" 1
    "Lagging" 1
    "Lam" 1
    "Pupae" 1
    "Turnip" 1
    "Scaly Spear" 1
    "Pincer" 1
    "Shrimp" 1
    "Anemone" 1
    "Teal Shell" 1
    "Piranha" 1
    "Croc" 1
    "Babylonia" 1
    "Risky Fish" 1
    "Navaga" 1
    "Green Thorns" 1
    "Pliers" 1
    "Nimo" 1
    "Watering Can" 1
    "Scaly Spoon" 1
    0))

(defn dps-move-score
  [move-name]
  (case move-name
    "The Last One" 4
    "Tri Feather" 4
    "Eggshell" 4
    "Little Owl" 4
    "Post Flight" 4
    "Kingfisher" 4
    "Cactus" 4
    "Nut Cracker" 4
    "Hare" 4
    "Ronin" 4
    "Feather Spear" 4
    "Doubletalk" 4
    "Little Branch" 4
    "Imp" 4
    "Swallow" 3
    "Raven" 3
    "Trump" 3
    "Peace Maker" 3
    "Shiba" 3
    "Cupid" 3
    "Wing Horn" 3
    "Hungry Bird" 3
    "Gerbil" 3
    "Scarab" 3
    "Cuckoo" 3
    "Axie Kiss" 3
    "Furball" 3
    "Shoal Star" 3
    "Cute Bunny" 3
    "Cloud" 3
    "Risky Beast" 3
    "Scaly Spear" 3
    "Tiny Turtle" 3
    "Navaga" 3
    "Jaguar" 3
    "Cerastes" 3
    "Razor Bite" 3
    "Iguana" 3
    "Hero" 3
    "Arco" 3
    "Toothless Bite" 3
    "Shrimp" 3
    "Granma's Fan" 2
    "Pigeon Post" 2
    "Pocky" 2
    "Goda" 2
    "Tadpole" 2
    "Bone Sail" 2
    "Pliers" 2
    "Kotaro" 2
    "Cottontail" 2
    "Perch" 2
    "Dual Blade" 2
    "Piranha" 2
    "Nimo" 2
    "Balloon" 2
    "Lagging" 2
    "Risky Fish" 2
    "Ranchu" 2
    "Blue Moon" 2
    "Parasite" 2
    "Rice" 2
    "Goldfish" 2
    "Kestrel" 2
    "Grass Snake" 2
    "Tri Spikes" 2
    "Bumpy" 2
    "Spiky Wing" 2
    "Babylonia" 2
    "Beech" 2
    "Snake Jar" 1
    "Anemone" 1
    "Teal Shell" 1
    "Catfish" 1
    "Koi" 1
    "Croc" 1
    "Merry" 1
    "Lam" 1
    "Hot Butt" 1
    "Garish Worm" 1
    "Clamshell" 1
    "Herbivore" 1
    "Green Thorns" 1
    "Serious" 1
    "Scaly Spoon" 1
    "Square Teeth" 1
    "Incisor" 1
    "Oranda" 1
    "Unko" 1
    "Watermelon" 1
    "Antenna" 1
    "Caterpillars" 1
    0))

(defn search-keys
  [a]
  (select-keys a [:id :class :name :stats :price :purity :attack :defense :atk+def :tank :dps :total-exp :breed-count]))

(defn mine-keys
  [a]
  (select-keys a [:id :class :name :stats :purity :attack :defense :atk+def :tank :dps :total-exp :breed-count]))

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

(defn attach-dps-score
  [{:keys [parts] :as axie}]
  (assoc axie :dps (->> parts
                        (map :name)
                        (map dps-move-score)
                        (apply +))))

(defn attach-tank-score
  [{:keys [parts] :as axie}]
  (assoc axie :tank (->> parts
                         (map :name)
                         (map tank-move-score)
                         (apply +))))

(defn adjust-axie
  [axie]
  (-> axie
      attach-attack
      attach-defense
      attach-atk+def
      attach-price
      attach-purity
      attach-total-exp
      attach-dps-score
      attach-tank-score))

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

(defn fetch-axie
  [id]
  (md/chain
    (fetch-json (format "https://axieinfinity.com/api/v2/axies/%d?lang=en" id))
    adjust-axie))

(defn fetch-team
  [team-id]
  (md/chain
    (fetch-json (format "https://api.axieinfinity.com/v1/battle/teams/%s" team-id))
    (fn [team]
      (apply md/zip (->> team :team-members (map :axie-id) (map fetch-axie))))
    (partial map mine-keys)))

(defn team-can-battle?
  [{:keys [team-members]}]
  (and (= 3 (count team-members))
       (every? (comp (partial <= 240) :activity-point) team-members)))

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

(defn multi-assigned-axies
  []
  (md/chain
    (fetch-teams true)
    (fn [teams]
      (->> teams
           (mapcat (fn [{:keys [team-id name team-members]}]
                     (map (fn [{:keys [axie-id]}]
                            [axie-id {:team-id team-id
                                      :team-name name}])
                          team-members)))
           (reduce (fn [a->ts [axie-id t]]
                     (update a->ts axie-id conj t)) {})
           (filter (fn [[_ ts]] (< 1 (count ts))))))))

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

(defn fetch-battle
  [battle-id]
  (md/chain
    (fetch-json (format "https://api.axieinfinity.com/v1/battle/history/matches/%s" battle-id))
    (fn [b]
      (update b :script #(json/parse-string % ->kebab-case-keyword)))))

(defn class-advantage
  [a b]
  (case [a b]
    ["plant" "aquatic"]   3
    ["plant" "bird"]      3
    ["reptile" "aquatic"] 3
    ["reptile" "bird"]    3
    ["aquatic" "bug"]     3
    ["aquatic" "beast"]   3
    ["bird" "bug"]        3
    ["bird" "beast"]      3
    ["beast" "plant"]     3
    ["beast" "reptile"]   3
    ["bug" "plant"]       3
    ["bug" "reptile"]     3
    ["aquatic" "plant"]   -3
    ["bird" "plant"]      -3
    ["aquatic" "reptile"] -3
    ["bird" "reptile"]    -3
    ["bug" "aquatic"]     -3
    ["beast" "aquatic"]   -3
    ["bug" "bird"]        -3
    ["beast" "bird"]      -3
    ["plant" "beast"]     -3
    ["reptile" "beast"]   -3
    ["plant" "bug"]       -3
    ["reptile" "bug"]     -3
    0))

(defn simulate-battle
  [attacker-id defender-id]
  (md/chain
    (md/zip
      (fetch-axie attacker-id)
      (fetch-axie defender-id))
    (fn [[attacker defender]]
      (let [defender-class (:class defender)
            attacks (->> attacker
                         :parts
                         (filter (comp seq :moves))
                         (map (fn [{:keys [name class moves]}]
                                {:name name
                                 :class class
                                 :attack (-> moves first :attack)})))
            defenses (->> defender
                          :parts
                          (filter (comp seq :moves))
                          (map (fn [{:keys [name class moves]}]
                                 {:name name
                                  :class class
                                  :defense (-> moves first :defense)})))]
        (for [a attacks
              d defenses]
          {:attack (:name a)
           :defense (:name d)
           :dmg (+ (- (:attack a) (:defense d))
                   (class-advantage (:class a) defender-class)
                   (class-advantage (:class a) (:class d)))})))))

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
