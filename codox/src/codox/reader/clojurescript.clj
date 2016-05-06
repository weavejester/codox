(ns codox.reader.clojurescript
  "Read raw documentation information from ClojureScript source directory."
  (:use [codox.utils :only [assoc-some update-some correct-indent]])
  (:require [clojure.java.io :as io]
            [cljs.analyzer :as an]
            [cljs.analyzer.api :as ana]
            [clojure.string :as str]))

(defn- cljs-file? [file]
  (and (.isFile file)
       (-> file .getName (.endsWith ".cljs"))))

(defn- remove-quote [x]
  (if (and (list? x) (= (first x) 'quote))
    (second x)
    x))

(defn- strip-parent [parent]
  (let [len (inc (count (.getPath parent)))]
    (fn [child]
      (let [child-name (.getPath child)]
        (if (>= (count child-name) len)
          (io/file (subs child-name len)))))))

(defn- find-files [file]
  (if (.isDirectory file)
    (->> (file-seq file)
         (filter cljs-file?)
         (keep (strip-parent file)))))

(defn- no-doc? [var]
  (or (:skip-wiki var) (:no-doc var)))

(defn- protocol-methods [protocol vars]
  (let [proto-name (name (:name protocol))]
    (filter #(if-let [p (:protocol %)] (= proto-name (name p))) vars)))

(defn- var-type [opts]
  (cond
   (:macro opts)           :macro
   (:protocol-symbol opts) :protocol
   :else                   :var))

(defn- read-var [file vars var]
  (-> var
      (select-keys [:name :line :arglists :doc :dynamic :added :deprecated :doc/format])
      (update-some :name (comp symbol name))
      (update-some :arglists remove-quote)
      (update-some :doc correct-indent)
      (assoc-some  :file    (.getPath file)
                   :type    (var-type var)
                   :members (map (partial read-var file vars)
                                 (protocol-methods var vars)))))

(defn- read-publics [state namespace file]
  (let [vars (vals (ana/ns-publics state namespace))]
    (->> vars
         (remove :protocol)
         (remove :anonymous)
         (remove no-doc?)
         (map (partial read-var file vars))
         (sort-by (comp str/lower-case :name)))))

(defn- analyze-file [file]
  (let [state (ana/empty-state)]
    (binding [an/*analyze-deps* false]
      (ana/no-warn
        (ana/analyze-file state file {})))
    state))

(defn- read-file [path file]
  (try
    (let [source  (io/file path file)
          ns-name (:ns (ana/parse-ns source))
          state   (analyze-file source)]
      {ns-name
       (-> (ana/find-ns state ns-name)
           (select-keys [:name :doc])
           (update-some :doc correct-indent)
           (assoc :publics (read-publics state ns-name file)))})
    (catch Exception e
      (println
       (format "Could not generate clojurescript documentation for %s - root cause: %s %s"
               file
               (.getName (class e))
               (.getMessage e)))
      (.printStackTrace e))))

(defn read-namespaces
  "Read ClojureScript namespaces from a source directory (defaults to
  \"src\"), and return a list of maps suitable for documentation
  purposes.

  The keys in the maps are:
    :name   - the name of the namespace
    :doc    - the doc-string on the namespace
    :author - the author of the namespace
    :publics
      :name       - the name of a public function, macro, or value
      :file       - the file the var was declared in
      :line       - the line at which the var was declared
      :arglists   - the arguments the function or macro takes
      :doc        - the doc-string of the var
      :type       - one of :macro, :protocol or :var
      :added      - the library version the var was added in
      :deprecated - the library version the var was deprecated in"
  ([] (read-namespaces "src"))
  ([path]
     (let [path (io/file path)
           file-reader (partial read-file path)]
       (->> (find-files path)
            (map file-reader)
            (apply merge)
            (vals)
            (sort-by :name))))
  ([path & paths]
     (mapcat read-namespaces (cons path paths))))
