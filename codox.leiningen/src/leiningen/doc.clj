(ns leiningen.doc
  (:refer-clojure :exclude [doc])
  (:use [leinjacker.eval :only (eval-in-project)])
  (:require [leinjacker.deps :as deps]))

(defn- get-options [project]
  (-> project
      (select-keys [:name :version :description])
      (merge {:sources (:source-paths project ["src"])}
             (:codox project))))

(defn doc
  "Generate API documentation from source code."
  [project]
  (eval-in-project
   (deps/add-if-missing project '[codox/codox.core "0.7.0"])
   `(codox.main/generate-docs '~(get-options project))
   `(require 'codox.main)))
