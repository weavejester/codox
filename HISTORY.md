## 0.7.1 (2014-05-04)

* Fixed URI exception for certain var names
* No longer includes proxy classes in docs
* Updated tools.namespace dependency

## 0.7.0 (2014-05-03)

* ClojureScript support
* Leiningen `:source-paths` respected
* URLs in docstrings turned into links
* Fixed links to vars with non-alphanumeric characters

## 0.6.8 (2014-04-27)

* Show `:added` and `:deprecated` metadata in docs
* Shorten namespace summaries to first line on index page

## 0.6.7 (2014-02-15)

* Display cause of errors when namespace cannot be loaded for docs

## 0.6.6 (2013-09-18)

* Fixed bug with 0.6.5 not working with projects that use old Clojure
  versions

## 0.6.5 (2013-09-18)

* Support for unicode characters in docs
* `:src-dir-uri` option now requires an explict ending '/'
* Public vars without docstrings now included in output. Use
  `^:no-doc` to remove them from documentation.
