(ns codox.main
  "Main namespace for generating documentation"
  (:use [codox.utils :only (ns-filter add-source-paths)])
  (:require [codox.reader.clojure :as clj]
            [codox.reader.clojurescript :as cljs]))

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

(def namespace-readers
  {:clojure       clj/read-namespaces
   :clojurescript cljs/read-namespaces})

(defn generate-docs
  "Generate documentation from source files."
  ([]
     (generate-docs {}))
  ([{:keys [language sources include exclude]
     :or   {language :clojure}
     :as   options}]
     (let [write-fn   (writer options)
           namespaces (-> (namespace-readers language)
                          (apply sources)
                          (ns-filter include exclude)
                          (add-source-paths sources))]
       (write-fn
        (assoc options :namespaces namespaces)))))
