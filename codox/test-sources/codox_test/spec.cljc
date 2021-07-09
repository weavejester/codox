(ns codox-test.spec
  (:require [clojure.spec.test.alpha :as st]))

(st/with-instrument-disabled (+ 1 2 3))
