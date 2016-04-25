(ns leiningen.codox
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [leiningen.core.project :as project]
            [leiningen.core.main :as main]
            [leinjacker.deps :as deps]
            [leinjacker.eval :as eval]))

(defn- get-options [project]
  (merge {:source-paths (:source-paths project ["src"])
          :output-path  (str (io/file (:target-path project "target") "doc"))}
         (-> project :codox)
         {:name      (str/capitalize (:name project))
          :package   (symbol (:group project) (:name project))
          :root-path (:root project)}
         (select-keys project [:version :description])
         (-> project :codox :project)))

(defn codox
  "Generate API documentation from source code."
  [project]
  (let [project (if (get-in project [:profiles :codox])
                  (project/merge-profiles project [:codox])
                  project)
        options (get-options project)]
    (eval/eval-in-project
     (deps/add-if-missing project '[codox "0.9.5"])
     `(codox.main/generate-docs '~options)
     `(require 'codox.main))
    (main/info "Generated HTML docs in" (:output-path options))))
