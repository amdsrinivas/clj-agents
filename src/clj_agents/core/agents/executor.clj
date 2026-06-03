(ns clj-agents.core.agents.executor
  (:require
    [com.walmartlabs.cond-let :refer [cond-let]]
    [clj-agents.core.tools.executor :as tool-executor]
    [clj-agents.core.llm.executor :as llm-executor]
    [clj-agents.core.agents.schema :as agents-schema]
    [clj-agents.core.llm.schema :as llm-schema]
    [clj-agents.core.state.schema :as state-schema]
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

(defn- validate-state!
  [session context]
  (utils/validate-warn state-schema/validate-state (:agent-state session) (str "agent-state after " context)))

(defn process-model-choice
  [session model-choice]
  (case (:finish_reason model-choice)
    "stop" (let [msg {:role "assistant"
                      :content (get-in model-choice [:message :content])}]
             (utils/validate-warn llm-schema/validate-message msg "assistant stop message")
             (println (format "%s> %s" (:agent-name session) (:content msg)))
             (let [updated-session (update-in session [:agent-state :messages] conj msg)]
               (validate-state! updated-session "stop")
               {:updated-session updated-session
                :re-run-step? false}))
    "tool_calls" (do
                   (println (format "%s> Executing tools calls" (:agent-name session)))
                   (let [tool-call-results (mapv (fn [tool-call]
                                                   (tool-executor/execute-tool-call (:compiled-tools session) tool-call))
                                                 (get-in model-choice [:message :tool_calls]))]
                     (let [updated-session (-> session
                                               (update-in [:agent-state :messages] conj {:role "assistant"
                                                                                         :tool_calls (get-in model-choice [:message :tool_calls])})
                                               (update-in [:agent-state :messages] into tool-call-results))]
                       (validate-state! updated-session "tool_calls")
                       {:updated-session updated-session
                        :re-run-step? true})))

    (doto
      (println (format "Error> Unknown finish reason : %s" model-choice))
      session)))


(defn run-step
  ([session]
   (run-step session nil))
  ([session message]
  (cond-let
    :let [user-msg (when message {:role "user" :content message})
          _ (when user-msg (utils/validate-warn llm-schema/validate-message user-msg "user message"))
          updated-session (cond-> session
                            (some? message) (update-in [:agent-state :messages] conj user-msg))
          _ (utils/validate-warn llm-schema/validate-llm-config (:model-config updated-session) "LLM config")
          llm-response (doto
                         (llm-executor/run-llm updated-session)
                         ;println
                         ;#(flush)
                         )]

    (or (nil? llm-response)
        (not= 200 (:status llm-response)))
    (do
      (println (format "Error> model returned error : %s", llm-response))
      (flush)
      updated-session)

    :let [model-choice (get-in llm-response [:body :choices 0])]

    (nil? model-choice)
    (do
      (println "Warn> no model choice available")
      (flush)
      updated-session)

    :else
    (let [{:keys [updated-session re-run-step?]} (process-model-choice updated-session model-choice)]
      (if re-run-step?
        (run-step updated-session )
        updated-session)))))


(defn start-session
  [agent tools]
  (validate! agents-schema/validate-agent agent "agent configuration")
  {:agent-name (:name agent)
   :model-config {:model (:model agent)}
   :agent-state {:messages [{:role "system"
                             :content (:system-prompt agent)}]}
   :compiled-tools (cond->> tools
                            seq (mapv (partial tool-executor/compile-tool :openai))
                            seq (keep (fn [compiled-tool]
                                        (when-let [name (:name compiled-tool)]
                                          [name compiled-tool])))
                            not-empty (into {}))})

(defn run-agent-loop
  [user-instructions session]
  (println user-instructions)
  (loop [current-session session]
    (print "User> ")
    (flush)
    (let [input (read-line)]
      (when-not (= "/exit" input)
        (println (format "%s> %s" (:agent-name current-session) "thinking..."))
        (flush)
        (let [updated-session (run-step current-session input)]
          ;; Print session updates to screen.
          (recur updated-session))))))
