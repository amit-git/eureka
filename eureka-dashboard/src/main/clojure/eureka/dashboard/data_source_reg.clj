(ns eureka.dashboard.data-source-reg
  (:import [rx Observable]
           [java.util.concurrent TimeUnit]
           [rx.subjects BehaviorSubject])
  (:use [clojure.core.async :only [put! go >! chan <! <!!]])
  (:require [eureka.dashboard.discovery-status :as discovery]
            [org.httpkit.client :as http]
            [rx.lang.clojure.core :as rx]
            [clojure.data.json :as json]))

(def rand-num (java.util.Random.))


(def atlas-5xx-count-metric "http://atlas-main.us-east-1.prod.netflix.net:7001/api/v2/fetch?q=nf.region,us-east-1,:eq,nf.app,discovery,:eq,:and,name,EpicAgent_ApacheAccessLog_all_5xx_Count,:eq,:and,:sum&e=now-5m&s=e-3h")
(def atlas-connections-metric "http://atlas-main.us-east-1.prod.netflix.net:7001/api/v2/fetch?q=nf.region,us-east-1,:eq,nf.app,discovery,:eq,:and,state,Keepalive,:eq,:and,name,Scoreboard,:eq,:and,:sum&e=now-5m&s=e-3h")
(def atlas-registry-size-metric "http://atlas-main.us-east-1.prod.netflix.net:7001/api/v2/fetch?q=nf.region,us-east-1,:eq,nf.app,discovery,:eq,:and,name,PeerAwareInstanceRegistry_numOfElementsinInstanceCache,:eq,:and,:sum&e=now-5m&s=e-3h")

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
  [{:keys [status headers body error opts]} data-src]
  (json-out
    (:name data-src)
    (mapcat
      #(build-graph-points %)
      (parse-atlas-resp body))))

(defn make-observable
  [data-src]
  (let [observable (atom nil)]
    (fn []
      (if-not (nil? @observable) @observable
        (let [bs (BehaviorSubject/create (:name data-src))
              ro (Observable/timer 0 1 TimeUnit/MINUTES)
              _  (rx/subscribe ro
                   (fn [v]
                     (println "Making async call for " (:metric-url data-src))
                     (http/get (:metric-url data-src)
                       (fn [res] (.onNext bs (build-atlas-data res data-src))))))]
          (reset! observable bs)
          bs)
        ))))

; get data returns an Observable - specifically BehaviorSubject
(defmulti get-data :name)

(defmethod get-data :default [data-src]
  (.nextInt rand-num 10))

(defmethod get-data "discovery" [data-src]
  (discovery/get-discovery-stream))

(def error-5xx-o (make-observable {:name "error-5xx" :metric-url atlas-5xx-count-metric}))
(defmethod get-data "error-5xx" [data-src] (error-5xx-o))

(def reg-size-o (make-observable {:name "reg-size" :metric-url atlas-registry-size-metric}))
(defmethod get-data "reg-size" [data-src] (reg-size-o))

(def num-conn-o (make-observable {:name "num-connections" :metric-url atlas-connections-metric}))
(defmethod get-data "num-connections" [data-src] (num-conn-o))

(defn init-atlas-datasources
  []
  (error-5xx-o)
  (reg-size-o)
  (num-conn-o))


(comment
  (def co (get-data {:name "reg-size"}))
  (def sub (rx/subscribe co (fn [v] (println "Got Metric " v))))
  (rx/unsubscribe sub)
  (.printStackTrace *e) )
