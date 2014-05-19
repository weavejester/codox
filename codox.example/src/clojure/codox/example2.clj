(ns codox.example2)

(defn foo [x y & z])

(defmacro bar [x & body])

(def baz true)

(def ^{:doc "Var with removed source link."} quz 10)

;; Test for issue #43
(alter-meta! #'quz assoc :file "/tmp/form-init1234.clj")
