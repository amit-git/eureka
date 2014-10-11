(ns eureka.dashboard.discovery-status
  (:import [rx Observable]
           [java.util.concurrent TimeUnit]
           [rx.subjects BehaviorSubject])
  (:use [clojure.core.async :only [go put! <! >! chan <!!]])
  (:require [eureka.dashboard.http-client-wrap :as http]
            [eureka.dashboard.eip-list :as eip-list]
            [rx.lang.clojure.core :as rx]
            [clojure.tools.logging :as log]
            [clojure.data.json :as json]))

(def lease-expiration-str "Lease expiration enabled: true")
(def timer-subscription (atom nil))


(defn http-get [{:keys [url jsonout]}]
  (let [c (chan)]
    (log/debug "Fetching " url)
    (http/http-get {:url url :callback (fn [r] (put! c r)) :jsonout jsonout})
    c))

(defn http-get-json [url] (http-get {:url url :jsonout true}))
(defn http-get-raw [url] (http-get {:url url :jsonout false}))


(defn get-discovery-status
  [eip]
  (go
    (let [status-page (-> (format "http://%s:7001/discovery" eip)
                        http-get-raw
                        <!)
          zone-match (re-find #"Zone: ([\w\d-]*)" status-page)
          status (.contains status-page lease-expiration-str)]
      (if status
        ; good status
        {:eip eip :zone (second zone-match) :status "G"}
        ; self preservation
        {:eip eip :zone (second zone-match) :status "Y"}))))


(defn group-by-status
  "group instances by their status. Returns a map containing status -> [list of instance ids]"
  [inst-list]
  (->>
    (group-by :status inst-list)
    (map
      (fn [[stat inst-list-with-stat]]
        (let [inst-list (mapv :instance inst-list-with-stat)]
          {stat inst-list})))
    (into {})))

(defn get-discovery-snapshot
  [inst]
  (go
    (let [apps-json-resp (-> (format "http://%s:8077/v1/platform/base/discovery" inst)
                           http-get-json
                           <!)
          apps-full-rec (get-in apps-json-resp [:discovery :rows])
          apps-short (map
                       (fn [[app inst status]] ; extract first three values
                         ;(let [[app inst status :as entire-rec] app-rec]
                         {:app app :instance inst :status status})
                       apps-full-rec)]
      (map
        (fn [[appId inst-list]]
          (let [no-app-inst-list (map #(dissoc % :app) inst-list)]
            {:app appId :count (count inst-list) :instances (group-by-status no-app-inst-list)}
            ))
        (group-by :app apps-short)))))

(defn get-data-stream
  []
  (go
    (let [discovery-hosts (eip-list/get-ec2-names)
          eip-status-chans (map get-discovery-status discovery-hosts)
          async-res-chan (clojure.core.async/map vector eip-status-chans)
          servers (<! async-res-chan)
          eip-up-inst (first (filter #(= "G" (:status %)) servers))
          reg-snapshot (<! (get-discovery-snapshot (:eip eip-up-inst)))]
      {:servers servers :registry reg-snapshot})))

(defn make-system-status-observable
  []
  (let [observable (atom nil)]
    (fn []
      (if-not (nil? @observable) @observable
        (let [bs (BehaviorSubject/create (:name "system-status"))
              ro (Observable/timer 0 5 TimeUnit/MINUTES)
              ts (rx/subscribe ro
                   ; on next
                   (fn [v]
                     (try
                       (let [system-status (<!! (get-data-stream))]
                         (->> {:source "discovery" :values system-status}
                           (json/write-str)
                           (.onNext bs)))
                       (catch Exception ex
                         (log/debug "Exception in get-data-stream " ex))))
                   ; on error
                   (fn [v]
                     (log/debug "Error from timer sub ? should not happen.")
                     (.onError bs)))]
          (reset! timer-subscription ts)
          (reset! observable bs)
          bs)))))

(def ob-maker (make-system-status-observable))
(defn get-discovery-stream [] (ob-maker))

(defn init-discovery-stream [] (ob-maker))
(defn shutdown-discovery-stream [] (.unsubscribe @timer-subscription))

(comment
  (<!! (get-discovery-eips))
  (rx/subscribe (get-discovery-stream)
    (fn [v]
      (println "value from observable " v)))

  (<!! (get-data-stream))

  (def s-obr (get-observable-stream))
  (def sub (rx/subscribe s-obr (fn [ss] (println "Got system status " ss))))
  (.unsubscribe sub)
  (<!! (get-discovery-snapshot "ec2-54-205-23-0.compute-1.amazonaws.com"))

  (def apps [{:app "map" :instance "i-1" :status "UP"}
             {:app "map" :instance "i-4" :status "STARTING"}
             {:app "api" :instance "i-7" :status "STARTING"}
             {:app "vms" :instance "i-10" :status "UP"}
             {:app "vms" :instance "i-12" :status "OOS"}
             {:app "vms" :instance "i-22" :status "UP"}
             {:app "api" :instance "i-33" :status "UP"}])

  (map
    (fn [[appId inst-list]]
      (let [no-app-inst-list (map #(dissoc % :app) inst-list)]
        {:app appId :count (count inst-list) :instances (group-by-status no-app-inst-list)}
        ))
    (group-by :app apps))



  (group-by-status [{:status "UP" :instance "i-1"}
                    {:instance "i-33" :status "STARTING"}
                    {:instance "i-30" :status "STARTING"}
                    {:instance "i-20" :status "UP"}
                    {:status "BAD" :instance "i-20"}])



  )

