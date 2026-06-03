(ns clj-agents.core.tools.executor
  (:require
    [com.walmartlabs.cond-let :refer [cond-let]]
    [malli.json-schema :as mj]
    [jsonista.core :as json]
    [clj-agents.core.tools.schema :as schema]
    [clj-agents.core.utils :as utils]))

(defn- validate!
  [schema-fn data context]
  (try
    (when-let [result (schema-fn data)]
      (when-not (:valid? result)
        (throw (ex-info (str "Validation failed: " context) result))))
    (catch Exception e
      (throw (ex-info (str "Validation error for " context ": " (.getMessage e))
                      {:context context, :data data} e)))))

(defn compile-tool
  [provider tool]
  (validate! schema/validate-tool tool "tool definition")
  (case provider
    :openai (let [{:keys [name description input-format handler]} tool]
              {:name name
               :spec {:type :function
                      :function {:name name
                                 :description description
                                 :parameters (mj/transform input-format)}}
               :handler handler})
    {:status :failed
     :reason :unsupported-provider}))

(defn execute-tool-call
  [tools {:keys [id function] :as call}]
  (utils/validate-warn schema/validate-tool-call call "LLM tool call")
  (cond-let
    :let [{:keys [arguments]} function
          tool (get tools (:name function))]

    (nil? tool)
    {:role "system"
     :content (format "The tool '%s' does not exist. Only use the provided tools" name)}

    :let [args (json/read-value arguments utils/json-keyword-decode-mapper)
          handler (:handler tool)
          tool-result (do
                        (println (format "%s> Executing with %s" (:name function) arguments))
                        (flush)
                        (handler args))]

    :else
    (let [result {:role "tool"
                  :tool_call_id id
                  :content (json/write-value-as-string tool-result utils/json-keyword-decode-mapper)}]
      (utils/validate-warn schema/validate-tool-result result "tool result")
      (do
        (println (format "%s> Result : %s" (:name function) tool-result))
        result))))
