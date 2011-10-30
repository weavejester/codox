(ns codox.writer.html
  "Documentation writer that outputs HTML."
  (:use [hiccup core page-helpers])
  (:require [clojure.java.io :as io]))

(defn- ns-filename [namespace]
  (str (:name namespace) ".html"))

(defn- ns-filepath [namespace]
  (str "doc/" (ns-filename namespace)))

(defn- make-index
  [namespaces]
  (html5
   [:head
    [:title "API documentation"]]
   [:body
    [:h1 "API documentation"]
    (for [namespace namespaces]
      [:div.namespace
       [:h2 (link-to (ns-filename namespace) (:name namespace))]
       [:pre.doc (:doc namespace)]])]))

(defn- make-ns-page
  [namespace]
  (html5
   [:head
    [:title (:name namespace) " documentation"]]
   [:body
    [:h1 (:name namespace) " documentation"]
    [:div.doc (:doc namespace)]
    (for [public (:publics namespace)]
      [:div.public
       [:h3 (:name public)]
       [:pre (pr-str (:arglists public))]
       [:pre.doc "  " (:doc public)]])]))

(defn write-docs
  "Take raw documentation info and turn it into formatted HTML."
  [info]
  (.mkdirs (io/file "doc"))
  (spit "doc/index.html" (make-index info))
  (doseq [namespace info]
    (spit (ns-filepath namespace) (make-ns-page namespace))))
