(ns codox.example-spec-link
  "Example with clojure.spec keyword links across namespaces."
  (:require [codox.example-spec :as ex]
            [clojure.spec :as s]))

(s/def ::local-kw (s/nilable (s/int-in 2 5)))

(s/def ::some-links (s/keys :req [::local-kw
                                  ::ex/kw-with-ns-alias
                                  :codox.example2/kw-without-ns-alias]))
