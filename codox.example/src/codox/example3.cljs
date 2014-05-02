(ns codox.example3
  "This is an example namespace for testing ClojureScript support.

  Some more detailed description down here.")

(defn foo
  "This is an example function."
  [x y & z])

(def baz
  "This is an example var."
  true)

(defn quz
  "Another example function."
  {:added "1.1"}
  [x])

(defn foobar
  "An obsolete function."
  {:deprecated true}
  [x])

(defn foobaz
  "An obsolete function with a specific version."
  {:deprecated "1.1"}
  [x])

(defn quzbar
  "A function with a lifespan."
  {:added "1.0" :deprecated "1.1"}
  [x])
