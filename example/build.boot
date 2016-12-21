(set-env!
  :source-paths #{"src/clojure"}
  :resource-paths #{"resources"}
  :dependencies '[[boot-codox "0.10.2"]])

(require '[codox.boot :refer [codox]])

(deftask docs []
  (comp
    (codox
      :name "Example Project"
      :description "FIXME: write description"
      :version "1.0.0"
      :source-uri "https://github.com/weavejester/codox/blob/{version}/codox.example/{filepath}#L{basename}-{line}"
      :themes [[:test {:test-message "Test! Test! "}]])
    (target)))
