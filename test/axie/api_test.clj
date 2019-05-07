(ns axie.api-test
  (:require [clojure.test :refer [deftest is are]]
            [vcr-clj.core :refer [with-cassette]]
            [axie.api :as nsut]))

(def aleph-specs
  [{:var #'aleph.http/get
    :return-transformer (fn [r] (-> r
                                    deref
                                    (update-in [:body]
                                               (fn [b]
                                                 (cond-> b
                                                   (instance? java.io.InputStream b)
                                                   vcr-clj.cassettes.serialization/serializablize-input-stream)))))}])

(deftest test-adult?
  (are [in expected]
       (= expected (nsut/adult? in))

       {}
       false

       {:status 1}
       false

       {:status 4}
       false))

(deftest test-attach-price
  (are [in expected]
       (= expected (select-keys
                     (nsut/attach-price in)
                     [:price :suggested-price :price-diff]))

       {}
       {:price nil
        :suggested-price nil
        :price-diff 0}

       {:auction {:buy-now-price "1000000000000000000"}}
       {:price 1M
        :suggested-price nil
        :price-diff -1M}

       {:auction {:buy-now-price "1000000000000000000"
                  :suggested-price "1500000000000000000"}}
       {:price 1M
        :suggested-price 1.5M
        :price-diff 0.5M}))

(deftest test-attach-purity
  (are [in expected]
       (= expected (select-keys
                     (nsut/attach-purity in)
                     [:purity]))

       {}
       {:purity 0}

       {:class "beast"
        :parts [{:class "reptile"}
                {:class "bird"}
                {:class "beast"}]}
       {:purity 1}

       {:class "bird"
        :parts [{:class "reptile"}
                {:class "bird"}
                {:class "beast"}
                {:class "bird"}]}
       {:purity 2}))

(deftest test-attach-attack
  (are [in expected]
       (= expected (select-keys
                     (nsut/attach-attack in)
                     [:attack]))

       {}
       {:attack 0}

       {:parts [{:moves [{:attack 1}]}
                {:moves [{:attack 2}
                         {:attack 3}]}]}
       {:attack 6}))

(deftest test-attach-defense
  (are [in expected]
       (= expected (select-keys
                     (nsut/attach-defense in)
                     [:defense]))

       {}
       {:defense 0}

       {:parts [{:moves [{:defense 5}]}
                {:moves [{:defense 10}
                         {:defense 20}]}]}
       {:defense 35}))

(deftest test-attach-atk+def
  (are [in expected]
       (= expected (select-keys
                     (nsut/attach-atk+def in)
                     [:atk+def]))

       {}
       {:atk+def 0}

       {:attack 10}
       {:atk+def 10}

       {:defense 15}
       {:atk+def 15}

       {:attack 20
        :defense 30}
       {:atk+def 50}))

(deftest test-attach-total-exp
  (are [in expected]
       (= expected (select-keys
                     (nsut/attach-total-exp in)
                     [:total-exp]))

       {}
       {:total-exp 0}

       {:exp 10}
       {:total-exp 10}

       {:pending-exp 100}
       {:total-exp 100}

       {:exp 14
        :pending-exp 30}
       {:total-exp 44}))

(deftest test-adjust-axies
  (are [in expected]
       (= expected (nsut/adjust-axies in))

       []
       []

       [{:class "plant"
         :exp 11
         :pending-exp 2
         :parts [{:class "plant"
                  :moves [{:attack 1
                           :defense 2}]}]
         :auction {:buy-now-price "1000000000000000000"
                   :suggested-price "1500000000000000000"}}]
       [{:class "plant"
         :exp 11
         :pending-exp 2
         :parts [{:class "plant"
                  :moves [{:attack 1
                           :defense 2}]}]
         :auction {:buy-now-price "1000000000000000000"
                   :suggested-price "1500000000000000000"}
         :attack 1
         :defense 2
         :atk+def 3
         :price 1M
         :suggested-price 1.5M
         :price-diff 0.5M
         :purity 1
         :total-exp 13}]))

(deftest test-sort-axies
  (are [in expected]
       (= expected (apply nsut/sort-axies in))

       [[:c :asc]
        [{:a 1 :b 1 :c 1}
         {:a 1 :b 2 :c 3}
         {:a 2 :b 1 :c 2}]]
       [{:a 1 :b 1 :c 1}
        {:a 2 :b 1 :c 2}
        {:a 1 :b 2 :c 3}]

       [[:a :desc]
        [:c :asc]
        [{:a 1 :b 1 :c 1}
         {:a 1 :b 2 :c 3}
         {:a 2 :b 1 :c 2}]]
       [{:a 2 :b 1 :c 2}
        {:a 1 :b 1 :c 1}
        {:a 1 :b 2 :c 3}]))

(deftest test-team-can-battle?
  (are [in expected]
       (= expected (nsut/team-can-battle? {:team-members in}))

       [{:activity-point 10}
        {:activity-point 20}
        {:activity-point 30}]
       false

       [{:activity-point 720}
        {:activity-point 230}
        {:activity-point 500}]
       false

       [{:activity-point 240}
        {:activity-point 240}]
       false

       [{:activity-point 240}
        {:activity-point 240}
        {:activity-point 240}]
       true

       [{:activity-point 720}
        {:activity-point 720}
        {:activity-point 720}]
       true))

(deftest test-total->chapters
  (are [in expected]
       (= expected (nsut/total->chapters in))

       5
       []

       12
       []

       20
       [[12]]

       500
       [[12 24 36 48 60 72 84 96 108 120 132 144 156 168 180 192 204 216 228 240]
        [252 264 276 288 300 312 324 336 348 360 372 384 396 408 420 432 444 456 468 480]
        [492]]))

(deftest test-fetch-all
  (with-cassette :fetch-all aleph-specs
    (let [axies @(nsut/fetch-all)]
      (is (= 3459 (count axies)))
      (is (= "Axie #48093"
             (->> axies
                  (nsut/sort-axies [:atk+def :desc] [:price :asc])
                  first
                  :name))))))
