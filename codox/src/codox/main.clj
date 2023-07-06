(ns codox.main
  "Main namespace for generating documentation"
  (:use [codox.utils :only (add-source-paths)])
  (:require [clojure.string :as str]
            [clojure.java.shell :as shell]
            [codox.reader.clojure :as clj]
            [codox.reader.clojurescript :as cljs]
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

(def ^:private namespace-readers
  {:clojure       clj/read-namespaces
   :clojurescript cljs/read-namespaces})

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

(defn- read-namespaces
  "Returns {<language> <namespace-seq>} for cross-platform opts,
  or <namespace-seq> otherwise."
  [{:keys [language root-path source-paths namespaces metadata exclude-vars] :as opts}]
  (if (:cross-platform? opts)
    (reduce
      (fn [m language]
        (assoc m language
          (read-namespaces
            (assoc opts
              :language language
              :cross-platform? false))))
      {}
      (:languages opts))
    (let [reader (namespace-readers language)]
      (-> (reader source-paths (select-keys opts [:exception-handler]))
          (filter-namespaces namespaces)
          (remove-excluded-vars exclude-vars)
          (add-source-paths root-path source-paths)
          (add-ns-defaults metadata)))))

(defn- get-var-langs
  "Returns {<ns> {<var> <set-of-languages>}} for given namespaces."
  ([language namespaces var-langs]
   (reduce
     (fn [var-langs ns]
       (reduce
         (fn [var-langs public-var]
           (update-in var-langs [(:name ns) (:name public-var)]
             #(conj (or % #{}) language)))
         var-langs
         (:publics ns)))
     var-langs
     namespaces))

  ([options namespaces]
   (if-not (:cross-platform? options)
     (get-var-langs (:language options) namespaces {})
     (reduce
       (fn [var-langs language]
         (get-var-langs language (get namespaces language) var-langs))
       {}
       (:languages options)))))

(comment (get-var-langs {:languages #{:clojure :clojurescript}}
           '({:name codox.main :publics ({:name defaults} {:name bar})}
             {:name codox.foo  :publics ({:name bar})})))

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
  (let [root-path (System/getProperty "user.dir")]
    {:language     :clojure
     :root-path    root-path
     :output-path  "target/doc"
     :source-paths ["src"]
     :doc-paths    ["doc"]
     :doc-files    :all
     :namespaces   :all
     :exclude-vars #"^(map)?->\p{Upper}"
     :metadata     {}
     :themes       [:default]
     :git-commit   (delay (git-commit root-path))}))

(defn- cross-platform-options [{:keys [language] :as opts}]
  (if-not (set? language)
    opts ; {:language <keyword>}
    (if (= (count language) 1)
      (assoc opts :language (first language)) ; {:language <keyword>}

      ;; Cross-platform case: {:language nil, :languages <set>}
      (assoc opts
        :language        nil
        :languages       language
        :cross-platform? true))))

(defn generate-docs
  "Generate documentation from source files."
  ([]
     (generate-docs {}))
  ([options]
     (let [options    (-> (merge defaults options) cross-platform-options)
           write-fn   (writer options)
           namespaces (read-namespaces options)
           documents  (read-documents options)
           var-langs  (get-var-langs options namespaces)]
       (write-fn (assoc options
                        :namespaces namespaces
                        :documents  documents
                        :var-langs  var-langs)))))
