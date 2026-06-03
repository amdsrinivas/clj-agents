(ns clj-agents.core.utils
  (:require [jsonista.core :as json]
            [clj-agents.core.config :refer [config]]))

(defn partition-by-key
  "Partitions a collection into vectors based on key values.
   Returns a vector of results in the order of `ks`."
  [k ks coll]
  (let [grouped (group-by k coll)]
    (mapv #(get grouped % []) ks)))

(def json-keyword-decode-mapper
  (json/object-mapper {:decode-key-fn true}))

(defn validate-warn
  [schema-fn data context]
  (when (= "true" (:enable-debug config))
    (when-let [result (schema-fn data)]
      (when-not (:valid? result)
        (println (format "[Validation] %s: %s" context (:errors result)))))))
