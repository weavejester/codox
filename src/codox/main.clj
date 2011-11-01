(ns codox.main
  "Main namespace for generating documentation"
  (:use [codox.reader :only (read-namespaces)]
        [codox.writer.html :only (write-docs)]))

(defn generate-docs
  "Generate documentation from source files."
  ([]
     (generate-docs {}))
  ([options]
     (write-docs (assoc options :namespaces (read-namespaces)))))
