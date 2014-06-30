(ns codox.example2
  {:added "1.1", :deprecated "2.0"})

(defn foo [x y & z])

(defmacro bar [x & body])

(def baz true)

(def ^{:doc "Var with removed source link."} quz 10)

;; Test for issue #43
(alter-meta! #'quz assoc :file "/tmp/form-init1234.clj")

;; Test for issue #57
(defn marklinks
  "Let's try some [links][1].

  [1]: http://example.com"
  {:doc/format :markdown}
  [x])

(def really-long-function-name-with-hyphens)
