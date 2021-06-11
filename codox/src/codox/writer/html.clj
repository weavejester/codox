(ns codox.writer.html
  "Documentation writer that outputs HTML."
  (:use [hiccup core page element])
  (:import [java.net URLEncoder]
           [java.io File]
           ;;Otherwise known has a hashmap...
           [com.vladsch.flexmark.util.data MutableDataSet]
           [com.vladsch.flexmark.parser Parser]
           [com.vladsch.flexmark.html HtmlRenderer HtmlRenderer$HtmlRendererExtension]
           [com.vladsch.flexmark.ext.gfm.strikethrough StrikethroughExtension]
           [com.vladsch.flexmark.ext.gfm.tasklist TaskListExtension]
           [com.vladsch.flexmark.ext.autolink AutolinkExtension]
           [com.vladsch.flexmark.ext.tables TablesExtension]
           [com.vladsch.flexmark.ext.wikilink WikiLinkExtension]
           [com.vladsch.flexmark.html LinkResolverFactory LinkResolver]
           [com.vladsch.flexmark.html.renderer LinkResolverBasicContext LinkType
            LinkStatus]
           [codox LinkResolverFactoryImpl])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [net.cgrand.enlive-html :as enlive-html]
            [net.cgrand.jsoup :as jsoup]
            [codox.utils :as util]))

(set! *warn-on-reflection* true)

(def enlive-operations
  {:append     enlive-html/append
   :prepend    enlive-html/prepend
   :after      enlive-html/after
   :before     enlive-html/before
   :substitute enlive-html/substitute})

(defn- enlive-transformer [[op & args]]
  (apply (enlive-operations op) (map enlive-html/html args)))

(defn- enlive-transform [nodes transforms]
  (reduce
   (fn [out [s t]]
     (enlive-html/transform out s (enlive-transformer t)))
   nodes
   (partition 2 transforms)))

(defn- enlive-emit [nodes]
  (apply str (enlive-html/emit* nodes)))

(defn- enlive-parse [s]
  (let [stream (io/input-stream (.getBytes (str s) "UTF-8"))]
    (enlive-html/html-resource stream {:parser jsoup/parser})))

(defn- transform-html [project s]
  (-> (enlive-parse s)
      (enlive-transform (-> project :html :transforms))
      (enlive-emit)))

(defn- var-id [var]
  (str "var-" (-> var name URLEncoder/encode (str/replace "%" "."))))

