(ns codox.writer.html.themes
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn- theme-path [theme]
  (str "codox/themes/" (name theme)))

(defn- theme-info [theme]
  (some-> (theme-path theme)
          (str "/theme.edn")
          (io/resource)
          (slurp)
          (edn/read-string)))

(defn- copy-theme-resources* [output-dir theme]
  (let [root (theme-path theme)]
    (doseq [path (:resources (theme-info theme))]
      (io/copy (io/input-stream (io/resource (str root "/public/" path)))
               (io/file output-dir path)))))

(defn copy-theme-resources [output-dir {:keys [themes]}]
  (io/copy (io/input-stream (io/resource "codox/highlight/highlight.min.js"))
           (io/file output-dir "js/highlight.min.js"))
  (doseq [theme themes]
    (copy-theme-resources* output-dir theme)))

(defn- prepare-single-theme [project theme]
  (if-let [{:keys [transforms]} (theme-info theme)]
    (update-in project [:html :transforms] concat transforms)
    (throw
      (IllegalArgumentException.
        (format
          "No such codox theme: %s. (Did you forget to add the theme dependency?)"
          theme)))))

(defn prepare [{:keys [themes] :as project}]
  (reduce prepare-single-theme project themes))
