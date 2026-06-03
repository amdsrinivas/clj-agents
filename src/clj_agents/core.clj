(ns clj-agents.core
  (:gen-class)
  (:require
    [clj-agents.core.agents.executor :as agent-executor]
    [clj-agents.core.tools.filesystem :as fs]
    [clj-agents.core.config :refer [config]]))

(defn -main
  [& args]
  (let [workspace-root (or (first args) ".")
        tools [{:name "list_directory"
                :description "List non-hidden files and subdirectories in a directory. Defaults to current directory."
                :input-format [:map [:path {:optional true} :string]]
                :handler fs/list-directory}
               {:name "read_file"
                :description "Read a range of lines from a text file. Start and end are 1-indexed. Returns up to 500 lines per call."
                :input-format [:map [:path :string] [:start :int] [:end :int]]
                :handler fs/read-file}
               {:name "file_info"
                :description "Get metadata about a file or directory: name, path, size, extension, line count (text files only), and last modified time."
                :input-format [:map [:path :string]]
                :handler fs/file-info}]
        agent {:name "file-explorer"
               :system-prompt "You are a codebase explorer assistant. Use list_directory, read_file, and file_info to understand the project structure and contents. Only use tools when you need specific information — for general conversation or simple acknowledgments, respond naturally without calling tools. Explore iteratively — start with the root, then drill into directories. Hidden files (starting with '.') are not accessible. If a tool returns an error, adjust your approach and try again."
               :model (or (:llm-model config) "gemma-4-e4b")
               :tools tools}
        session (agent-executor/start-session agent tools)]
    (binding [fs/*workspace-root* workspace-root]
      (agent-executor/run-agent-loop "Welcome to Filesystem Explorer. (type /exit to exit.)" session))))
