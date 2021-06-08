(ns codox-test
  (:require [codox.main :as main]))



(comment

  (main/generate-docs {:source-paths ["test/src"]
                       :output-path "test/doc"
                       :doc-paths ["test/topics"]
                       :namespaces ['testns]
                       :metadata {:doc/format :markdown}
                       :source-uri "https://github.com/cnuernber/codox/blob/master/codox/{filepath}#L{line}"}
                      )

  )
