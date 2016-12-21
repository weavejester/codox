(ns codox.boot
  {:boot/export-tasks true}
  (:require [clojure.java.io :as io]
            [boot.core :as core :refer [deftask]]
            [boot.pod :as pod]
            [boot.util :as util]))

(defn- pod-deps []
  (remove pod/dependency-loaded? '[[codox "0.10.2"]]))

(defn- init [fresh-pod]
  (pod/require-in fresh-pod '[codox.main]))

(deftask codox
  "Generate documentation in a pod."
  [n  name                STRING str    "The project name"
   e  description         STRING str    "The project description"
   v  version             STRING str    "The project version"
   o  output-path         PATH   str    "The directory where the documentation will be generated"
   s  source-paths        PATHS  #{str} "Source paths from which to create documentation (defaults to :source-paths in Boot env)"
   u  source-uri          URI    str    "Source URI template string"
   d  doc-paths           PATHS  #{str} "Path to documentation files"
   l  language            LANG   kw     "Library language. (defaults to :clojure)"
   f  filter-namespaces   NS     #{sym} "Namespace restriction for documentation generation (defaults to all namespaces)"
   m  metadata            META   edn    "Metadata settings in edn format"
   w  writer              WRITER sym    "Custom output writer"
   t  themes              THEMES edn    "Custom CSS/JS themes"]
  (when-not name
    (util/fail "No codox project name specified\n")
    (System/exit 1))

  (let [output-path (or output-path "doc")
        source-paths (or source-paths (:source-paths (core/get-env)))
        updated-env (update (core/get-env) :dependencies into (pod-deps))
        pods (pod/pod-pool updated-env :init init)]
    (core/cleanup (pods :shutdown))
    (core/with-pre-wrap fileset
      (let [worker-pod (pods :refresh)
            tmp-dir (core/tmp-dir!)
            docs-dir (io/file tmp-dir output-path)]

        (pod/with-eval-in worker-pod
          (->> {:name         ~name
                :version      ~version
                :description  ~description
                :source-paths ~source-paths
                :output-path  ~(.getPath docs-dir)
                :source-uri   ~source-uri
                :doc-paths    ~doc-paths
                :language     ~language
                :namespaces   (quote ~filter-namespaces)
                :metadata     ~metadata
                :writer       (quote ~writer)
                :themes       ~themes}
            (remove (comp nil? second))
            (into {})
            (codox.main/generate-docs)))

        (util/info (str "Generated HTML docs in " output-path "\n"))

        (-> fileset
          (core/add-asset tmp-dir)
          (core/commit!))))))
