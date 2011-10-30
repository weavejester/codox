(ns codox.main
  "Main namespace for generating documentation"
  (:use [codox.reader :only (read-info)]
        [codox.writer.html :only (write-docs)]))

(defn generate-docs
  "Generate documentaiton from source files."
  []
  (write-docs (read-info)))
