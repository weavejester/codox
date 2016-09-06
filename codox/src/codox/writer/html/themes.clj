(ns codox.writer.html.themes
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [resauce.core :as rs]))

;; ## Resources

(defn- theme-path
  [theme]
  (str "codox/themes/" (name theme)))

(defn- directory-listing
  ([uri] (directory-listing uri ""))
  ([uri prefix]
   (let [uri-length (count (str uri))]
     (for [entry (rs/url-dir uri)
           :let [relative-name (subs (str entry) uri-length)]
           [child-name child] (if (.endsWith relative-name "/")
                                (directory-listing entry relative-name)
                                [[relative-name entry]])]
       [(str prefix child-name) child]))))

(defn- theme-resources
  [theme]
  (->> (str (theme-path theme) "/public")
       (rs/resources)
       (mapcat directory-listing)))

(defn- copy-resource
  [output-dir src dest]
  (with-open [in (io/input-stream src)]
    (io/copy in (io/file output-dir dest))))

(defn- copy-theme-resources*
  [output-dir theme]
  (doseq [[path uri] (theme-resources theme)
          :let [dest (subs path 1)]]
    (copy-resource output-dir uri dest)))

(defn copy-theme-resources
  [output-dir {:keys [theme themes] :or {theme :default}}]
  (copy-resource
    output-dir
    (io/resource "codox/highlight/highlight.min.js")
    "js/highlight.min.js")
  (doseq [theme (or themes [theme])]
    (copy-theme-resources* output-dir theme)))

;; ## Project

(defn- theme-info
  [theme]
  (some-> (theme-path theme)
          (str "/theme.edn")
          (io/resource)
          (slurp)
          (edn/read-string)))

(defn- prepare-single-theme
  [project theme]
  (if-let [{:keys [transforms]} (theme-info theme)]
    (-> project
        (update-in [:html :transforms] concat transforms))
    (throw
      (IllegalArgumentException.
        (format
          "No such codox theme: %s. (Did you forget to add the theme dependency?)"
          theme)))))

(defn prepare
  [{:keys [theme themes] :or {theme :default} :as project}]
  (when (and themes (not (vector? themes)))
    (throw
      (IllegalArgumentException.
        (str "Codox ':themes' key has to be a vector (given:"
             (pr-str themes)
             ")."))))
  (let [themes (or themes [theme])]
    (reduce prepare-single-theme project themes)))
