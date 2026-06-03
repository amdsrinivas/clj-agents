(ns clj-agents.core.agents.schema
  (:require
    [malli.core :as m]
    [malli.error :as me]
    [clj-agents.core.tools.schema :refer [tool-spec]]))

(def agent-spec
  [:map
   [:name :string]
   [:system-prompt :string]
   [:model :string]
   [:tools {:optional true} [:vector tool-spec]]])

(defn- validate [schema data]
  (try
    (if-let [explained (m/explain schema data)]
      {:valid? false, :errors (me/humanize explained)}
      {:valid? true})
    (catch Exception e
      {:valid? false, :errors (str "Schema error: " (.getMessage e))})))

(defn validate-agent [agent] (validate agent-spec agent))