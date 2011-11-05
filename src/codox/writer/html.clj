(ns codox.writer.html
  "Documentation writer that outputs HTML."
  (:use [hiccup core page-helpers])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- ns-filename [namespace]
  (str (:name namespace) ".html"))

(defn- ns-filepath [namespace]
  (str "doc/" (ns-filename namespace)))

(defn- var-uri [namespace var]
  (str (ns-filename namespace) "#" (:name var)))

(defn- link-to-ns [namespace]
  (link-to (ns-filename namespace) (:name namespace)))

(defn- link-to-var [namespace var]
  (link-to (var-uri namespace var) (:name var)))

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

(defn- project-title [project]
  (str (str/capitalize (:name project)) " "
       (:version project) " API documentation"))

(defn- index-page [project]
  (html5
   [:head
    (include-css "css/default.css")
    [:title (project-title project)]]
   [:body
    (namespaces-menu project)
    [:div#content
     [:h1 (project-title project)]
     [:div.doc (:description project)]
     (for [namespace (:namespaces project)]
       [:div.namespace
        [:h2 (link-to-ns namespace)]
        [:pre.doc (:doc namespace)]
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
    (include-css "css/default.css")
    [:title (namespace-title namespace)]]
   [:body
    (namespaces-menu project namespace)
    (vars-menu namespace)
    [:div#content
     [:h1 (namespace-title namespace)]
     [:pre.doc (:doc namespace)]
     (for [var (:publics namespace)]
      [:div.public {:id (:name var)}
       [:h3 (:name var)]
       [:div.usage
        (for [form (var-usage var)]
          [:code (pr-str form)])]
       [:pre.doc (:doc var)]])]]))

(defn- copy-resource [src dest]
  (io/copy (io/input-stream (io/resource src))
           (io/file dest)))

(defn- mkdirs [dir]
  (.mkdirs (io/file dir)))

(defn write-docs
  "Take raw documentation info and turn it into formatted HTML."
  [project]
  (mkdirs "doc/css")
  (copy-resource "codox/css/default.css" "doc/css/default.css")
  (spit "doc/index.html" (index-page project))
  (doseq [namespace (:namespaces project)]
    (spit (ns-filepath namespace)
          (namespace-page project namespace))))
