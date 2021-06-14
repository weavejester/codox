(ns ^{:doc "Generate, as SVG, the characteristic graphs of Cloverage."
      :author "Simon Brooke [simon@journeyman.cc]"}
 codox.cloverage-integration.graph
  (:require [clojure.string :as s]
            [hiccup.core :refer [html]]))


(def named-colours
  "Useful valid SVG colour names for bars in graphs, roughly
   in spectral order."
  [:crimson
   :red
   :orangered
   :coral
   :orange
   :gold
   :yellow
   :greenyellow
   :lime
   :green
   :turquoise
   :blue
   :blueviolet
   :indigo])

(defn colour-for-segment
  "Return the appropriate colour for the graph segment whose index is `n`,
   taken from among these `colours` if passed, or from `named-colours` 
   otherwise."
  ([n]
   (colour-for-segment n named-colours))
  ([n colours]
   (nth colours (mod (dec n) (count colours)))))

(def default-style
  "The components of the default style string for relevant elements, as a map.
   Probably better to put these defaults into a separate stylesheet, but 'twill
   do for now."
  {:rect
   {:fill-opacity 0.75}
   :text
   {}})

(defn style-string
  "Generate an appropriate style string (i.e. value for the `style` attribute)
   for a rect from this style map."
  [style-map tag]
  (let [with-defaults (merge (default-style tag) style-map)]
    (s/join "; " (map
                  #(str (name %) ": " (let [val (with-defaults %)]
                                        (if (keyword? val)
                                          (name val)
                                          val)))
                  (keys with-defaults)))))

(defn rect
  ([left top width height id style]
   (rect left top width height id style nil))
  ([left top width height id style content]
   [:g
    [:rect
     {:style (style-string style :rect)
      :x left
      :y top
      :width width
      :height height
      :id id}]
    (cond
      (string? content)
      [:text {:x (+ left 4)
              :y (+ top 2 (* height 0.75))
              :font-size (str (* height 0.75) "px")}
       [:tspan content]]
      (vector? content)
      content)]))

(defn graph-segment
  [left width height index value colours]
  (let [i (inc index)]
    (rect left 2 width height
          (str "segment_" i)
          {:fill (colour-for-segment i colours)}
          (str value))))

(defn graph-segments
  [values width height colours]
  (let [total (reduce + values) scale (float (/ width total))]
    (loop [left 2 vs values n 0 result '()]
      (if
       (empty? vs)
        (apply vector (reverse result))
        (let [v (first vs)
              w (* v scale)]
          (recur
           (+ left w)
           (rest vs)
           (inc n)
           (cons (graph-segment left w height n v colours)
                 result)))))))

(defn proportional-bar
  "Where `values` is a finite sequence of numbers, and `colours` (if passed)
   is a sequence either of valid SVG colour names or else of RGB hexcodes as 
   strings, return an SVG graph illustrating the proportions between the
   values."
  ([values]
   (proportional-bar values 500 30))
  ([values colours]
   (proportional-bar values 500 30 colours))
  ([values width height]
   (proportional-bar values width height named-colours))
  ([values width height colours]
   [:svg
    {:version "1.1"
     :baseProfile "full"
     :width width :height height
     :xmlns "http://www.w3.org/2000/svg"
     :style "position: absolute; left:0; top:0; width:100%; height:100% "}
    (apply
     vector
     (concat [:g]
             (graph-segments values width height colours)))]))

