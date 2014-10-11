(ns eureka.dashboard.core
  (:gen-class)
  (:use [clojure.core.async :only [go >! chan <!!]]
        [clojure.java.io]
        [ring.util.response :only [redirect]]
        [compojure.route :only [files not-found] :as route]
        [compojure.handler :only [site]] ; form, query params decode; cookie; session, etc
        [compojure.core :only [defroutes GET POST DELETE ANY context]]
        [clojure.tools.nrepl.server :only (start-server stop-server) :as nrepl]
        [org.httpkit.server])
  (:require [org.httpkit.client :as http-client]
            [clojure.tools.logging :as log]
            [eureka.dashboard.subscribe-service :as sub-svr]
            [eureka.dashboard.cloud-env :as cloud-env]
            [clojure.data.json :as json]
            [eureka.dashboard.data-source-reg :as data-sources]
            [eureka.dashboard.discovery-status :as discovery]))

(defonce server (atom nil))
(defonce nrepl-server (atom nil))

(defn get-ds
  "parse request to find data source subscribed"
  [client-req]
  (let [{cmd "cmd" ds "ds"} (json/read-str client-req)]
    {:name ds}))

(defn healthcheck-handler [req]
  (if (nil? @server)
    {:status  500
     :headers {"Content-Type" "text/html"}
     :body    "Server did not start properly."}
    {:status  200
     :headers {"Content-Type" "text/html"}
     :body    "Good to go"})
  )

(defn get-env [req]
  (let [env (cloud-env/getEnv)]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str env)}))

(defn web-socket-handler [request]
  (with-channel request channel
    (on-close channel (fn [status] (log/debug "channel closed: " status) (sub-svr/unsubscribe channel)))
    (on-receive channel (fn [client-req]
                          (log/debug "handling web socket received in " (.getId (Thread/currentThread)))
                          (if-let [data-src (get-ds client-req)]
                            (sub-svr/subscribe channel data-src))))))

(defroutes all-routes
  "Composure routes defined including web socket handler"
  (GET "/sub" [] web-socket-handler) ;; websocket
  (GET "/healthcheck" [] healthcheck-handler)
  (GET "/getenv" [] get-env)
  (route/resources "/")
  (route/not-found "<p>Page not found.</p>"))

(def app
  (->
    (site #'all-routes)))

(defn stop-app-server []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(defn stop-nrepl-server []
  (when-not (nil? @nrepl-server)
    (nrepl/stop-server @nrepl-server)
    (reset! nrepl-server nil)))

(defn start-nrepl-server []
  (future (reset! nrepl-server (nrepl/start-server :port 7888))))

(defn start-app-server [port]
  (reset! server (run-server #'app {:port port})))

(defn shutdown
  []
  (stop-app-server)
  (discovery/shutdown-discovery-stream)
  (data-sources/shutdown))

(defn bootstrap
  [port]
  (log/info (str "Starting websocket server on port " port))
  (start-app-server port)
  (discovery/init-discovery-stream)
  (data-sources/init-atlas-datasources)
  (log/info "Data sources initialized"))


(defn -main [& args]
  (let [port (Integer/parseInt (first args))]
    (bootstrap port)
    (start-nrepl-server)))

(comment
  (-main "8080")
  (bootstrap 8080)

  (get-env "")
  (shutdown))
