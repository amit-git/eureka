(ns eureka.dashboard.discovery-status
  (:import [rx Observable]
           [java.util.concurrent TimeUnit]
           [rx.subjects BehaviorSubject])
  (:use [clojure.core.async :only [go put! <! >! chan <!!]])
  (:require [org.httpkit.client :as http]
            [rx.lang.clojure.core :as rx]
            [eureka.dashboard.cloud-env :as cloud-env]
            [clojure.data.json :as json]))

(def lease-expiration-str "Lease expiration enabled: true")
(def timer-subscription (atom nil))
(def system-env (cloud-env/getEnv))

(defn http-get [url]
  (let [c (chan)]
    (println "Fetching " url)
    (http/get url
      (fn [r] (put! c r)))
    c))

(defn get-discovery-eips
  []
  (go
    (let [json-payload (-> (format "http://eipapi.%s.netflix.net/api/v1/eip/%s" (:env system-env) (:region system-env))
                         http-get
                         <!
                         :body (json/read-str))]
      (->> json-payload
        (filter
          (fn [[_ v]] (= v "discovery")))
        (map first)))))


(defn get-discovery-status
  [eip]
  (go
    (let [status-page (-> (format "http://%s:7001/discovery" eip)
                        http-get
                        <!
                        :body)
          zone-match (re-find #"Zone: ([\w\d-]*)" status-page)
          status (.contains status-page lease-expiration-str)]
      (if status
        ; good status
        {:eip eip :zone (second zone-match) :status "G"}
        ; self preservation
        {:eip eip :zone (second zone-match) :status "Y"}))))

(defn get-discovery-snapshot
  [inst]
  (go
    (let [apps-json-resp (-> (format "http://%s:8077/v1/platform/base/discovery" inst)
                           http-get
                           <!
                           :body (json/read-str :key-fn keyword))
          apps-full-rec (get-in apps-json-resp [:discovery :rows])
          apps-short (map
                       (fn [[app inst status]] ; extract first three values
                         ;(let [[app inst status :as entire-rec] app-rec]
                         {:app app :instance inst :status status})
                       apps-full-rec)
          apps-up-status (filter #(= "UP" (:status %)) apps-short)
          app-inst-pairs (map #(dissoc % :status) apps-up-status)
          only-apps (map :app app-inst-pairs)]
      (->>
        (reduce
          ; app frequency count map
          #(assoc %1 %2 (inc (%1 %2 0)))
          {} ; initial count map
          only-apps)
        ; sort by second field ie count
        (sort-by second >)))))

(defn get-data-stream
  []
  (go
    (let [eips (<! (get-discovery-eips))
          eip-status-chans (map get-discovery-status eips)
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
                     (let [system-status (<!! (get-data-stream))]
                       (->> {:source "discovery" :values system-status}
                         (json/write-str)
                         (.onNext bs))))
                   ; on error
                   (fn [v]
                     (.onError bs)))]
          (reset! timer-subscription ts)
          (reset! observable bs)
          bs)))))

(def ob-maker (make-system-status-observable))
(defn get-discovery-stream [] (ob-maker))

(defn init-discovery-stream [] (ob-maker))
(defn shutdown-discovery-stream [] (.unsubscribe @timer-subscription))

(comment
  (get-system-status)
  (<!! (get-discovery-eips))
  (<!! (go
         (->>
           (get-discovery-eips)
           <!
           (map get-discovery-status)
           )))

  (<!! (go
         (let [eips (<! (get-discovery-eips))
               eip-status-chans (map get-discovery-status eips)
               async-res-chan (clojure.core.async/map vector eip-status-chans)
               servers (<! async-res-chan)
               eip-up-inst (first (filter #(= "G" (:status %)) servers))
               reg-snapshot (<! (get-discovery-snapshot (:eip eip-up-inst)))]
           {:servers servers :registry reg-snapshot})))
  (def s-obr (get-observable-stream))
  (def sub (rx/subscribe s-obr (fn [ss] (println "Got system status " ss))))
  (.unsubscribe sub))

