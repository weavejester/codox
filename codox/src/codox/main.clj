(ns codox.main
  "Main namespace for generating documentation"
  (:use [codox.utils :only (add-source-paths)])
  (:require [clojure.string :as str]
            [clojure.java.shell :as shell]
            [codox.reader.clojure :as clj]
            [codox.reader.plaintext :as text]))

(defn- writer [{:keys [writer]}]
  (let [writer-sym (or writer 'codox.writer.html/write-docs)
        writer-ns (symbol (namespace writer-sym))]
    (try
      (require writer-ns)
      (catch Exception e
        (throw
         (Exception. (str "Could not load codox writer " writer-ns) e))))
    (if-let [writer (resolve writer-sym)]
      writer
      (throw
         (Exception. (str "Could not resolve codox writer " writer-sym))))))

(defn- macro? [var]
  (= (:type var) :macro))

(defn- read-macro-namespaces [paths read-opts]
  (->> (clj/read-namespaces paths read-opts)
       (map (fn [ns] (update-in ns [:publics] #(filter macro? %))))
       (remove (comp empty? :publics))))

(defn- merge-namespaces [namespaces]
  (for [[name namespaces] (group-by :name namespaces)]
    (assoc (first namespaces) :publics (mapcat :publics namespaces))))

(defn- cljs-read-namespaces [paths read-opts]
  ;; require is here to allow Clojure 1.3 and 1.4 when not using ClojureScript
  (require 'codox.reader.clojurescript)
  (let [reader (find-var 'codox.reader.clojurescript/read-namespaces)]
    (merge-namespaces
     (concat (reader paths read-opts)
             (read-macro-namespaces paths read-opts)))))

(def ^:private namespace-readers
  {:clojure       clj/read-namespaces
   :clojurescript cljs-read-namespaces})

(defn- var-symbol [namespace var]
  (symbol (name (:name namespace)) (name (:name var))))

(defn- remove-matching-vars [vars re namespace]
  (remove (fn [var]
            (when (and re (re-find re (name (:name var))))
              (println "Excluding var" (var-symbol namespace var))
              true))
          vars))

(defn- remove-excluded-vars [namespaces exclude-vars]
  (map #(update-in % [:publics] remove-matching-vars exclude-vars %) namespaces))

(defn- add-var-defaults [vars defaults]
  (for [var vars]
    (-> (merge defaults var)
        (update-in [:members] add-var-defaults defaults))))

(defn- add-ns-defaults [namespaces defaults]
  (for [namespace namespaces]
    (-> (merge defaults namespace)
        (update-in [:publics] add-var-defaults defaults))))

(defn- ns-matches? [{ns-name :name} pattern]
  (cond
    (instance? java.util.regex.Pattern pattern) (re-find pattern (str ns-name))
    (string? pattern) (= pattern (str ns-name))
    (symbol? pattern) (= pattern (symbol ns-name))))

(defn- filter-namespaces [namespaces ns-filters]
  (if (and ns-filters (not= ns-filters :all))
    (filter #(some (partial ns-matches? %) ns-filters) namespaces)
    namespaces))

(defn- read-lang-namespaces
  [{:keys [language root-path source-paths namespaces metadata exclude-vars]}]
  (-> (namespace-readers language)
      (apply source-paths)
      (filter-namespaces namespaces)
      (remove-excluded-vars exclude-vars)
      (add-source-paths root-path source-paths)
      (add-ns-defaults metadata)))

(defn- read-documents [{:keys [doc-paths doc-files] :or {doc-files :all}}]
  (cond
    (not= doc-files :all) (map text/read-file doc-files)
    (seq doc-paths)       (->> doc-paths
                               (apply text/read-documents)
                               (sort-by :name))))

(defn- git-commit [dir]
  (let [{:keys [out exit] :as result} (shell/sh "git" "rev-parse" "HEAD" :dir dir)]
    (when-not (zero? exit)
      (throw (ex-info "Error getting git commit" result)))
    (str/trim out)))

(def defaults
  {:languages    [:clojure]
   :root-path    (System/getProperty "user.dir")
   :output-path  "target/doc"
   :cloverage-path "target/cloverage" ;; kind of ought to get this from the 
                                      ;; cloverage source, so that if they
                                      ;; change their default path we do too;
                                      ;; but it isn't exposed in a usable way.
   :source-paths ["src"]
   :doc-paths    ["doc"]
   :doc-files    :all
   :namespaces   :all
   :exclude-vars #"^(map)?->\p{Upper}"
   :metadata     {}
   :themes       [:default]})

(defn- read-namespaces [options]
  (mapcat #(read-lang-namespaces (assoc options :language %)) (:languages options)))

(defn generate-docs
  "Generate documentation from source files."
  ([]
     (generate-docs {}))
  ([options]
     (let [options    (merge defaults options)
           write-fn   (writer options)
           namespaces (read-namespaces options)
           documents  (read-documents options)]
       (write-fn (assoc options
                        :namespaces namespaces
                        :documents  documents)))))
