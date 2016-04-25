(set-env!
  :source-paths #{"src/clojure"}
  :dependencies '[[boot-codox "0.9.5"]])

(require '[codox.boot :refer [codox]])

(deftask docs []
  (comp
    (codox
      :name "Example Project"
      :version "1.0.0"
      :source-uri "https://github.com/weavejester/codox/blob/{version}/codox.example/{filepath}#L{basename}-{line}")
    (target)))
