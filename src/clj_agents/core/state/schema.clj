(ns clj-agents.core.state.schema
  (:require
    [malli.core :as m]
    [malli.error :as me]
    [clj-agents.core.llm.schema :as llm-schema]))

(def agent-state
  [:map
   [:messages [:vector llm-schema/core-message]]])

(defn- validate [schema data]
  (try
    (if-let [explained (m/explain schema data)]
      {:valid? false, :errors (me/humanize explained)}
      {:valid? true})
    (catch Exception e
      {:valid? false, :errors (str "Schema error: " (.getMessage e))})))

(defn validate-state [state] (validate agent-state state))