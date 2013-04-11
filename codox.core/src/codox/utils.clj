(ns codox.utils
  "Miscellaneous utility functions."
  (:require [clojure.string :as str]
            [clojure.java.io :as io]))

(defn- find-minimum [coll]
  (if (seq coll)
    (apply min coll)))

(defn- find-smallest-indent [text]
  (->> (str/split-lines text)
       (remove str/blank?)
       (map #(re-find #"^\s+" %))
       (map count)
       (find-minimum)))

(defn- find-file-in-repo
  "Given a classpath-relative file (as from the output of
   `codox.reader/read-namespaces`), and a sequence of source directory paths,
   returns a File object indicating the file from the repo root."
  [file sources]
  (if file
    (->> (map #(io/file % file) sources)
         (filter #(.exists %))
         first)))

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

(defn symbol-set
  "Accepts a single item (or a collection of items), converts them to
  symbols and returns them in set form."
  [x]
  (->> (if (coll? x) x [x])
       (filter identity)
       (map symbol)
       (into #{})))

(defn ns-filter
  "Accepts a sequence of namespaces (generated by
  `codox.reader/read-namespaces`), a sequence of namespaces to keep
  and a sequence of namespaces to drop. The sequence is returned with
  all namespaces in `exclude` and all namespaces NOT in `include`
  removed."
  [ns-seq include exclude]
  (let [has-name? (fn [names] (comp (symbol-set names) :name))
        ns-seq    (remove (has-name? exclude) ns-seq)]
    (if include
      (filter (has-name? include) ns-seq)
      ns-seq)))

(defn add-source-paths
  "Accepts a sequence of namespaces (generated by
   `codox.reader/read-namespaces`), and a list of source
   directories. The sequence is returned with :path items added in
   each public var's entry in the :publics map, which indicate the
   path to the source file relative to the repo root."
  [ns-seq sources]
  (for [ns ns-seq]
    (assoc ns
      :publics
      (map #(assoc % :path (find-file-in-repo (:file %) (or sources ["src"])))
           (:publics ns)))))

(defn summary
  "Return the summary of a docstring.
   The summary is the first portion of the string, from the first
   character to the first page break (\f) character OR the first TWO
   newlines."

  ([s] (str/trim (summary (str s) [#"\f" #"\n\n"])))

  ([s [re & res]]
     (let [[sum tail] (str/split s re)]
       (if (and (not tail) res) (recur s res) sum))))
