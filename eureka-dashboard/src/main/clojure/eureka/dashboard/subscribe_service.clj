(ns eureka.dashboard.subscribe-service
  (:use [org.httpkit.server])
  (:require [eureka.dashboard.data-source-reg :as data-sources]
            [rx.lang.clojure.core :as rx]))

(def subscriptions (atom {}))

(defn subscribe
  [channel data-src-name]
  (when-not (get @subscriptions {:ch channel :ds data-src-name})
    (let [data-o (data-sources/get-data data-src-name)
          sub (rx/subscribe data-o
                ; on next value
                (fn [v]
                  (send! channel v)))]
      (swap! subscriptions assoc {:ch channel :ds data-src-name} sub))))

(defn unsubscribe
  ([channel data-src-name]
    "unsubscribe data source from channel"
    (if-let [sub (get @subscriptions {:ch channel :ds data-src-name})]
      (do
        (.unsubscribe sub)
        (swap! subscriptions dissoc {:ch channel :ds data-src-name})
        )))

  ([channel]
    "unsubscribe all data sources for a channel"
    (doseq [key (keys @subscriptions)]
      (let [{ch :ch ds :ds} key]
          (when (= ch channel)
            (unsubscribe ch ds))))))

