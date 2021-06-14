(ns ^{:doc "Generate links to Cloverage output in Codox reports."
      :author "Simon Brooke [simon@journeyman.cc]"}
 codox.cloverage-integration.runner
  (:require [cloverage.coverage :refer [run-tests]]
            [clojure.pprint :refer [pprint]]))

(def defaults
  "These are the default values for cloverage arguments we shall use; they can
   be overridden by values in the map which should be the value of `:cloverage`
   in the `:codox` map in the target project's `project.clj`."
  {:text?            false
   :html?            true
   :raw?             false
   :emma-xml?        false
   :junit?           false
   :lcov?            false
   :codecov?         false
   :coveralls?       false
   :summary?         true
   :colorize?        true
   :fail-threshold   0
   :low-watermark    50
   :high-watermark   80
   :debug?           false
   :nop?             false
   :extra-test-ns    []
   :help?            false
   :ns-regex         []
   :test-ns-regex    []
   :ns-exclude-regex []
   :exclude-call     []
   :src-ns-path      []
   :runner           :clojure.test
   :runner-opts      {}
   :test-ns-path     []
   :custom-report    nil})

(defn should-emit-cloverage-link
  "True if we should emit cloverage links for this namespace, else false.
   
   TODO: I can't think of any reason we might omit cloverage link for one
   particular namespace, unless perhaps no tests existed for that namespace.
   But I'm not (yet) sure how one would establish that."
  [namespace-name options]
  (let [opts (merge defaults options)]
  (and true (:html? opts)
       (if (:ns-regex opts)
         (re-find (:ns-regex opts) namespace-name)
         true)
       (if (:ns-exclude-regex opts)
         (not (re-find (:ns-exclude-regex opts) namespace-name))
         true))))

;;(defn format-cloverage-link
;;  [namespace-name options]
;;  (println (str "Printing cloverage output for " namespace-name))
;;  (let [results (run-tests (merge defaults options) [namespace-name])
;;        p (with-out-str (pprint results))]
;;    [:pre p]))

