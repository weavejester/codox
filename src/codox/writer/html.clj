(ns codox.writer.html
  "Documentation writer that outputs HTML."
  (:use [hiccup core page-helpers])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- ns-filename [namespace]
  (str (:name namespace) ".html"))

(defn- ns-filepath [namespace]
  (str "doc/" (ns-filename namespace)))

(defn- var-id [var]
  (str "var-" (:name var)))

(defn- var-uri [namespace var]
  (str (ns-filename namespace) "#" (var-id var)))

(defn- link-to-ns [namespace]
  (link-to (ns-filename namespace) (h (:name namespace))))

(defn- link-to-var [namespace var]
  (link-to (var-uri namespace var) (h (:name var))))

(defn- namespaces-menu [project & [namespace]]
  [:div#namespaces.sidebar
   [:h3 "Namespaces"]
   [:ul
    (for [ns (:namespaces project)]
      (if (= ns namespace)
        [:li.current (link-to-ns ns)]
        [:li (link-to-ns ns)]))]])

(defn- var-links [namespace]
  (unordered-list
    (map (partial link-to-var namespace)
         (:publics namespace))))

(defn- vars-menu [namespace]
  [:div#vars.sidebar
   [:h3 "Public Vars"]
   (var-links namespace)])

(def ^{:private true} default-includes
  (list
   (include-css "css/default.css")
   (include-js "js/jquery.min.js")
   (include-js "js/page_effects.js")))

(defn- project-title [project]
  (str (str/capitalize (:name project)) " "
       (:version project) " API documentation"))

(defn- header [project]
  [:div#header
   [:h1 (link-to "index.html" (h (project-title project)))]])

(defn- index-page [project]
  (html5
   [:head
    default-includes
    [:title (h (project-title project))]]
   [:body
    (header project)
    (namespaces-menu project)
    [:div#content.namespace-index
     [:h2 (h (project-title project))]
     [:div.doc (h (:description project))]
     (for [namespace (:namespaces project)]
       [:div.namespace
        [:h3 (link-to-ns namespace)]
        [:pre.doc (h (:doc namespace))]
        [:div.index
         [:p "Public variables and functions:"]
         (var-links namespace)]])]]))

(defn- var-usage [var]
  (for [arglist (:arglists var)]
    (list* (:name var) arglist)))

(defn- namespace-title [namespace]
  (str (:name namespace) " documentation"))

(defn- namespace-page [project namespace]
  (html5
   [:head
    default-includes
    [:title (h (namespace-title namespace))]]
   [:body
    (header project)
    (namespaces-menu project namespace)
    (vars-menu namespace)
    [:div#content.namespace-docs
     [:h2 (h (namespace-title namespace))]
     [:pre.doc (h (:doc namespace))]
     (for [var (:publics namespace)]
       [:div.public {:id (h (var-id var))}
        [:h3 (h (:name var))]
        [:div.usage
         (for [form (var-usage var)]
           [:code (h (pr-str form))])]
        [:pre.doc (h (:doc var))]])]]))

(defn- copy-resource [src dest]
  (io/copy (io/input-stream (io/resource src))
           (io/file dest)))

(defn- mkdirs [& dirs]
  (doseq [dir dirs]
    (.mkdirs (io/file dir))))

(defn write-docs
  "Take raw documentation info and turn it into formatted HTML."
  [project]
  (mkdirs "doc/css" "doc/js")
  (copy-resource "codox/css/default.css" "doc/css/default.css")
  (copy-resource "codox/js/jquery.min.js" "doc/js/jquery.min.js")
  (copy-resource "codox/js/page_effects.js" "doc/js/page_effects.js")
  (spit "doc/index.html" (index-page project))
  (doseq [namespace (:namespaces project)]
    (spit (ns-filepath namespace)
          (namespace-page project namespace))))
