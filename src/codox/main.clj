(ns codox.main
  "Main namespace for generating documentation"
  (:use [codox.utils :only (ns-filter)]
        [codox.reader :only (read-namespaces)]
        [codox.writer.html :only (write-docs)]))

(defn generate-docs
  "Generate documentation from source files."
  ([]
     (generate-docs {}))
  ([{:keys [sources include exclude] :as options}]
     (let [namespaces (-> (apply read-namespaces sources)
                          (ns-filter include exclude))]
       (write-docs (assoc options :namespaces namespaces)))))
