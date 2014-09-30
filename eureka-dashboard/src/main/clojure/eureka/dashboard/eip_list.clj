(ns eureka.dashboard.eip-list
  (:require [eureka.dashboard.cloud-env :as cloud-env]))

(def system-env (cloud-env/getEnv))
(def eip-list
  {:test {:us-east-1 ["204.236.228.170" "204.236.228.165" "75.101.165.111" "50.19.255.91"]
          :us-west-2 ["50.112.119.129" "50.112.119.128" "50.112.119.130"]
          :eu-west-1 ["46.137.103.199" "46.137.103.193" "46.137.103.212"]}

   :prod {:us-east-1 ["174.129.253.55" "174.129.215.79" "174.129.254.34" "184.72.237.247"]
          :us-west-2 ["50.112.119.93" "50.112.119.94" "50.112.119.95"]
          :eu-west-1 ["46.137.171.142" "46.137.171.144" "46.137.171.143"]}})

(defn get-eips []
  (get-in eip-list [(keyword (:env system-env))
                    (keyword (:region system-env))]))

(defn pick-rand-eip []
  (let [eips (get-eips)
        len (count eips)
        rand-index (rand-int len)]
    (nth eips rand-index)))

(comment
  (get-eips)
  (pick-rand-eip))
