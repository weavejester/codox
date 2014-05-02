(ns codox.reader
  "Read raw documentation information from source directories."
  (:require
    [codox.reader.clojure :as clj]
    [codox.reader.clojurescript :as cljs]))

(defn read-namespaces
  "Read namespaces from a source directory (defaults to \"src\"), and
  return a list of maps suitable for documentation purposes.

  The keys in the maps are:
    :name   - the name of the namespace
    :doc    - the doc-string on the namespace
    :author - the author of the namespace
    :publics
      :name     - the name of a public function, macro, or value
      :file     - the file the var was declared in
      :line     - the line at which the var was declared
      :arglists - the arguments the function or macro takes
      :doc      - the doc-string of the var
      :macro    - true if the var is a macro
      :added    - the library version the var was added in"
  [& paths]
  (concat
    (apply clj/read-namespaces paths)
    (apply cljs/read-namespaces paths)))
