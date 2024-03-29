(defproject codox "0.10.8"
  :description "Generate documentation from Clojure source files"
  :url "https://github.com/weavejester/codox"
  :scm {:dir ".."}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [org.clojure/clojurescript "1.7.189"]
                 [hiccup "1.0.5"]
                 [enlive "1.1.6"]
                 [com.vladsch.flexmark/flexmark "0.62.2"]
                 [com.vladsch.flexmark/flexmark-profile-pegdown "0.62.2"]
                 [com.vladsch.flexmark/flexmark-util-misc"0.62.2"]
                 [org.ow2.asm/asm-all "5.0.3"]])
