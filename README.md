# Codox

A tool for generating API documentation from Clojure source code.

## Usage

Include the following development dependency in your `project.clj` file:

    [codox "0.4.1"]

Then run:

    lein doc

This will generate API documentation in the "doc" subdirectory.

## Options

By default Codox looks for source files in the `src` subdirectory, but
you can change this by placing the following in your `project.clj`
file:

```clojure
:codox {:sources ["path/to/source"]}
```

To exclude a namespace, use the `:exclude` key:

```clojure
:codox {:exclude my.private.ns}
```

Sequences work too:

```clojure
:codox {:exclude [my.private.ns another.private.ns]
```

To include only one or more namespaces, set them with the `:include` key:

```clojure
;; Again, a single symbol or a collection are both valid
:codox {:include library.core}
:codox {:include [library.core library.io]}
```

To write output to a directory other than the default `doc` directory, use the
`:output-dir` key:

```clojure
:codox {:output-dir "doc/codox"}
```

To use a different output writer, specify the fully qualified symbol of the
writer function in the `:writer` key:

```clojure
:codox {:writer codox.writer.html/write-docs}
```

Each of these keywords can be used together, of course.

### Skipping Individual Functions

To force codox to skip an individual public var, add `:no-doc true` to the var's metadata. For example,

```clojure
;; square show up in codox...
(defn square
  "Squares the supplied number."
  [x])

;; but hidden-square won't.
(defn hidden-square
  "Squares the supplied number."
  {:no-doc true}
  [x]
  (* x x))
```
