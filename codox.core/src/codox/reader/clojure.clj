(ns codox.reader.clojure
  "Read raw documentation information from Clojure source directory."
  (:import java.util.jar.JarFile)
  (:use [codox.utils :only (correct-indent)])
  (:require [clojure.java.io :as io]
            [clojure.tools.namespace :as ns]
            [clojure.string :as str]))

(defn- sorted-public-vars [namespace]
  (->> (ns-publics namespace)
       (vals)
       (sort-by (comp :name meta))))

(defn- no-doc? [var]
  (let [{:keys [skip-wiki no-doc]} (meta var)]
    (or skip-wiki no-doc)))

(defn- proxy? [var]
  (re-find #"proxy\$" (-> var meta :name str)))

(defn- macro? [var]
  (:macro (meta var)))

(defn- multimethod? [var]
  (instance? clojure.lang.MultiFn (var-get var)))

(defn- protocol? [var]
  (let [value (var-get var)]
    (and (map? value) (:on-interface value))))

(defn- protocol-method? [var]
  (:protocol (meta var)))

(defn- protocol-methods [protocol vars]
  (filter #(= protocol (:protocol (meta %))) vars))

(defn- var-type [var]
  (cond
   (macro? var)       :macro
   (multimethod? var) :multimethod
   (protocol? var)    :protocol
   :else              :var))

(defn- read-var [vars var]
  (-> (meta var)
      (select-keys [:name :file :line :arglists :doc :dynamic
                    :added :deprecated :doc/format])
      (update-in [:doc] correct-indent)
      (assoc :type (var-type var))
      (assoc :members (map (partial read-var vars)
                           (protocol-methods var vars)))))

(defn- read-publics [namespace]
  (let [vars (sorted-public-vars namespace)]
    (->> vars
         (remove proxy?)
         (remove no-doc?)
         (remove protocol-method?)
         (map (partial read-var vars))
         (sort-by (comp str/lower-case :name)))))

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
  "Read Clojure namespaces from a source directory (defaults to
  \"src\"), and return a list of maps suitable for documentation
  purposes.

  Any namespace with {:no-doc true} in its metadata will be skipped.

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
      :type       - one of :macro, :protocol, :multimethod or :var
      :added      - the library version the var was added in
      :deprecated - the library version the var was deprecated in"
  ([] (read-namespaces "src"))
  ([path]
     (->> (io/file path)
          (find-namespaces)
          (mapcat read-ns)
          (remove :no-doc)))
  ([path & paths]
     (mapcat read-namespaces (cons path paths))))
