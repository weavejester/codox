(ns codox.reader
  "Read raw documentation information from Clojure source directory."
  (:use [codox.utils :only (unindent)])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.namespace :as ns]
            [cljs.analyzer :as an])
  (:import java.util.jar.JarFile))

(defn- correct-indent [text]
  (if text
    (let [lines (str/split-lines text)]
      (->> (rest lines)
           (str/join "\n")
           (unindent)
           (str (first lines) "\n")))))

(defn- sorted-public-vars [namespace]
  (->> (ns-publics namespace)
       (vals)
       (sort-by (comp :name meta))))

(defn- skip-public? [var]
  (let [{:keys [skip-wiki no-doc]} (meta var)]
    (or skip-wiki no-doc)))

(defn- read-clj-publics [namespace]
  (for [var (sorted-public-vars namespace)
        :when (not (skip-public? var))]
    (-> (meta var)
        (select-keys
         [:name :file :line :arglists :doc :macro :added :deprecated])
        (update-in [:doc] correct-indent))))

(defn- read-clj-ns [namespace]
  (try
    (require namespace)
    (-> (find-ns namespace)
        (meta)
        (assoc :name namespace)
        (assoc :publics (read-clj-publics namespace))
        (update-in [:doc] correct-indent)
        (list))
    (catch Exception e
      (println (format "Could not generate clojure documentation for %s - root cause: %s %s"
                       namespace
                       (.getName (class e))
                       (.getMessage e))))))

(defn- jar-file? [file]
  (and (.isFile file)
       (-> file .getName (.endsWith ".jar"))))

(defn- find-clj-namespaces [file]
  (cond
   (.isDirectory file) (ns/find-namespaces-in-dir file)
   (jar-file? file)    (ns/find-namespaces-in-jarfile (JarFile. file))))

(defn read-clj-namespaces
  ([]
     (read-clj-namespaces "src"))
  ([path]
     (->> (io/file path)
          (find-clj-namespaces)
          (mapcat read-clj-ns)))
  ([path & paths]
     (mapcat read-clj-namespaces (cons path paths))))

(defn- cljs-file? [file]
  (and (.isFile file)
       (-> file .getName (.endsWith ".cljs"))))

(defn strip-parent [parent]
  (let [len (inc (count (.getPath parent)))]
    (fn [child]
      (let [child-name (.getPath child)]
        (when (>= (count child-name) len)
          (io/file (subs child-name len)))))))

(defn find-cljs-files [file]
  (when (.isDirectory file)
    (keep
      (strip-parent file)
      (filter
        cljs-file?
        (file-seq file)))))

(defn read-cljs-publics [analysis namespace file]
  (sort-by
   :name
    (for [[name opts] (get-in analysis [:cljs.analyzer/namespaces namespace :defs])]
      (->
        opts
        (select-keys [:line :doc :macro :added :deprecated])
        (update-in [:doc] correct-indent)
        (assoc :file (.getPath file)
               :arglists (second (:arglists opts)) ; dont know why cljs.analyzer double quotes this..
               :name name)))))

(defn read-cljs-file [path file]
  (try
    (let [analysis (an/analyze-file (io/file path file))]
      (apply merge
        (for [namespace (keys (:cljs.analyzer/namespaces analysis))
              :let [doc (get-in analysis [:cljs.analyzer/namespaces namespace :doc])]]
          {namespace
            {:name namespace
             :publics (read-cljs-publics analysis namespace file)
             :doc (correct-indent doc)}})))
    (catch Exception e
      (println
        (format
          "Could not generate clojurescript documentation for %s - root cause: %s %s"
          file
          (.getName (class e))
          (.getMessage e))))))

(defn read-cljs-namespaces
  ([]
     (read-cljs-namespaces "src"))
  ([path]
     (->>
       (find-cljs-files (io/file path))
       (map (partial read-cljs-file (io/file path)))
       (apply merge)
       (vals)
       (sort-by :name)))
  ([path & paths]
     (->>
       (cons path paths)
       (mapcat read-cljs-namespaces))))

(defn read-namespaces
  "Read namespaces from a source directory (defaults to \"src\"), and
  return a list of maps suitable for documentation purposes.

  The keys in the maps are:
    :name   - the name of the namespace
    :doc    - the doc-string on the namespace
    :author - the author of the namespace
    :publics
      :name     - the name of a public function, macro, or value
      :file     - the file the var was declared in
      :line     - the line at which the var was declared
      :arglists - the arguments the function or macro takes
      :doc      - the doc-string of the var
      :macro    - true if the var is a macro
      :added    - the library version the var was added in"
  [& paths]
  (concat
    (apply read-clj-namespaces paths)
    (apply read-cljs-namespaces paths)))
