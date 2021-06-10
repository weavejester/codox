(ns ^{:doc "Generate links to Cloverage output in Codox reports."
      :author "Simon Brooke [simon@journeyman.cc]"}
 codox.cloverage-integration
  (:require [clojure.data.zip.xml :as zip-xml]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.xml :as xml]
            [clojure.zip :as zip]))

;; Cloverage emits two things for a namespace:
;; 1. on the page `index.html`, a table row whose fist cell contains the 
;;    name of the namespace, and the remainder of which contains a
;;    graphical representation of test coverage
;; 2. a colour coded HTML rendering of the source, in a file in a
;;    directory heirarchy representing the namespace name, following
;;    the same rules of hyphens/underscores as clojure source.
;;
;;    thus the namespace `cc.journeyman.the-great-game.gossip.news-items`
;;    is in the file 
;;    `cloverage/cc/journeyman/the_great_game/gossip/news_items.clj.html`

;; My first thought on how to do this was to web-scrape from the Cloverage
;; HTML output using Enlive.
;;
;; An alternative strategy would be, rather than running Cloverage as one
;; process and then Codox as another, to make calls from Codoz into 
;; Cloverage code as needed. However, Cloverage isn't really written with
;; that in mind, and this looks really awkward.
;;
;; Cloverage can write a `codecov.json` file, but the format of this isn't
;; easy to use.
;;
;; Finally it can write a report in an XML format called EMMA which is
;; eminently parseable, and this is clearly the easiest way to go.


(def ^:dynamic *cloverage-dir*
  "Where we expect to find Cloverage output. Dynamic so we can rebind it in
   the calling environment once I've worked out what Codox does with options."
  "target/cloverage")

(defn should-emit-cloverage-link
  "True if we should emit cloverage links for this namespace, else false.
   
   TODO: I can't think of any reason we might omit cloverage link for one
   particular namespace, unless perhaps no tests existed for that namespace.
   But I'm not (yet) sure how one would establish that.
   
   TODO: Need to get this from arguments passed in at run time, which end up 
   in `options` passed to `write-fn` in `main.clj`, q.v."
  [namespace-name]
  true)

(defn cloverage-relative-dir
  "Return the relative path from the codox output directory to the cloverage
   output directory, in the current binding environment."
  []
  ;; TODO: make this actually work
  "../cloverage")

(defn parse-emma
  "Parse the EMMA XML file from the cloverage output directory. Memoized, 
   because we're going to reference this file repeatedly but we don't expect
   it to change during the course of a run so there's no point in rereading it.
   
   If `file-path` is passed, parse it from that path; otherwise pass it from
   the default name ('coverage.xml') in the directory indicated by the current
   binging of `*cloverage-dir*`."
  ([]
   (parse-emma (s/join "/" [*cloverage-dir* "coverage.xml"]))
  )
  ([file-path]
   (-> file-path io/file xml/parse zip/xml-zip)))

(defn interpret-stat
  "Interpret a single coverage statistic element `stat` taken from an
   EMMA XML file into something we can use."
  [stat]
  (let [type (zip-xml/attr stat :type)
        name (when (string? type) (keyword (re-find #"^[a-z]+" type)))
        val (zip-xml/attr stat :value)
        matcher (when (string? val) (re-matcher #"[0-9]+" val))]
    [name {:value val
           :percent (when matcher (edn/read-string (re-find matcher)))
           :covered (when matcher (edn/read-string (re-find matcher)))
           :total (when matcher (edn/read-string (re-find matcher)))}]))

(defn summarise-package
  "Summarise a single package element `pkg` from an EMMA XML file"
  [pkg]
  [(zip-xml/attr pkg :name)
   (into {}
         (for [stat (zip-xml/xml-> pkg :coverage)]
           (interpret-stat stat)))])

(def summarise-emma
  "What we want as an output of the parse is a map keyed by namespace name, 
   where the value of the namespace name is a map with the following keys:
   
   1. `:value` the raw value string, primarily as a debugging aid;
   2. `:percentage` the percentage part, as a number;
   3. `:covered` the number of lines in the file covered by unit 
       tests;
   4. `:total` the total number of lines in the file.
   
   NOTE: memoised, since we don't expect this data to change during a run,
   it's expensive to fetch and parse, and we'll need it frequently.
   
   TODO: note that this still does not give us the passing/failing data that
   the HTNL does, so it may be we need to scrape that after all."
  (memoize
   (fn []
     (into
      {}
      (let [emma (parse-emma)]
        (for [pkg (zip-xml/xml-> emma :report :data :all :package)]
          (summarise-package pkg)))))))

;; (binding [*cloverage-dir* "../../the-great-game/docs/cloverage/"]
;;   (summarise-emma))
