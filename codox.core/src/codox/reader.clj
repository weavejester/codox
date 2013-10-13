(ns codox.reader
  "Read raw documentation information from Clojure source directory."
  (:use [codox.utils :only (unindent)])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.namespace :as ns])
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

(defn- read-publics [namespace]
  (for [var (sorted-public-vars namespace)
        :when (not (skip-public? var))]
    (-> (meta var)
        (select-keys
         [:name :file :line :arglists :doc :macro :added :deprecated])
        (update-in [:doc] correct-indent))))

(defn- read-ns [namespace]
  (try
    (require namespace)
    (-> (find-ns namespace)
        (meta)
        (assoc :name namespace)
        (assoc :publics (read-publics namespace))
        (update-in [:doc] correct-indent)
        (list))
    (catch Exception e
      (println (format "Could not generate documentation for %s - root cause: %s %s"
                       namespace (.getName (class e)) (.getMessage e))))))

(defn- jar-file? [file]
  (and (.isFile file)
       (-> file .getName (.endsWith ".jar"))))

(defn- find-namespaces [file]
  (cond
   (.isDirectory file) (ns/find-namespaces-in-dir file)
   (jar-file? file)    (ns/find-namespaces-in-jarfile (JarFile. file))))

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
  ([]
     (read-namespaces "src"))
  ([path]
     (->> (io/file path)
          (find-namespaces)
          (mapcat read-ns)))
  ([path & paths]
     (mapcat read-namespaces (cons path paths))))
