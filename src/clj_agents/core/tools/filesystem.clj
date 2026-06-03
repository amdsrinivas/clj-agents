(ns clj-agents.core.tools.filesystem
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.io File]))

(def text-extensions
  #{"clj" "cljs" "cljc" "edn" "md" "txt" "json" "yml" "yaml"
    "xml" "html" "css" "js" "ts" "jsx" "tsx" "py" "java" "rb" "go"
    "rs" "sh" "sql" "toml" "properties" "csv" "log" "gitignore" "gradle"
    "cfg" "conf" "ini" "lock" "nix" "kt" "scala" "bb"})

(defn- text-file? [^File f]
  (let [name (.getName f)
        dot (str/last-index-of name ".")]
    (when dot
      (let [ext (subs name (inc dot))]
        (contains? text-extensions (str/lower-case ext))))))

(defn- dotfile? [^File f]
  (.startsWith (.getName f) "."))

(def ^:dynamic *workspace-root* ".")

(defn- reject-absolute
  [path]
  (when (.isAbsolute (io/file path))
    "Absolute paths are not allowed. Use a relative path."))

(defn- resolve-path
  [path]
  (.getAbsolutePath (io/file *workspace-root* path)))

(defn- lazy-line-count [^File f]
  (when (and (.isFile f) (text-file? f))
    (with-open [rdr (io/reader f)]
      (count (line-seq rdr)))))

(defn list-directory
  [{:keys [path]}]
  (let [dir-path (or path ".")]
    (if-let [err (reject-absolute dir-path)]
      {:error err}
      (let [dir (io/file (resolve-path dir-path))]
        (if (.exists dir)
          (->> (.listFiles dir)
               (remove #(or (.isHidden %) (dotfile? %)))
               (sort-by #(.getName %))
               (mapv (fn [f]
                       {:name (.getName f)
                        :type (if (.isDirectory f) "dir" "file")
                        :size (when (.isFile f) (.length f))})))
          {:error (format "Directory '%s' not found" dir-path)})))))

(defn read-file
  [{:keys [path start end]}]
  (if-let [err (reject-absolute path)]
    {:error err}
    (let [file (io/file (resolve-path path))]
      (cond
        (not (.exists file))
        {:error (format "File '%s' not found" path)}

        (.isDirectory file)
        {:error (format "'%s' is a directory, not a file" path)}

        (or (.isHidden file) (dotfile? file))
        {:error (format "'%s' is a hidden file and cannot be read" path)}

        (and start end (> (- end start) 500))
        {:error "Requested range exceeds 500 lines. Please narrow your range."}

        :else
        (let [lines (str/split-lines (slurp file))
              total (count lines)
              start' (max 1 (or start 1))
              end' (min total (or end total))]
          (if (< start' end')
            {:filename path
             :lines (subvec (vec lines) (dec start') end')}
            {:error "Invalid range: start must be less than end"}))))))

(defn file-info
  [{:keys [path]}]
  (if-let [err (reject-absolute path)]
    {:error err}
    (let [file (io/file (resolve-path path))]
      (cond
        (not (.exists file))
        {:error (format "File '%s' not found" path)}

        (or (.isHidden file) (dotfile? file))
        {:error (format "'%s' is a hidden file" path)}

        :else
        (let [line-count (lazy-line-count file)
              base {:name (.getName file)
                    :path path
                    :modified (.toString (java.time.Instant/ofEpochMilli (.lastModified file)))
                    :type (if (.isDirectory file) "dir" "file")}]
          (cond-> base
            (.isFile file) (assoc :size (.length file))
            (.isFile file) (assoc :extension
                                  (when-let [dot (str/last-index-of (.getName file) ".")]
                                    (subs (.getName file) (inc dot))))
            line-count (assoc :line-count line-count)))))))
