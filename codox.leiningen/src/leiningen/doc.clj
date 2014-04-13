(ns leiningen.doc
  (:refer-clojure :exclude [doc])
  (:use [leinjacker.eval :only (eval-in-project)])
  (:require [leinjacker.deps :as deps]))

(defn- get-options [project]
  (-> project
      (select-keys [:name :version :description])
      (merge {:sources ["src"]} ;; Default gets overwritten if set in :codox
             (get project :codox))))

(defn doc
  "Generate API documentation from source code."
  [project]
  (eval-in-project
   (deps/add-if-missing project '[ccfontes/codox.core "0.6.7-SNAPSHOT"])
   `(codox.main/generate-docs
     (update-in '~(get-options project) [:src-uri-mapping] eval))
   `(require 'codox.main)))
