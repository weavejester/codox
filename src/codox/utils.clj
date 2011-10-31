(ns codox.utils
  "Miscellaneous utility functions."
  (:require [clojure.string :as str]))

(defn- find-minimum [coll]
  (if (seq coll)
    (apply min coll)))

(defn- find-smallest-indent [text]
  (->> (str/split-lines text)
       (remove str/blank?)
       (map #(re-find #"^\s+" %))
       (map count)
       (find-minimum)))

(defn unindent
  "Unindent a block of text by a specific amount or the smallest common
  indentation size."
  ([text]
     (unindent text (find-smallest-indent text)))
  ([text indent-size]
     (let [re (re-pattern (str "^\\s{0," indent-size "}"))]
       (->> (str/split-lines text)
            (map #(str/replace % re ""))
            (str/join "\n")))))
