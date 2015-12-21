(defproject codox/example "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.3.0"]]
  :plugins [[lein-codox "0.9.1"]]
  :source-paths ["src/clojure"]
  :target-path "target/%s/"
  :codox
  {:project {:name "Example Project", :version "1.0.0"}
   :metadata {:doc "FIXME: write docs"}
   :namespaces :all
   :source-uri
   "https://github.com/weavejester/codox/blob/{version}/codox.example/{filepath}#L{line}"}
  :profiles
  {:md   {:codox {:metadata {:doc/format :markdown}}}
   :cljs {:dependencies [[org.clojure/clojure "1.7.0"]
                         [org.clojure/clojurescript "1.7.189"]]
          :codox {:language :clojurescript}}
   :om   {:dependencies [[org.omcljs/om "0.8.8"]]}
   :no-src {:codox ^:replace {}}
   :no-doc {:codox {:doc-paths ^:replace []}}
   :1.7  {:dependencies [[org.clojure/clojure "1.7.0"]]}})
