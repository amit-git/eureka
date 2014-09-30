(ns eureka.dashboard.subscribe-service-test
  (:import [rx Observable])
  (:require [clojure.test :refer :all]
            [eureka.dashboard.data-source-reg :as data-sources]
            [eureka.dashboard.subscribe-service :as sub-service]
            [rx.lang.clojure.core :as rx]))



(deftype MockChannel []
  org.httpkit.server/Channel
  (open? [ch] (println "mock open"))
  (close [ch] (println "mock close"))
  (websocket? [ch] (println "websocket"))
  (send!
    [ch data] (println "sent data " data))
  (on-receive [ch callback] (println "on-receive"))
  (on-close [ch callback] (println "on-close")))

(defn mock-data-src [_] (Observable/from [3 4 5 6]) )

(defn test-fixture
  [f]
  ;before
  (swap! sub-service/subscriptions empty)
  (f)
  ;after
  )


(use-fixtures :each test-fixture)

(deftest simple-subscribe
  []
  (with-redefs [data-sources/get-data mock-data-src]
    (sub-service/subscribe (MockChannel.) "mock-ds1")
    (is (= 1 (count @sub-service/subscriptions)))
    (sub-service/subscribe (MockChannel.) "mock-ds2")
    (is (= 2 (count @sub-service/subscriptions)))))


(deftest unsub-channel-ds
  []
  (with-redefs [data-sources/get-data mock-data-src]
    (let [ch1 (MockChannel.)
          ds1 "mock-ds1"
          ds2 "mock-ds2"]
      (sub-service/subscribe ch1 ds1)
      (is (= 1 (count @sub-service/subscriptions)))
      (sub-service/subscribe ch1 ds2)
      (is (= 2 (count @sub-service/subscriptions)))
      (sub-service/unsubscribe ch1 ds1)
      (is (= 1 (count @sub-service/subscriptions))))))


(deftest unsub-channel
  []
  (with-redefs [data-sources/get-data mock-data-src]
    (let [ch1 (MockChannel.)
          ds1 "mock-ds1"
          ds2 "mock-ds2"]
      (sub-service/subscribe ch1 ds1)
      (is (= 1 (count @sub-service/subscriptions)))
      (sub-service/subscribe ch1 ds2)
      (is (= 2 (count @sub-service/subscriptions)))
      (sub-service/unsubscribe ch1)
      (is (= 0 (count @sub-service/subscriptions))))))

(comment
  (run-tests))
