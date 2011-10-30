(ns codox.writer.html
  "Documentation writer that outputs HTML."
  (:use [hiccup core page-helpers]))

(defn- make-index
  [namespaces]
  (html5
   [:head
    [:title "Documentation"]]
   [:body
    (for [namespace namespaces]
      [:div.namespace
       [:h1 (:name namespace)]])]))

(defn write-docs
  "Take raw documentation info and turn it into formatted HTML."
  [info]
  (spit "index.html" (make-index info)))
