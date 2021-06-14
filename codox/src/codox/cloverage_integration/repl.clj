(ns ^{:doc "tools to help me debug this in the repl."
      :author "Simon Brooke [simon@journeyman.cc]"}
 codox.cloverage-integration.repl
  (:require [clojure.string :as s]))

(def documented-project-dir "/home/simon/workspace/the-great-game")

(def project
  "This is essentially the `:codox` entry from the-great-game's `:project.clj`,
   modified for the fact that we're not running from inside the-great-game's
   project directory."
  {:metadata {:doc "**TODO**: write docs"
              :doc/format :markdown}
   :cloverage {:output "docs/cloverage"
               :codecov? true
               :html? true
               :src-ns-path [(s/join "/" [documented-project-dir "/src"])]
               :test-ns-path [(s/join "/" [documented-project-dir "/test"])]}
   :output-path "docs/codox"
   :source-uri "https://github.com/simon-brooke/the-great-game/blob/master/{filepath}#L{line}"})