(def ^:private url-regex
  #"((https?|ftp|file)://[-A-Za-z0-9+()&@#/%?=~_|!:,.;]+[-A-Za-z0-9+()&@#/%=~_|])")

(defn- add-anchors [text]
  (if text
    (str/replace text url-regex "<a href=\"$1\">$1</a>")))

(defmulti format-docstring
  "Format the docstring of a var or namespace into HTML."
  (fn [project ns var] (:doc/format var))
  :default :plaintext)

(defmethod format-docstring :plaintext [_ _ metadata]
  [:pre.plaintext (add-anchors (h (:doc metadata)))])

(declare link-resolver-factory)

(defn flexmark-options
  ^MutableDataSet [project]
  (doto (MutableDataSet.)
    (.set Parser/EXTENSIONS
          [(TablesExtension/create)
           (StrikethroughExtension/create)
           ;;Our custom link resolver has to be first else the extensions below
           ;;get first crack at the links and we never see them.  We can always
           ;;do nothing with the link and that will in effect forward the link
           ;;further down the line.
           (reify HtmlRenderer$HtmlRendererExtension
             (rendererOptions [this options])
             (extend [this builder renderer-type]
               (.linkResolverFactory builder ^LinkResolverFactory (link-resolver-factory project))))
           (AutolinkExtension/create)
           (WikiLinkExtension/create)])))

(defn parser
  [project]
  (.build (Parser/builder (flexmark-options project))))

(defn renderer
  ^HtmlRenderer [project]
  (.build (HtmlRenderer/builder (flexmark-options project))))

(defn- find-wikilink [project ns text]
  (let [ns-strs (map (comp str :name) (:namespaces project))]
    (if (contains? (set ns-strs) text)
      (str text ".html")
      (if-let [var (util/search-vars (:namespaces project) text (:name ns))]
        (str (namespace var) ".html#" (var-id var))))))


(defn- parse-wikilink [text]
  (let [pos (.indexOf (str text) "|")]
    (if (>= pos 0)
      [(subs text 0 pos) (subs text (inc pos))]
      [text text])))

(defn- absolute-url? [url]
  (re-find #"^([a-z]+:)?//" url))

(defn- fix-markdown-url [url]
  (if-not (absolute-url? url)
    (str/replace url #"\.(md|markdown)$" ".html")
    url))

(defn link-resolver-factory
  ^LinkResolverFactory [project]
  (LinkResolverFactoryImpl.
   nil nil false
   (fn [^LinkResolverBasicContext outer-context]
     (reify
       LinkResolver
       (resolveLink [this node context link]
         (if (= "WIKI" (.getName (.getLinkType link)))
           (if-let [url (find-wikilink project nil (.getUrl link))]
             (-> link
                 (.withUrl url)
                 (.withStatus LinkStatus/VALID))
             link)
           (if-let [url (fix-markdown-url (.getUrl link))]
             (-> link
                 (.withUrl url)
                 (.withStatus LinkStatus/VALID))
             link)))))))


(defn markdown->html
  ^String [markdown-str project]
  (when (and markdown-str (not= (count markdown-str) 0))
    (->> (.parse ^Parser (:parser project) (str markdown-str))
         (.render ^HtmlRenderer (:renderer project)))))

(defmethod format-docstring :markdown [project ns metadata]
  [:div.markdown (markdown->html (:doc metadata) project)])

(defn- ns-filename [namespace]
  (str (:name namespace) ".html"))

(defn- ns-filepath [output-dir namespace]
  (str output-dir "/" (ns-filename namespace)))

(defn- doc-filename [doc]
  (str (:name doc) ".html"))

(defn- doc-filepath [output-dir doc]
  (str output-dir "/" (doc-filename doc)))

(defn- var-uri [namespace var]
  (str (ns-filename namespace) "#" (var-id (:name var))))

(defn- get-source-uri [source-uris path]
  (some (fn [[re f]] (if (re-find re path) f)) source-uris))

(defn- uri-basename [path]
  (second (re-find #"/([^/]+?)$" path)))

(defn- force-replace [^String s match replacement]
  (if (.contains s match)
    (str/replace s match (force replacement))
    s))

(defn- var-source-uri
  [{:keys [source-uri version git-commit]}
   {:keys [path file line]}]
  (let [path (util/uri-path path)
        uri  (if (map? source-uri) (get-source-uri source-uri path) source-uri)]
    (-> uri
        (str/replace   "{filepath}"   path)
        (str/replace   "{classpath}"  (util/uri-path file))
        (str/replace   "{basename}"   (uri-basename path))
        (str/replace   "{line}"       (str line))
        (str/replace   "{version}"    (str version))
        (force-replace "{git-commit}" git-commit))))

(defn- split-ns [namespace]
  (str/split (str namespace) #"\."))

(defn- namespace-parts [namespace]
  (->> (split-ns namespace)
       (reductions #(str %1 "." %2))
       (map symbol)))

(defn- add-depths [namespaces]
  (->> namespaces
       (map (juxt identity (comp count split-ns)))
       (reductions (fn [[_ ds] [ns d]] [ns (cons d ds)]) [nil nil])
       (rest)))

(defn- add-heights [namespaces]
  (for [[ns ds] namespaces]
    (let [d (first ds)
          h (count (take-while #(not (or (= d %) (= (dec d) %))) (rest ds)))]
      [ns d h])))

(defn- add-branches [namespaces]
  (->> (partition-all 2 1 namespaces)
       (map (fn [[[ns d0 h] [_ d1 _]]] [ns d0 h (= d0 d1)]))))

(defn- namespace-hierarchy [namespaces]
  (->> (map :name namespaces)
       (sort)
       (mapcat namespace-parts)
       (distinct)
       (add-depths)
       (add-heights)
       (add-branches)))

(defn- index-by [f m]
  (into {} (map (juxt f identity) m)))

;; The values in ns-tree-part are chosen for aesthetic reasons, based
;; on a text size of 15px and a line height of 31px.

(defn- ns-tree-part [height]
  (if (zero? height)
    [:span.tree [:span.top] [:span.bottom]]
    (let [row-height 31
          top        (- 0 21 (* height row-height))
          height     (+ 0 30 (* height row-height))]
      [:span.tree {:style (str "top: " top "px;")}
       [:span.top {:style (str "height: " height "px;")}]
       [:span.bottom]])))

(defn- index-link [project on-index?]
  (list
   [:h3.no-link [:span.inner "Project"]]
   [:ul.index-link
    [:li.depth-1 {:class (if on-index? "current")}
     (link-to "index.html" [:div.inner "Index"])]]))

(defn- topics-menu [project current-doc]
  (if-let [docs (seq (:documents project))]
    (list
     [:h3.no-link [:span.inner "Topics"]]
     [:ul
      (for [doc docs]
        [:li.depth-1
         {:class (if (= doc current-doc) " current")}
         (link-to (doc-filename doc) [:div.inner [:span (h (:title doc))]])])])))

(defn- nested-namespaces [namespaces current-ns]
  (let [ns-map (index-by :name namespaces)]
    [:ul
      (for [[name depth height branch?] (namespace-hierarchy namespaces)]
        (let [class  (str "depth-" depth (if branch? " branch"))
              short  (last (split-ns name))
              inner  [:div.inner (ns-tree-part height) [:span (h short)]]]
          (if-let [ns (ns-map name)]
            (let [class (str class (if (= ns current-ns) " current"))]
              [:li {:class class} (link-to (ns-filename ns) inner)])
            [:li {:class class} [:div.no-link inner]])))]))

(defn- flat-namespaces [namespaces current-ns]
  [:ul
   (for [ns (sort-by :name namespaces)]
     [:li.depth-1
      {:class (if (= ns current-ns) "current")}
      (link-to (ns-filename ns) [:div.inner [:span (h (:name ns))]])])])

(defn- namespace-list-type [project]
  (let [default (if (> (-> project :namespaces count) 1) :nested :flat)]
    (get-in project [:html :namespace-list] default)))

(defn- namespaces-menu [project current-ns]
  (let [namespaces (:namespaces project)]
    (list
     [:h3.no-link [:span.inner "Namespaces"]]
     (case (namespace-list-type project)
       :flat   (flat-namespaces namespaces current-ns)
       :nested (nested-namespaces namespaces current-ns)))))

(defn- primary-sidebar [project & [current]]
  [:div.sidebar.primary
   (index-link project (nil? current))
   (topics-menu project current)
   (namespaces-menu project current)])

(defn- sorted-public-vars [namespace]
  (sort-by (comp str/lower-case :name) (:publics namespace)))

(defn- vars-sidebar [namespace]
  [:div.sidebar.secondary
   [:h3 (link-to "#top" [:span.inner "Public Vars"])]
   [:ul
    (for [var (sorted-public-vars namespace)]
      (list*
       [:li.depth-1
        (link-to (var-uri namespace var) [:div.inner [:span (h (:name var))]])]
      (for [mem (:members var)]
        (let [branch? (not= mem (last (:members var)))
              class   (if branch? "depth-2 branch" "depth-2")
              inner   [:div.inner (ns-tree-part 0) [:span (h (:name mem))]]]
          [:li {:class class}
           (link-to (var-uri namespace mem) inner)]))))]])

(def ^:private default-meta
  [:meta {:charset "UTF-8"}])

(defn- project-title [project]
  [:span.project-title
   [:span.project-name (h (:name project))] " "
   [:span.project-version (h (:version project))]])

(defn- header [project]
  [:div#header
   [:h2 "Generated by " (link-to "https://github.com/weavejester/codox" "Codox")]
   [:h1 (link-to "index.html" (project-title project))]])

(defn- package [project]
  (if-let [p (:package project)]
    (if (= (namespace p) (name p))
      (symbol (name p))
      p)))

(defn- add-ending [^String s ^String ending]
  (if (.endsWith s ending) s (str s ending)))

(defn- strip-prefix [s prefix]
  (if s (str/replace s (re-pattern (str "(?i)^" prefix)) "")))

(defn- index-page [project]
  (html5
   [:head
    default-meta
    [:title (h (:name project)) " " (h (:version project))]]
   [:body
    (header project)
    (primary-sidebar project)
    [:div#content.namespace-index
     [:h1 (project-title project)]
     (if-let [license (-> (get-in project [:license :name]) (strip-prefix "the "))]
       [:h5.license
        "Released under the "
        (if-let [url (get-in project [:license :url])]
          (link-to url license)
          license)])
     (if-let [description (:description project)]
       [:div.doc [:p (h (add-ending description "."))]])
     (if-let [package (package project)]
       (list
        [:h2 "Installation"]
        [:p "To install, add the following dependency to your project or build file:"]
        [:pre.deps (h (str "[" package " " (pr-str (:version project)) "]"))]))
     (if-let [docs (seq (:documents project))]
       (list
        [:h2 "Topics"]
        [:ul.topics
         (for [doc docs]
           [:li (link-to (doc-filename doc) (h (:title doc)))])]))
     [:h2 "Namespaces"]
     (for [namespace (sort-by :name (:namespaces project))]
       [:div.namespace
        [:h3 (link-to (ns-filename namespace) (h (:name namespace)))]
        [:div.doc (format-docstring project nil (update-in namespace [:doc] util/summary))]
        [:div.index
         [:p "Public variables and functions:"]
         (unordered-list
          (for [var (sorted-public-vars namespace)]
            (list " " (link-to (var-uri namespace var) (h (:name var))) " ")))]])]]))

(defmulti format-document
  "Format a document into HTML."
  (fn [project doc] (:format doc)))

(defmethod format-document :markdown [project doc]
  [:div.markdown (markdown->html (:content doc) project)])

(defn- document-page [project doc]
  (html5
   [:head
    default-meta
    [:title (h (:title doc))]]
   [:body
    (header project)
    (primary-sidebar project doc)
    [:div#content.document
     [:div.doc (format-document project doc)]]]))

(defn- var-usage [var]
  (for [arglist (:arglists var)]
    (list* (:name var) arglist)))

(defn- added-and-deprecated-docs [var]
  (list
   (if-let [added (:added var)]
     [:h4.added "added in " added])
   (if-let [deprecated (:deprecated var)]
     [:h4.deprecated "deprecated" (if (string? deprecated) (str " in " deprecated))])))

(defn- remove-namespaces [x namespaces]
  (if (and (symbol? x) (contains? namespaces (namespace x)))
    (symbol (name x))
    x))

(defn- normalize-types [types]
  (read-string (pr-str types)))

(defn- pprint-str [x]
  (with-out-str (pp/pprint x)))

(defn- type-sig [namespace var]
  (let [implied-namespaces #{(str (:name namespace)) "clojure.core.typed"}]
    (->> (:type-sig var)
         (normalize-types)
         (walk/postwalk #(remove-namespaces % implied-namespaces))
         (pprint-str))))

(defn- var-docs [project namespace var]
  [:div.public.anchor {:id (h (var-id (:name var)))}
   [:h3 (h (:name var))]
   (if-not (= (:type var) :var)
     [:h4.type (name (:type var))])
   (if (:dynamic var)
     [:h4.dynamic "dynamic"])
   (added-and-deprecated-docs var)
   (if (:type-sig var)
     [:div.type-sig
      [:pre (h (type-sig namespace var))]])
   [:div.usage
    (for [form (var-usage var)]
      [:code (h (pr-str form))])]
   [:div.doc (format-docstring project namespace var)]
   (if-let [members (seq (:members var))]
     [:div.members
      [:h4 "members"]
      [:div.inner
       (let [project (dissoc project :source-uri)]
         (map (partial var-docs project namespace) members))]])
   (if (:source-uri project)
     (if (:path var)
       [:div.src-link (link-to (var-source-uri project var) "view source")]
       (println "Could not generate source link for" (:name var))))])

(defn- namespace-page [project namespace]
  (html5
   [:head
    default-meta
    [:title (h (:name namespace)) " documentation"]]
   [:body
    (header project)
    (primary-sidebar project namespace)
    (vars-sidebar namespace)
    [:div#content.namespace-docs
     [:h1#top.anchor (h (:name namespace))]
     (added-and-deprecated-docs namespace)
     [:div.doc (format-docstring project namespace namespace)]
     (for [var (sorted-public-vars namespace)]
       (var-docs project namespace var))]]))

(defn- mkdirs [output-dir & dirs]
  (doseq [dir dirs]
    (.mkdirs (io/file output-dir dir))))

(defn- write-index [output-dir project]
  (spit (io/file output-dir "index.html") (transform-html project (index-page project))))

(defn- write-namespaces [output-dir project]
  (doseq [namespace (:namespaces project)]
    (spit (ns-filepath output-dir namespace)
          (transform-html project (namespace-page project namespace)))))

(defn- write-documents [output-dir project]
  (doseq [document (:documents project)]
    (spit (doc-filepath output-dir document)
          (transform-html project (document-page project document)))))

(defn- theme-path [theme]
  (let [theme-name (if (vector? theme) (first theme) theme)]
    (str "codox/theme/" (name theme-name))))

(defn- insert-params [theme-data theme]
  (let [params   (if (vector? theme) (or (second theme) {}) {})
        defaults (:defaults theme-data {})]
    (assert (map? params) "Theme parameters must be a map")
    (assert (map? defaults) "Theme defaults must be a map")
    (->> (dissoc theme-data :defaults)
         (walk/postwalk #(if (keyword? %) (params % (defaults % %)) %)))))

(defn- read-theme [theme]
  (some-> (theme-path theme)
          (str "/theme.edn")
          io/resource slurp
          edn/read-string
          (insert-params theme)))

(defn- make-parent-dir [file]
  (-> file io/file .getParentFile .mkdirs))

(defn- copy-resource [resource output-path]
  (io/copy (io/input-stream (io/resource resource)) output-path))

(defn- copy-theme-resources [output-dir project]
  (doseq [theme (:themes project)]
    (let [root (theme-path theme)]
      (doseq [path (:resources (read-theme theme))]
        (let [output-file (io/file output-dir path)]
          (make-parent-dir output-file)
          (copy-resource (str root "/" path) output-file))))))

(defn- apply-one-theme [project theme]
  (if-let [{:keys [transforms]} (read-theme theme)]
    (update-in project [:html :transforms] concat transforms)
    (throw (IllegalArgumentException. (format "Could not find Codox theme: %s" theme)))))

(defn- apply-theme-transforms [{:keys [themes] :as project}]
  (reduce apply-one-theme project themes))

(defn write-docs
  "Take raw documentation info and turn it into formatted HTML."
  [{:keys [output-path] :as project}]
  (let [project (-> (apply-theme-transforms project)
                    (assoc :parser (parser project)
                           :renderer (renderer project)))]
    (doto output-path
      (copy-theme-resources project)
      (write-index project)
      (write-namespaces project)
      (write-documents project))))
