(ns codox.writer.html
  "Documentation writer that outputs HTML."
  (:use [hiccup core page-helpers])
  (:require [clojure.java.io :as io]))

(defn- ns-filename [namespace]
  (str (:name namespace) ".html"))

(defn- ns-filepath [namespace]
  (str "doc/" (ns-filename namespace)))

(defn- var-uri [namespace var]
  (str (ns-filename namespace) "#" (:name var)))

(defn- make-index [namespaces]
  (html5
   [:head
    [:title "API documentation"]
    (include-css "css/default.css")]
   [:body
    [:h1 "API documentation"]
    (for [namespace namespaces]
      [:div.namespace
       [:h2 (link-to (ns-filename namespace) (:name namespace))]
       [:pre.doc (:doc namespace)]
       [:ul.publics
        (for [var (:publics namespace)]
          [:li (link-to (var-uri namespace var) (:name var))])]])]))

(defn- var-usage [var]
  (for [arglist (:arglists var)]
    (list* (:name var) arglist)))

(defn- make-ns-page [namespace]
  (html5
   [:head
    [:title (:name namespace) " documentation"]
    (include-css "css/default.css")]
   [:body
    [:h1 (:name namespace) " documentation"]
    [:pre.doc (:doc namespace)]
    (for [var (:publics namespace)]
      [:div.public {:id (:name var)}
       [:h3 (:name var)]
       [:div.usage
        (for [form (var-usage var)]
          [:code (pr-str form)])]
       [:pre.doc "  " (:doc var)]])]))

(defn- copy-resource [src dest]
  (io/copy (io/input-stream (io/resource src))
           (io/file dest)))

(defn- mkdirs [dir]
  (.mkdirs (io/file dir)))

(defn write-docs
  "Take raw documentation info and turn it into formatted HTML."
  [info]
  (mkdirs "doc/css")
  (copy-resource "codox/css/default.css" "doc/css/default.css")
  (spit "doc/index.html" (make-index info))
  (doseq [namespace info]
    (spit (ns-filepath namespace) (make-ns-page namespace))))
