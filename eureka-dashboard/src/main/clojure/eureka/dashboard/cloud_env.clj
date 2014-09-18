(ns eureka.dashboard.cloud-env)

(defn getEnv
  []
  (let [env-name (if-let [system-env-name (.get (System/getenv) "NETFLIX_ENVIRONMENT")]
                   system-env-name
                   "test")
        region-id (if-let [system-region-id (.get (System/getenv) "EC2_REGION")]
                    system-region-id
                    "us-east-1")]
    {:region region-id :env env-name}))

