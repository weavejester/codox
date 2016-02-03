(ns codox.writer.html
  "Documentation writer that outputs HTML."
  (:use [hiccup core page element])
  (:import [java.net URLEncoder]
           [java.io File]
           [org.pegdown PegDownProcessor Extensions LinkRenderer LinkRenderer$Rendering]
           [org.pegdown.ast WikiLinkNode])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [net.cgrand.enlive-html :as enlive-html]
            [net.cgrand.jsoup :as jsoup]
            [codox.utils :as util]))

(def enlive-operations
  {:append enlive-html/append})

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
  (let [stream (io/input-stream (.getBytes s "UTF-8"))]
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

(def ^:private pegdown
  (PegDownProcessor.
   (bit-or Extensions/AUTOLINKS
           Extensions/QUOTES
           Extensions/SMARTS
           Extensions/STRIKETHROUGH
           Extensions/TABLES
           Extensions/FENCED_CODE_BLOCKS
           Extensions/WIKILINKS
           Extensions/DEFINITIONS
           Extensions/ABBREVIATIONS
           Extensions/ATXHEADERSPACE
           Extensions/RELAXEDHRULES
           Extensions/EXTANCHORLINKS)
   2000))

(defn- find-wikilink [project ns text]
  (let [ns-strs (map (comp str :name) (:namespaces project))]
    (if (contains? (set ns-strs) text)
      (str text ".html")
      (if-let [var (util/search-vars (:namespaces project) text (:name ns))]
        (str (namespace var) ".html#" (var-id var))))))

(defn- link-renderer [project & [ns]]
  (proxy [LinkRenderer] []
    (render
      ([node]
         (if (instance? WikiLinkNode node)
           (let [text (.getText node)]
             (LinkRenderer$Rendering. (find-wikilink project ns text) text))
           (proxy-super render node)))
      ([node text]
         (proxy-super render node text))
      ([node url title text]
         (proxy-super render node url title text)))))

(defmethod format-docstring :markdown [project ns metadata]
  [:div.markdown
   (if-let [doc (:doc metadata)]
     (.markdownToHtml pegdown doc (link-renderer project ns)))])

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

(defn- uri-path [path]
  (str/replace (str path) File/separator "/"))

(defn- var-source-uri
  [{:keys [source-uri version]}
   {:keys [path file line]}]
  (let [path (uri-path path)
        uri  (if (map? source-uri) (get-source-uri source-uri path) source-uri)]
    (-> uri
        (str/replace "{filepath}"  path)
        (str/replace "{classpath}" (uri-path file))
        (str/replace "{line}"      (str line))
        (str/replace "{version}"   version))))

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
      (for [doc (sort-by :name docs)]
        [:li.depth-1
         {:class (if (= doc current-doc) " current")}
         (link-to (doc-filename doc) [:div.inner [:span (h (:title doc))]])])])))

(defn- namespaces-menu [project current-ns]
  (let [namespaces (:namespaces project)
        ns-map     (index-by :name namespaces)]
    (list
     [:h3.no-link [:span.inner "Namespaces"]]
     [:ul
      (for [[name depth height branch?] (namespace-hierarchy namespaces)]
        (let [class  (str "depth-" depth (if branch? " branch"))
              short  (last (split-ns name))
              inner  [:div.inner (ns-tree-part height) [:span (h short)]]]
          (if-let [ns (ns-map name)]
            (let [class (str class (if (= ns current-ns) " current"))]
              [:li {:class class} (link-to (ns-filename ns) inner)])
            [:li {:class class} [:div.no-link inner]])))])))

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

(def ^{:private true} default-includes
  (list
   [:meta {:charset "UTF-8"}]
   (include-css "css/default.css")
   (include-js "js/jquery.min.js")
   (include-js "js/page_effects.js")))

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

(defn- index-page [project]
  (html5
   [:head
    default-includes
    [:title (h (:name project)) " " (h (:version project))]]
   [:body
    (header project)
    (primary-sidebar project)
    [:div#content.namespace-index
     [:h1 (project-title project)]
     [:div.doc [:p (h (:description project))]]
     (if-let [package (package project)]
       (list
        [:h2 "Installation"]
        [:p "To install, add the following dependency to your project file:"]
        [:pre.deps (h (str "[" package " " (pr-str (:version project)) "]"))]))
     (if-let [docs (seq (:documents project))]
       (list
        [:h2 "Topics"]
        [:ul.topics
         (for [doc (sort-by :name docs)]
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
  [:div.markdown (.markdownToHtml pegdown (:content doc) (link-renderer project))])

(defn- document-page [project doc]
  (html5
   [:head
    default-includes
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

(defn- var-docs [project namespace var]
  [:div.public.anchor {:id (h (var-id (:name var)))}
   [:h3 (h (:name var))]
   (if-not (= (:type var) :var)
     [:h4.type (name (:type var))])
   (if (:dynamic var)
     [:h4.dynamic "dynamic"])
   (added-and-deprecated-docs var)
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
    default-includes
    [:title (h (:name namespace)) " documentation"]]
   [:body
    (header project)
    (primary-sidebar project namespace)
    (vars-sidebar namespace)
    [:div#content.namespace-docs
     [:h1#top.anchor (h (:name namespace))]
     (added-and-deprecated-docs namespace)
     [:div.doc (format-docstring project nil namespace)]
     (for [var (sorted-public-vars namespace)]
       (var-docs project namespace var))]]))

(defn- copy-resource [output-dir src dest]
  (io/copy (io/input-stream (io/resource src))
           (io/file output-dir dest)))

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

(defn write-docs
  "Take raw documentation info and turn it into formatted HTML."
  [{:keys [output-path] :as project}]
  (doto output-path
    (mkdirs "css" "js")
    (copy-resource "codox/css/default.css" "css/default.css")
    (copy-resource "codox/js/jquery.min.js" "js/jquery.min.js")
    (copy-resource "codox/js/page_effects.js" "js/page_effects.js")
    (write-index project)
    (write-namespaces project)
    (write-documents project))
  (println "Generated HTML docs in" (.getAbsolutePath (io/file output-path))))
