(ns eureka.dashboard.http-client-wrap-test
  (:use [clojure.core.async :only [timeout put! chan <!! alts!!]])
  (:require [clojure.test :refer :all]
            [eureka.dashboard.http-client-wrap :as wrap]))

(deftest json-parse-good
  []
  (is (= {:name "amit"} (wrap/json-parse "{\"name\": \"amit\"}"))))

(deftest json-parse-bad
  []
  (is (= {} (wrap/json-parse "{ame\": \"amit\"}"))))

(deftest http-get-simple
  []
  (let [c (chan)]
    (wrap/http-get {:url "http://ip.jsontest.com"
                    :jsonout false :callback (fn [ip]
                                               (is (= (type ip) java.lang.String))
                                               (put! c "done"))})
    (alts!! [c (timeout 2000)])))

(deftest http-get-json
  []
  (let [c (chan)]
    (wrap/http-get {:url "http://ip.jsontest.com"
                    :jsonout true :callback (fn [ip-resp]
                                              (put! c (and
                                                        (contains? ip-resp :ip)
                                                        (map? ip-resp))))})
    (is (first (alts!! [c (timeout 3000)])))))

(deftest http-get-bad-url
  []
  (let [c (chan)]
    (wrap/http-get {:url "http://ip.jsontest.com1"
                    :jsonout false :callback (fn [ip]
                                               (put! c (empty? ip)))})
    (is (first (alts!! [c (timeout 3000)])))
    ))

(comment
  (run-tests)
  )



