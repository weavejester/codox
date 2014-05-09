(ns codox.reader.clojurescript
  "Read raw documentation information from ClojureScript source directory."
  (:use [codox.utils :only [correct-indent]])
  (:require [clojure.java.io :as io]
            [cljs.analyzer :as an]
            [clojure.string :as str]))

(defn- cljs-file? [file]
  (and (.isFile file)
       (-> file .getName (.endsWith ".cljs"))))

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

(defn- var-type [opts]
  (cond
   (:macro opts)           :macro
   (:protocol-symbol opts) :protocol
   :else                   :var))

(defn- read-var [file var]
  (-> var
      (select-keys [:name :line :doc :added :deprecated])
      (update-in [:doc] correct-indent)
      (update-in [:arglists] second)
      (assoc :file (.getPath file)
             :type (var-type var))))

(defn- read-publics [analysis namespace file]
  (->> (get-in analysis [::an/namespaces namespace :defs])
       (map (fn [[name opts]] (assoc opts :name name)))
       (remove :protocol)
       (map (partial read-var file))
       (sort-by (comp str/lower-case :name))))

(defn- read-file [path file]
  (try
    (let [analysis (an/analyze-file (io/file path file))]
      (apply merge
        (for [namespace (keys (::an/namespaces analysis))
              :let [doc (get-in analysis [::an/namespaces namespace :doc])]]
          {namespace
           {:name     namespace
            :publics (read-publics analysis namespace file)
            :doc     (correct-indent doc)}})))
    (catch Exception e
      (println
       (format "Could not generate clojurescript documentation for %s - root cause: %s %s"
               file
               (.getName (class e))
               (.getMessage e))))))

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
