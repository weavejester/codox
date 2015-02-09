(defproject codox/codox.core "0.8.10"
  :description "Generate documentation from Clojure source files"
  :url "https://github.com/weavejester/codox"
  :scm {:dir ".."}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.namespace "0.2.4"]
                 [org.clojure/clojurescript "0.0-2913"]
                 [hiccup "1.0.5"]
                 [org.pegdown/pegdown "1.4.2"]])
