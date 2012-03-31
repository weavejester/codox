(ns leiningen.doc
  (:refer-clojure :exclude [doc]))

(defn- eval-in-project
  "Support eval-in-project in both Leiningen 1.x and 2.x."
  [project form init]
  (let [[eip two?] (or (try (require 'leiningen.core.eval)
                            [(resolve 'leiningen.core.eval/eval-in-project)
                             true]
                            (catch java.io.FileNotFoundException _))
                       (try (require 'leiningen.compile)
                            [(resolve 'leiningen.compile/eval-in-project)]
                            (catch java.io.FileNotFoundException _)))]
    (if two?
      (eip project form init)
      (eip project form nil nil init))))

(defn- get-options [project]
  (-> project
      (select-keys [:name :version :description])
      (merge (get project :codox))))

(defn doc
  "Generate API documentation from source code."
  [project]
  (eval-in-project
   (update-in project [:dependencies] conj ['codox/codox.core "0.6.1"])
   `(codox.main/generate-docs '~(get-options project))
   `(require 'codox.main)))
