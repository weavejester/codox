(ns codox.writer.html
  "Documentation writer that outputs HTML."
  (:use [hiccup core page element])
  (:import [java.net URLEncoder]
           [org.pegdown PegDownProcessor Extensions LinkRenderer LinkRenderer$Rendering]
           [org.pegdown.ast WikiLinkNode])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [codox.utils :as util]))

(defn- var-id [var]
  (str "var-" (-> var name URLEncoder/encode (str/replace "%" "."))))

(def ^:private url-regex
  #"((https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|])")

(defn- add-anchors [text]
  (if text
    (str/replace text url-regex "<a href=\"$1\">$1</a>")))

(defmulti format-doc
  "Format the docstring of a var or namespace into HTML."
  (fn [project ns var] (:doc/format var))
  :default :plaintext)

(defmethod format-doc :plaintext [_ _ metadata]
  [:pre.plaintext (add-anchors (h (:doc metadata)))])

(def ^:private pegdown
  (PegDownProcessor.
   (bit-or Extensions/AUTOLINKS
           Extensions/QUOTES
           Extensions/SMARTS
           Extensions/STRIKETHROUGH
           Extensions/TABLES
           Extensions/FENCED_CODE_BLOCKS
           Extensions/WIKILINKS)
   2000))

(defn- find-wikilink [project ns text]
  (if-let [var (util/search-vars (:namespaces project) text (:name ns))]
    (str (namespace var) ".html#" (var-id var))))

(defn- link-renderer [project ns]
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

(defmethod format-doc :markdown [project ns metadata]
  [:div.markdown
   (if-let [doc (:doc metadata)]
     (.markdownToHtml pegdown doc (link-renderer project ns)))])

(defn- ns-filename [namespace]
  (str (:name namespace) ".html"))

(defn- ns-filepath [output-dir namespace]
  (str output-dir "/" (ns-filename namespace)))

(defn- var-uri [namespace var]
  (str (ns-filename namespace) "#" (var-id (:name var))))

(defn- get-mapping-fn [mappings path]
  (some (fn [[re f]] (if (re-find re path) f)) mappings))

(defn- var-source-uri
  [{:keys [src-dir-uri src-uri-mapping src-linenum-anchor-prefix]}
   {:keys [path file line]}]
  (str src-dir-uri
       (if-let [mapping-fn (get-mapping-fn src-uri-mapping (str path))]
         (mapping-fn file)
         path)
       (if src-linenum-anchor-prefix
         (str "#" src-linenum-anchor-prefix line))))

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

(defn- namespaces-menu [project & [current]]
  (let [namespaces (:namespaces project)
        ns-map     (index-by :name namespaces)]
    [:div#namespaces.sidebar
     [:h3 (link-to "index.html" [:span.inner "Namespaces"])]
     [:ul
      (for [[name depth height branch?] (namespace-hierarchy namespaces)]
        (let [class  (str "depth-" depth (if branch? " branch"))
              short  (last (split-ns name))
              inner  [:div.inner (ns-tree-part height) [:span (h short)]]]
          (if-let [ns (ns-map name)]
            (let [class (str class (if (= ns current) " current"))]
              [:li {:class class} (link-to (ns-filename ns) inner)])
            [:li {:class class} [:div.no-link inner]])))]]))

(defn- sorted-public-vars [namespace]
  (sort-by (comp str/lower-case :name) (:publics namespace)))

(defn- vars-menu [namespace]
  [:div#vars.sidebar
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
  (str (:name project) " " (:version project)))

(defn- header [project]
  [:div#header
   [:h2 "Generated by " (link-to "https://github.com/weavejester/codox" "Codox")]
   [:h1 (link-to "index.html" (h (project-title project)) " API documentation")]])

(defn- index-page [project]
  (html5
   [:head
    default-includes
    [:title (h (project-title project)) " API documentation"]]
   [:body
    (header project)
    (namespaces-menu project)
    [:div#content.namespace-index
     [:h2 (h (project-title project))]
     [:div.doc [:p (h (:description project))]]
     (for [namespace (sort-by :name (:namespaces project))]
       [:div.namespace
        [:h3 (link-to (ns-filename namespace) (h (:name namespace)))]
        [:div.doc (format-doc project nil (update-in namespace [:doc] util/summary))]
        [:div.index
         [:p "Public variables and functions:"]
         (unordered-list
          (for [var (sorted-public-vars namespace)]
            (list " " (link-to (var-uri namespace var) (h (:name var))) " ")))]])]]))

(defn- var-usage [var]
  (for [arglist (:arglists var)]
    (list* (:name var) arglist)))

(defn- var-docs [project namespace var]
  [:div.public.anchor {:id (h (var-id (:name var)))}
   [:h3 (h (:name var))]
   (if-not (= (:type var) :var)
     [:h4.type (name (:type var))])
   (if (:dynamic var)
     [:h4.dynamic "dynamic"])
   (if-let [added (:added var)]
     [:h4.added "added in " added])
   (if-let [deprecated (:deprecated var)]
     [:h4.deprecated "deprecated" (if (string? deprecated) (str " in " deprecated))])
   [:div.usage
    (for [form (var-usage var)]
      [:code (h (pr-str form))])]
   [:div.doc (format-doc project namespace var)]
   (if-let [members (seq (:members var))]
     [:div.members
      [:h4 "members"]
      [:div.inner
       (let [project (dissoc project :src-dir-uri)]
         (map (partial var-docs project namespace) members))]])
   (if (:src-dir-uri project)
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
    (namespaces-menu project namespace)
    (vars-menu namespace)
    [:div#content.namespace-docs
     [:h2#top.anchor (h (:name namespace))]
     [:div.doc (format-doc project nil namespace)]
     (for [var (sorted-public-vars namespace)]
       (var-docs project namespace var))]]))

(defn- copy-resource [output-dir src dest]
  (io/copy (io/input-stream (io/resource src))
           (io/file output-dir dest)))

(defn- mkdirs [output-dir & dirs]
  (doseq [dir dirs]
    (.mkdirs (io/file output-dir dir))))

(defn- write-index [output-dir project]
  (spit (io/file output-dir "index.html") (index-page project)))

(defn- write-namespaces
  [output-dir project]
  (doseq [namespace (:namespaces project)]
    (spit (ns-filepath output-dir namespace)
          (namespace-page project namespace))))

(defn write-docs
  "Take raw documentation info and turn it into formatted HTML."
  [project]
  (doto (:output-dir project "doc")
    (mkdirs "css" "js")
    (copy-resource "codox/css/default.css" "css/default.css")
    (copy-resource "codox/js/jquery.min.js" "js/jquery.min.js")
    (copy-resource "codox/js/page_effects.js" "js/page_effects.js")
    (write-index project)
    (write-namespaces project))
  nil)
