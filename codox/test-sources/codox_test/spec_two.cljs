(ns codox-test.spec-two
  (:require [clojure.spec.test.alpha :as st]))

(st/with-instrument-disabled (+ 1 2 3))
