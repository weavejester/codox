(ns codox.reader.clojurescript
  "Read raw documentation information from ClojureScript source directory."
  (:use [codox.utils :only [correct-indent]])
  (:require [clojure.java.io :as io]
            [cljs.analyzer :as an]))

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

(defn- read-publics [analysis namespace file]
  (sort-by :name
    (for [[name opts] (get-in analysis [:cljs.analyzer/namespaces namespace :defs])]
      (-> opts
          (select-keys [:line :doc :macro :added :deprecated])
          (update-in [:doc] correct-indent)
          (assoc :file (.getPath file)
                 :arglists (second (:arglists opts))
                 :name name)))))

(defn- read-file [path file]
  (try
    (let [analysis (an/analyze-file (io/file path file))]
      (apply merge
        (for [namespace (keys (:cljs.analyzer/namespaces analysis))
              :let [doc (get-in analysis [:cljs.analyzer/namespaces namespace :doc])]]
          {namespace
           {:name namespace
            :publics (read-publics analysis namespace file)
            :doc (correct-indent doc)}})))
    (catch Exception e
      (println
       (format "Could not generate clojurescript documentation for %s - root cause: %s %s"
               file
               (.getName (class e))
               (.getMessage e))))))

(defn read-namespaces
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
