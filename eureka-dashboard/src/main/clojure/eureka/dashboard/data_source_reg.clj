(ns eureka.dashboard.data-source-reg
  (:import [rx Observable]
           [java.util.concurrent TimeUnit]
           [rx.subjects BehaviorSubject])
  (:use [clojure.core.async :only [put! go >! chan <! <!!]])
  (:require [eureka.dashboard.discovery-status :as discovery]
            [eureka.dashboard.cloud-env :as cloud-env]
            [clojure.tools.logging :as log]
            [eureka.dashboard.http-client-wrap :as http]
            [eureka.dashboard.discovery-client-wrapper :as dc-wrapper]
            [rx.lang.clojure.core :as rx]
            [clojure.data.json :as json]))

(def rand-num (java.util.Random.))

(def system-env (cloud-env/getEnv))
(def atlas-5xx-count-metric-f "http://%s:7001/api/v2/fetch?q=nf.region,%s,:eq,nf.app,discovery,:eq,:and,name,EpicAgent_ApacheAccessLog_all_5xx_Count,:eq,:and,:sum&e=now-5m&s=e-3h")
(def atlas-connections-metric-f "http://%s:7001/api/v2/fetch?q=nf.region,%s,:eq,nf.app,discovery,:eq,:and,state,Keepalive,:eq,:and,name,Scoreboard,:eq,:and,:sum&e=now-5m&s=e-3h")
(def atlas-registry-size-metric-f "http://%s:7001/api/v2/fetch?q=nf.region,%s,:eq,nf.app,discovery,:eq,:and,name,PeerAwareInstanceRegistry_numOfElementsinInstanceCache,:eq,:and,:sum&e=now-5m&s=e-3h")

(def timer-subscriptions (atom #{}))

(defn build-atlas-url
  [metric-url-format]
  (format metric-url-format (dc-wrapper/get-instance "atlas_regional-main") (:region system-env)))

(defn is-data-line
  [resp-line]
  (.startsWith resp-line "data: {\"type\":\"timeseries\""))

(defn strip-data-prefix
  [resp-line]
  (.replace resp-line "data: " ""))

(defn json-out
  [data-src graph-points]
  (json/write-str {:source data-src :values graph-points}))

(defn parse-atlas-resp
  "parses atlas response into a map of
   start, end, step data"
  [resp]
  (let [full-data-lines (->>
                          (rest (.split resp "\n"))
                          (filter is-data-line))
        trimmed-data-lines (map strip-data-prefix full-data-lines)
        json-obj-list (map #(json/read-str % :key-fn keyword) trimmed-data-lines)]
    (map
      #(select-keys % [:start :end :step :data])
      json-obj-list)))

(defn build-graph-points
  [graph-data-row]
  (let [values (get-in graph-data-row [:data :values])
        ^long st (:start graph-data-row)
        ^long step (:step graph-data-row)
        total-val (count values)]
    (map
      vector ; [x y] x = timestamp, y = value
      (take total-val (iterate (partial + step) st))
      values)))

(defn build-atlas-data
  [atlas-resp data-src]
  (json-out
    (:name data-src)
    (mapcat
      #(build-graph-points %)
      (parse-atlas-resp atlas-resp))))

(defn make-observable
  [data-src]
  (let [observable (atom nil)]
    (fn []
      (if-not (nil? @observable) @observable
        (let [bs (BehaviorSubject/create (:name data-src))
              ro (Observable/timer 0 1 TimeUnit/MINUTES)
              ts (rx/subscribe ro
                   (fn [v]
                     (let [metric-url ((:metric-url data-src))]
                       (try
                         (do
                           (log/debug "Making async call for " metric-url)
                           (http/http-get {:url metric-url
                                           :jsonout false
                                           :callback (fn [res] (.onNext bs (build-atlas-data res data-src)))}))
                         (catch Exception e (println "Exception in fetching metric " (:name data-src) e))))))]
          (reset! observable bs)
          (swap! timer-subscriptions conj ts)
          bs)
        ))))

; get data returns an Observable - specifically BehaviorSubject
(defmulti get-data :name)

(defmethod get-data :default [data-src]
  (.nextInt rand-num 10))

(defmethod get-data "discovery" [data-src]
  (discovery/get-discovery-stream))

(def error-5xx-o (make-observable {:name "error-5xx"
                                   :metric-url (fn [] (build-atlas-url atlas-5xx-count-metric-f))}))
(defmethod get-data "error-5xx" [data-src] (error-5xx-o))

(def reg-size-o (make-observable {:name "reg-size"
                                  :metric-url (fn [] (build-atlas-url atlas-registry-size-metric-f))}))
(defmethod get-data "reg-size" [data-src] (reg-size-o))

(def num-conn-o (make-observable {:name "num-connections"
                                  :metric-url (fn [] (build-atlas-url atlas-connections-metric-f))}))
(defmethod get-data "num-connections" [data-src] (num-conn-o))

(defn init-atlas-datasources
  []
  (error-5xx-o)
  (reg-size-o)
  (num-conn-o))

(defn shutdown
  []
  (map
    #(.unsubscribe %)
    @timer-subscriptions))

(comment
  (def co (get-data {:name "num-connections"}))
  (def sub (rx/subscribe co (fn [v] (println "Got Metric " v))))
  (build-atlas-url atlas-5xx-count-metric-f)
  (rx/unsubscribe sub)
  (.printStackTrace *e))
