(defproject codox/example "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :plugins [[lein-codox "0.10.2"]]
  :source-paths ["src/clojure"]
  :target-path "target/%s/"
  :codox
  {:project {:name "Example Project", :version "1.0.0"}
   :metadata {:doc "FIXME: write docs"}
   :namespaces :all
   :doc-files ["doc/intro.md"
               "doc/formatting.md"]
   :source-uri
   "https://github.com/weavejester/codox/blob/{version}/codox.example/{filepath}#L{basename}-{line}"
   :html
   {:transforms [[:head] [:prepend [:script "console.log('hello');"]]
                 [:head] [:append  [:script "console.log('world');"]]
                 [:head :title] [:substitute [:title "Made up title"]]
                 [:pre.deps] [:before [:p "Before test"]]
                 [:pre.deps] [:after  [:p "After test"]]]}}
  :profiles
  {:md     {:codox {:metadata {:doc/format :markdown}}}
   :theme  {:codox {:themes [[:test {:test-message "Test! Test! "}]]}}
   :no-exclude {:codox {:exclude-vars nil}}
   :cljs   {:dependencies [[org.clojure/clojure "1.7.0"]
                           [org.clojure/clojurescript "1.7.189"]]
            :codox {:language :clojurescript}}
   :om     {:dependencies [[org.omcljs/om "0.8.8"]]}
   :typed  {:dependencies [[org.clojure/clojure "1.7.0"]
                           [org.clojure/core.typed "0.3.22"]]
            :plugins [[lein-typed "0.3.5"]]
            :source-paths ^:replace ["src-typed/clojure"]}
   :no-src {:codox ^:replace {}}
   :no-doc {:codox {:doc-paths ^:replace []}}
   :1.7    {:dependencies [[org.clojure/clojure "1.7.0"]]}})
