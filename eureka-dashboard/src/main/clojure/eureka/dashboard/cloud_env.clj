(ns eureka.dashboard.cloud-env)

(defn get-sys-env [] (System/getenv))

(defn getEnv
  []
  (let [env-name (if-let [system-env-name (.get (get-sys-env) "NETFLIX_ENVIRONMENT")]
                   system-env-name
                   "test")
        region-id (if-let [system-region-id (.get (get-sys-env) "EC2_REGION")]
                    system-region-id
                    "us-east-1")]
    {:region region-id :env env-name}))



