(ns axie.api-test
  (:require [clojure.test :refer [deftest is are]]
            [axie.api :as nsut]))

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
        {:activity-point 240}
        {:activity-point 240}]
       true

       [{:activity-point 720}
        {:activity-point 720}
        {:activity-point 720}]
       true))
