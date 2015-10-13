(ns codox.example3
  "This is an example namespace for testing ClojureScript support.

  Some more detailed description down here.

  An inline link: http://example.com
  An inline HTTPS link: https://example.com"
  (:require om.core))

(defn foo
  "This is an example function."
  ([x])
  ([x y & z]))

(def baz
  "This is an example var."
  true)

(defn zoo?
  "An example predicate."
  [x])

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

(defprotocol Foop
  "An example protocol."
  (foop [x] "A protocol function belonging to the protocol Foom.")
  (^:deprecated barp [x y] "Another protocol function."))

(defmulti foom
  "An example multimethod."
  {:arglists '([x])}
  :type)

(defn ^:no-doc hidden [x])

(defn markfoo
  "A docstring that selectively uses **markdown**."
  {:doc/format :markdown}
  [x])

(def ^:dynamic *conn*
  "A dynamic var."
  nil)

(def froo
  "A derefable thing created using reify."
  (reify IDeref
    (-deref [_] "froo")))
