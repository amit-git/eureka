(ns eureka.dashboard.http-client-wrap
  (:require [org.httpkit.client :as http]
            [clojure.tools.logging :as log]
            [clojure.data.json :as json]))

(defn json-parse
  [resp]
  (try
    (json/read-str resp :key-fn keyword)
    (catch Exception ex (do (log/debug "Exception in json parsing " resp) {}))))

(defn do-callback
  [resp jsonout callback]
  (if (true? jsonout)
    (callback (json-parse resp))
    (callback resp)))

(defn http-get
  [{:keys [url jsonout callback]}]
  (log/debug "Async calling " url)
  (http/get url
    (fn [res]
      (let [{:keys [status headers body error _]} res]
        (if (not (= status 200))
          (do
            (log/debug "Error (" status ") in calling " url " error => " error)
            (do-callback "" jsonout callback))
          (do-callback body jsonout callback))
        ))))

(defn http-get-resp-header
  [{:keys [url header-name]}]
  (log/debug "Getting headers for " url)
  (let [{:keys [status headers body error _]} @(http/get url {:follow-redirects false})]
    (get headers (keyword (.toLowerCase header-name)))))

(comment
  (http-get {:url "http://ip.jsontest.com" :jsonout false :callback (fn [ip] (println "My ip is " ip))})
  (json-parse "{\"name\": \"amit\"}")
  (json-parse "{me\": \"amit\"}")

  )
