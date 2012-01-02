# Codox

A tool for generating API documentation from Clojure source code.

## Usage

Include the following development dependency in your `project.clj` file:

    [codox "0.3.2"]

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

## Deploying Codox to Github Pages

The [Github Pages](http://pages.github.com/) feature is an excellent way to share codox documentation with your users. Get started with the following steps:

1. Add `doc` to your project's `.gitignore` file.
2. Inside your project directory, run the following commands:

```bash
rm -rf doc && mkdir doc
git clone git@github.com:<user-name>/<project-name>.git doc
cd doc
git symbolic-ref HEAD refs/heads/gh-pages
rm .git/index
git clean -fdx
cd ..
```

3. Build your documentation with `lein doc`.
4. To publish your docs to Github Pages, run the following commands:

```bash
cd doc
git checkout gh-pages # To be sure you're on the right branch
git add .
git commit -am "new documentation push."
git push -u origin gh-pages
cd ..
```

That's it! Your documentation should appear within minutes at `http://<user-name>.github.com/<project-name>`.
