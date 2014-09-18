(ns eureka.dashboard.core
  (:gen-class)
  (:use [clojure.core.async :only [go >! chan <!!]]
        [clojure.java.io]
        [compojure.route :only [files not-found] :as route]
        [compojure.handler :only [site]] ; form, query params decode; cookie; session, etc
        [compojure.core :only [defroutes GET POST DELETE ANY context]]
        [org.httpkit.server])
  (:require [org.httpkit.client :as http-client]
            [eureka.dashboard.subscribe-service :as sub-svr]
            [clojure.data.json :as json]
            [eureka.dashboard.data-source-reg :as data-sources]
            [eureka.dashboard.discovery-status :as discovery]))


(defonce server (atom nil))


(defn get-ds
  "parse request to find data source subscribed"
  [client-req]
  (let [{cmd "cmd" ds "ds"} (json/read-str client-req)]
    {:name ds}))

(defn web-socket-handler [request]
  (with-channel request channel
    (on-close channel (fn [status] (println "channel closed: " status) (sub-svr/unsubscribe channel)))
    (on-receive channel (fn [client-req]
                          (println "handling web socket receiv in " (.getId (Thread/currentThread)))
                          (if-let [data-src (get-ds client-req)]
                            (sub-svr/subscribe channel data-src))))))

(defroutes all-routes
  "Composure routes defined including web socket handler"
  (GET "/" [] "Hello amit")
  (GET "/sub" [] web-socket-handler) ;; websocket
  (route/not-found "<p>Page not found.</p>"))

(defn stop-server []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(defn startServer [port]
  (reset! server (run-server (site #'all-routes) {:port port})))

(defn -main []
  (println "Starting websocket server on port 9000")
  (startServer 9000)
  ; init datasources
  (discovery/init-discovery-stream)
  (data-sources/init-atlas-datasources)
  (println "Data sources initialized"))

(comment
  (startServer 9000)
  (.printStackTrace *e)
  (stop-server)

  (def cr "{\"cmd\" : \"getStream\", \"ds\" : \"num-connections\"}")
  (def cr "{\"cmd\" : \"getStream\", \"ds\" : \"5xx\"}")
  (get-ds cr)
  )


