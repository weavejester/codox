(ns codox.main
  "Main namespace for generating documentation"
  (:use [codox.utils :only (ns-filter add-source-paths)])
  (:require [codox.reader.clojure :as clj]))

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

(defn- read-macro-namespaces [& paths]
  (->> (apply clj/read-namespaces paths)
       (map (fn [ns] (update-in ns [:publics] #(filter macro? %))))
       (remove (comp empty? :publics))))

(defn- merge-namespaces [namespaces]
  (for [[name namespaces] (group-by :name namespaces)]
    (assoc (first namespaces) :publics (mapcat :publics namespaces))))

(defn- cljs-read-namespaces [& paths]
  (require 'codox.reader.clojurescript)
  (let [reader (find-var 'codox.reader.clojurescript/read-namespaces)]
    (merge-namespaces
     (concat (apply reader paths)
             (apply read-macro-namespaces paths)))))

(def namespace-readers
  {:clojure       clj/read-namespaces
   :clojurescript cljs-read-namespaces})

(def init-dir (System/getProperty "user.dir"))

(defn generate-docs
  "Generate documentation from source files."
  ([]
     (generate-docs {}))
  ([{:keys [language root sources include exclude]
     :or   {language :clojure, sources ["src"], root init-dir}
     :as   options}]
     (let [write-fn   (writer options)
           namespaces (-> (namespace-readers language)
                          (apply sources)
                          (ns-filter include exclude)
                          (add-source-paths root sources))]
       (write-fn
        (assoc options :namespaces namespaces)))))
