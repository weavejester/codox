# Codox

A tool for generating API documentation from Clojure source code.

## Usage

Include the following development dependency in your `project.clj` file:

    [codox "0.3.1"]

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

Each of these keywords can be used together, of course.
