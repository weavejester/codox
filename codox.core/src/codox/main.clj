(ns codox.main
  "Main namespace for generating documentation"
  (:use [codox.utils :only (ns-filter)]
        [codox.reader :only (read-namespaces)]))

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

(defn generate-docs
  "Generate documentation from source files."
  ([]
     (generate-docs {}))
  ([{:keys [sources include exclude] :as options}]
     (let [namespaces (-> (apply read-namespaces sources)
                          (ns-filter include exclude))
           write (writer options)]
       (write (assoc options :namespaces namespaces)))))
