(ns codox.reader.plaintext
  (:require [clojure.java.io :as io]
            [codox.utils :as util]))

(defn basename [file]
  (second (re-find #"([^/]*)\.(.*?)$" (util/uri-path file))))

(defn file-extension [file]
  (re-find #"\..*?$" (str file)))

(def file-types
  {".md"       :markdown
   ".markdown" :markdown})

(defmulti find-title
  (fn [file-type _] file-type))

(defmethod find-title :markdown [_ content]
  (second (re-find #"(?m)^\s*#\s*(.*)\s*$" content)))

(defn read-file [file]
  (if-let [format (file-types (file-extension file))]
    (let [content (slurp file)]
      {:name    (basename file)
       :title   (find-title format content)
       :format  format
       :content content})))

(defn read-documents
  ([]
   (read-documents "doc"))
  ([path]
   (keep read-file (file-seq (io/file path))))
  ([path & paths]
   (mapcat read-documents (cons path paths))))
