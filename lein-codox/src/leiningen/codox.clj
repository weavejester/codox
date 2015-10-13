(ns leiningen.codox
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [leiningen.core.project :as project]
            [leinjacker.deps :as deps]
            [leinjacker.eval :as eval]))

(defn- get-options [project]
  (merge {:sources    (:source-paths project ["src"])
          :output-dir (str (io/file (:target-path project "target") "doc"))}
         (-> project :codox)
         {:name (str/capitalize (:name project))}
         (select-keys project [:root :version :description])
         (-> project :codox :project)))

(defn codox
  "Generate API documentation from source code."
  [project]
  (let [project (if (get-in project [:profiles :codox])
                  (project/merge-profiles project [:codox])
                  project)]
    (eval/eval-in-project
     (deps/add-if-missing project '[codox "0.9.0-SNAPSHOT"])
     `(codox.main/generate-docs
       (update-in '~(get-options project) [:src-uri-mapping] eval))
     `(require 'codox.main))))
