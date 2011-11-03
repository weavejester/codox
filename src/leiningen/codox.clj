(ns leiningen.codox
  (:use [leiningen.compile :only (eval-in-project)]))

(defn codox
  "Generate documentation"
  [project]
  (eval-in-project project
    `(codox.main/generate-docs
       '~(select-keys project [:name :version :description]))
    nil nil
    `(require 'codox.main)))
