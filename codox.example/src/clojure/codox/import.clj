(ns codox.import
  (:require [codox.example :as ex]))

(defn foop
  [& args]
  (apply ex/foop args))

(alter-meta! #'foop merge (meta #'ex/foop))
