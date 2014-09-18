(ns eureka.dashboard.cloud-env-test
  (:require [clojure.test :refer :all]
            [eureka.dashboard.cloud-env :refer :all]))

(deftest check-defaults
  []
  (let [res (getEnv)]
    (is (= (:env res) "test"))
    (is (= (:region res) "us-east-1"))))

(deftest verify-env
  []
  (with-redefs [System/getenv (doto (java.util.HashMap.) "NETFLIX_ENVIRONMENT" "prod")]
    (let [res (getEnv)]
      (is (= (:env res)) "prod")
      (is (= (:region res) "us-east-1")))))


(deftest verify-region
  []
  (with-redefs [System/getenv (doto (java.util.HashMap.) "EC2_REGION" "us-west-2")]
    (let [res (getEnv)]
      (is (= (:env res)) "test")
      (is (= (:region res) "us-west-2")))))

(comment
  (run-tests))
