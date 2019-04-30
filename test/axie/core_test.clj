(ns axie.core-test
  (:require [clojure.test :refer [deftest is are]]
            [axie.core :as nsut]))

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
