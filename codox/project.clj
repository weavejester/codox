(defproject com.cnuernber/codox "1.00-SNAPSHOT"
  :description "Generate documentation from Clojure source files - forked from
weavejester's codox and upgraded to use flexmark-java instead of pegdown."
  :url "https://github.com/cnuernber/codox"
  :scm {:dir ".."}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.3" :scope "provided"]
                 [org.clojure/tools.namespace "1.1.0"]
                 [hiccup "1.0.5"]
                 ;;later version of jsoup used by flexmark
                 [enlive "1.1.6" :exclusions [org.jsoup/jsoup]]
                 [com.vladsch.flexmark/flexmark-all "0.62.2"]]
  :profiles {:dev {:dependencies [[org.clojure/clojurescript "1.10.866"]]}})
