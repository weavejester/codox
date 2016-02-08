(ns codox.example
  "This is an example namespace for testing.

  Some more detailed description down here.

  An inline link: http://example.com
  An inline HTTPS link: https://example.com"
  {:added "1.1"}
  (:require [clojure.core.typed :as t]))

(defn foo
  "This is an example function for foo."
  ([x])
  ([x y & z]))

(defn bar
  "This is an example function for deprecated bar."
  {:deprecated "2.0"}
  ([x])
  ([x y & z]))

(defn ^:no-doc hidden [x])

(defn markfoo
  "A docstring that uses **markdown**.
  See [[foo]], and also [[typed.array/sum]]."
  {:doc/format :markdown}
  [x])
