(ns eureka.dashboard.discovery-client-wrapper
  (:require [eureka.dashboard.cloud-env :as cloud-env]
            [eureka.dashboard.eip-list :as eip-list]
            [eureka.dashboard.http-client-wrap :as http]))


(defn get-instance
  [cluster]
  (let [ eip (eip-list/pick-rand-eip)
         redirect-url (http/http-get-resp-header
                       {:url (format "http://%s:7001/discovery/resolver/cluster/%s" eip cluster)
                        :header-name "Location"})]
    (second (re-find #"http://(.*):7001" redirect-url))))

(comment
  (get-instance "atlas_regional"))



