(ns leiningen.codox
  (:use [leiningen.compile :only (eval-in-project)]))

(defn- get-options [project]
  (-> project
      (select-keys [:name :version :description])
      (assoc :sources (get-in project [:codox :sources]))))

(defn codox
  "Generate API documentation from source code."
  [project]
  (eval-in-project project
    `(codox.main/generate-docs '~(get-options project))
    nil nil
    `(require 'codox.main)))
