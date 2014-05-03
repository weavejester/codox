(ns codox.reader.clojure
  "Read raw documentation information from Clojure source directory."
  (:import java.util.jar.JarFile)
  (:use [codox.utils :only (correct-indent)])
  (:require [clojure.java.io :as io]
            [clojure.tools.namespace :as ns]))

(defn- sorted-public-vars [namespace]
  (->> (ns-publics namespace)
       (vals)
       (sort-by (comp :name meta))))

(defn- no-doc? [var]
  (let [{:keys [skip-wiki no-doc]} (meta var)]
    (or skip-wiki no-doc)))

(defn- proxy? [var]
  (re-find #"proxy\$" (-> var meta :name str)))

(defn- read-publics [namespace]
  (for [var (sorted-public-vars namespace)
        :when (not (or (proxy? var) (no-doc? var)))]
    (-> (meta var)
        (select-keys [:name :file :line :arglists :doc :macro :added :deprecated])
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
      (println
       (format "Could not generate clojure documentation for %s - root cause: %s %s"
               namespace
               (.getName (class e))
               (.getMessage e))))))

(defn- jar-file? [file]
  (and (.isFile file)
       (-> file .getName (.endsWith ".jar"))))

(defn- find-namespaces [file]
  (cond
    (.isDirectory file) (ns/find-namespaces-in-dir file)
    (jar-file? file)    (ns/find-namespaces-in-jarfile (JarFile. file))))

(defn read-namespaces
  ([] (read-namespaces "src"))
  ([path]
     (->> (io/file path)
          (find-namespaces)
          (mapcat read-ns)))
  ([path & paths]
     (mapcat read-namespaces (cons path paths))))
