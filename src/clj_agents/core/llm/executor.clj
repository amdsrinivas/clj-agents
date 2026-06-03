(ns clj-agents.core.llm.executor
  (:require
    [clj-http.client :as client]
    [cheshire.core :as cheshire]
    [jsonista.core :as json]
    [clj-agents.core.config :refer [config]]))

(def llm-provider
  {:api_base_url (or (:llm-api-base-url config) "http://localhost:1234")
   :api_endpoint (or (:llm-api-endpoint config) "v1/chat/completions")
   :method client/post
   :api-key (:llm-api-key config)
   :debug? (= "true" (:enable-debug config))})

(defn- log-llm-request
  [url model-config agent-state compiled-tools headers request-data]
  (println)
  (println "[LLM Request] --------------------------------")
  (println (format "  URL:    %s" url))
  (println (format "  Model:  %s" (:model model-config)))
  (println (format "  Tools:  %s" (if (seq (vals compiled-tools)) "yes" "no")))
  (println (format "  Auth:   %s" (if (:authorization headers) "yes" "no")))
  (println "  Body:")
  (println (cheshire/generate-string request-data {:pretty true}))
  (println "----------------------------------------------"))

(defn run-llm
  [{:keys [model-config agent-state compiled-tools]}]
  (let [request-data (cond-> (merge model-config agent-state)
                       (seq (vals compiled-tools)) (assoc :tools (mapv #(:spec %) (vals compiled-tools))))
        llm-request (json/write-value-as-string request-data)
        llm-trigger (:method llm-provider)
        url (format "%s/%s" (:api_base_url llm-provider) (:api_endpoint llm-provider))
        headers {"content-type" "application/json"
                 :authorization (when (seq (:api-key llm-provider))
                                  (format "Bearer %s" (:api-key llm-provider)))}
        prepared-headers (cond-> headers
                           (nil? (:authorization headers)) (dissoc :authorization))
        _ (when (:debug? llm-provider)
            (log-llm-request url model-config agent-state compiled-tools prepared-headers request-data))
        llm-response (llm-trigger url
                                  {:body llm-request
                                   :headers prepared-headers
                                   :as :json
                                   :throw-exceptions false})]
    (when (:debug? llm-provider)
      (println (format "[LLM Response] Status: %s" (:status llm-response)))
      (flush))
    llm-response))