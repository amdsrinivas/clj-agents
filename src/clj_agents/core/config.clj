(ns clj-agents.core.config
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- parse-env-file
  [path]
  (with-open [rdr (io/reader (io/file path))]
    (->> (line-seq rdr)
         (remove #(re-find #"^\s*(#|$)" %))
         (map #(str/split % #"=" 2))
         (filter #(= 2 (count %)))
         (map (fn [[k v]]
                [(keyword (str/replace (str/lower-case k) "_" "-")) v]))
         (into {}))))

(def config
  (let [file-vars (when (.exists (io/file ".env"))
                    (parse-env-file ".env"))]
    (into {}
          (map (fn [[k v]]
                 [k (or v (System/getenv (str/upper-case (str/replace (name k) "-" "_"))))]))
          file-vars)))
