(defproject codox.example "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.3.0"]]
  :plugins [[codox "0.7.4"]]
  :source-paths ["src/clojure"]
  :codox
  {:src-dir-uri "http://github.com/weavejester/codox/blob/master/"
   :src-linenum-anchor-prefix "L"
   :src-uri-mapping {#"src/clojure" #(str "codox.example/src/clojure/" %)}}
  :profiles
  {:cljs {:dependencies [[org.clojure/clojure "1.5.1"]]
          :codox {:language :clojurescript}}})
