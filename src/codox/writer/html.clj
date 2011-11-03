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

(defn- html-page [title & body]
  (html5
   [:head
    [:title title]
    (include-css "css/default.css")]
   [:body
    [:h1 title]
    body]))

(defn- make-var-links [namespace]
  [:div.index
   [:p "Public variables and functions:"]
   [:ul
    (for [var (:publics namespace)]
      [:li (link-to (var-uri namespace var) (:name var))])]])

(defn- make-index [options]
  (html-page
   (str (str/capitalize (:name options)) " " (:version options)
        " API documentation")
   [:div.doc (:description options)]
   (for [namespace (:namespaces options)]
     [:div.namespace
      [:h2 (link-to (ns-filename namespace) (:name namespace))]
      [:pre.doc (:doc namespace)]
      (make-var-links namespace)])))

(defn- var-usage [var]
  (for [arglist (:arglists var)]
    (list* (:name var) arglist)))

(defn- make-ns-page [namespace]
  (html-page
   (str (:name namespace) " documentation")
   [:pre.doc (:doc namespace)]
   (make-var-links namespace)
   (for [var (:publics namespace)]
      [:div.public {:id (:name var)}
       [:h3 (:name var)]
       [:div.usage
        (for [form (var-usage var)]
          [:code (pr-str form)])]
       [:pre.doc (:doc var)]])))

(defn- copy-resource [src dest]
  (io/copy (io/input-stream (io/resource src))
           (io/file dest)))

(defn- mkdirs [dir]
  (.mkdirs (io/file dir)))

(defn write-docs
  "Take raw documentation info and turn it into formatted HTML."
  [options]
  (mkdirs "doc/css")
  (copy-resource "codox/css/default.css" "doc/css/default.css")
  (spit "doc/index.html" (make-index options))
  (doseq [namespace (:namespaces options)]
    (spit (ns-filepath namespace) (make-ns-page namespace))))
