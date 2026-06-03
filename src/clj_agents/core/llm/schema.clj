(ns clj-agents.core.llm.schema
  (:require
    [malli.core :as m]
    [malli.error :as me]
    [clj-agents.core.tools.schema :as tool-schema]))

(def assistant-message
  [:map
   [:role [:= "assistant"]]
   [:content {:optional true} :string]
   [:reasoning_content {:optional true} :string]
   [:tool_calls {:optional true} [:vector tool-schema/tool-call]]])

(def tool-message
  [:map
   [:role [:= "tool"]]
   [:tool_call_id :string]
   [:content :string]])

(def core-message
  [:multi {:dispatch :role}
   ["user"
    [:map
     [:role [:= "user"]]
     [:content :string]]]
   ["system"
    [:map
     [:role [:= "system"]]
     [:content :string]]]
   ["assistant"
    assistant-message]
   ["tool"
    tool-message]])

(def llm-config
  [:map
   [:model :string]])

(defn- validate [schema data]
  (try
    (if-let [explained (m/explain schema data)]
      {:valid? false, :errors (me/humanize explained)}
      {:valid? true})
    (catch Exception e
      {:valid? false, :errors (str "Schema error: " (.getMessage e))})))

(defn validate-llm-config [config] (validate llm-config config))
(defn validate-message [msg] (validate core-message msg))
