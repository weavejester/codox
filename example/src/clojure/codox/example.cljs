(ns codox.example
  "ClojureScript version."
  {:added "1.1", :deprecated "2.0"})

(defn foo
  "This is a function in Clojure and ClojureScript."
  [x y & z])

(defn quzquz
  "This is a ClojureScript-only function."
  [x])

(defrecord CljsRecord [aah choo])
