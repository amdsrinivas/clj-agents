(ns clj-agents.core.tools.schema
  (:require
    [malli.core :as m]
    [malli.error :as me]))

(def tool-spec
  [:map
   [:name :string]
   [:description :string]
    [:input-format {:optional true} :any]
   [:handler fn?]])

(def tool-call
  [:map
   [:id :string]
   [:type :string]
   [:function [:map
               [:name :string]
               [:arguments :string]]]])

(def tool-call-result
  [:map
   [:role :string]
   [:tool_call_id {:optional true} :string]
   [:content :string]])

(defn- validate [schema data]
  (try
    (if-let [explained (m/explain schema data)]
      {:valid? false, :errors (me/humanize explained)}
      {:valid? true})
    (catch Exception e
      {:valid? false, :errors (str "Schema error: " (.getMessage e))})))

(defn validate-tool [tool] (validate tool-spec tool))
(defn validate-tool-call [call] (validate tool-call call))
(defn validate-tool-result [result] (validate tool-call-result result))