(ns testns
  "Some namespace docs!")


(defn testvar
  "Some documentation on the testvar.

```clojure
user> (+ 1 2)
3
```"
  [a]
  (+ a 2))


(defn testvar2
  "Some more documentation.
  See [[testvar]]."
  [a b]
  (+ a b))
