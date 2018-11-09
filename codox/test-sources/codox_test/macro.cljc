(ns codox-test.macro)

(defmacro test
  [a b]
  `(+ ~a ~b))

(defmacro test2
  [a & xs]
  `(reduce + ~a ~(vec xs)))

;; https://github.com/jarohen/nomad/blob/5520c332c5c6d5eef4dcb0930c735900d63dea2a/src/nomad/config.clj#L98-L111

(defn with-config-override* [{:keys [switches secret-keys override-switches] :as opts-override} f]
  nil)

(doto (defmacro with-config-override [opts & body]
        `(with-config-override* ~opts (fn [] ~@body)))
  (alter-meta! assoc :arglists '([{:keys [switches secret-keys override-switches] :as opts-override} & body])))
