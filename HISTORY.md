## 0.9.3 (2016-02-04)

* Tweak styling and design slightly
* Add option for a flat rather than nested namespace menu
* Projects with only one namespace are flat by default

## 0.9.2 (2016-02-03)

* Add `{basename}` parameter to `:source-uri` option
* Add Enlive-style HTML transformations
* Fix an issue with document paths under Windows

## 0.9.1 (2015-12-21)

* Add `{version}` parameter to `:source-uri` option
* Ensure hashtags are not rendered as headers in markdown
* Relax rules for markdown horizontal rules

## 0.9.0 (2015-10-19)

* Added support for including markdown doc files in output
* Changed the top header style to be bigger and flatter
* Renamed `codox.leiningen` package to `lein-codox`
* Renamed `codox.core` package to `codox`
* Renamed `lein doc` task to `lein codox`
* Changed default output path from `doc` to to `target/doc`
* Renamed `:sources` option to `:source-paths`
* Renamed `:output-dir` option to `:output-path`
* Renamed `:defaults` option to `:metadata`
* Replaced the `:include` and `:exclude` options with the `:namespaces` option
* Replaced the `:src-*` options with the `:source-uri` option

## 0.8.15 (2015-10-14)

* Add asm-all as explicit dependency to fix issue with core.async

## 0.8.14 (2015-10-13)

* Added regex support to :include and :exclude options
* Fixed Codox not working with ClojureScript versions due to analyzer API changes
* Updated Pegdown dependency

## 0.8.13 (2015-07-11)

* Updated tools.namespace to support .cljc files in Clojure 1.7

## 0.8.12 (2015-05-02)

* Fixed error caused by CLJ-1242 when documenting vars holding sorted maps

## 0.8.11 (2015-03-08)

* Outputs message when docs are generated
* Fixed imported protocol methods being ignored
* Fixed path separator for source links generated under Windows
* Fixed anonymous vars showing up in ClojureScript docs
* Fixed dependencies showing up in ClojureScript docs

## 0.8.10 (2014-06-30)

* Fixed spacing between vars, add subtle separator line
* Added support for `:added` and `:deprecated` metadata on namespaces

## 0.8.9 (2014-06-05)

* Fixed URLs containing parentheses in plaintext format
* Allow namespaces in wikilink syntax

## 0.8.8 (2014-05-25)

* Added Markdown extensions for definition lists and abbreviations
* Minor CSS fixes

## 0.8.7 (2014-05-22)

* Fixed bug when rendering some Markdown links
* Stopped wrapping of long var names

## 0.8.6 (2014-05-20)

* Fixed disappearing `:arglists` in ClojureScript docs

## 0.8.5 (2014-05-19)

* Minor CSS fixes

## 0.8.4 (2014-05-17)

* Added `:project` map to override project name, version and description
* `:defaults` map now accepts default `:doc` option
* Various minor CSS fixes and improvements

## 0.8.3 (2014-05-16)

* Added wikilink-style links to vars in Markdown docstrings
* Show `:dynamic` metadata in docs
* Vars with invalid file metadata don't cause crashes anymore

## 0.8.2 (2014-05-15)

* Markdown support for var and namespace docstrings

## 0.8.1 (2014-05-14)

* CSS fix for added/deprecated protocol methods
* Added `:no-doc` support for namespaces in Clojure

## 0.8.0 (2014-05-09)

* Hierarchical visualization for protocol functions
* Added type information for multimethods and protocols
* Fixed `:no-doc` not working under ClojureScript
* Improved look of source links

## 0.7.5 (2014-05-08)

* Fixed bug in source links when using absolute source paths

## 0.7.4 (2014-05-08)

* Added `:src-uri-mapping` option
* Cleaner namespace index page

## 0.7.3 (2014-05-07)

* Namespace sidebar made hierarchical
* Various aesthetic tweaks and fixes

## 0.7.2 (2014-05-05)

* Var index automatically scrolls with content
* Links on namespace and public vars headers
* Better sizing of sidebars
* `:codox` profile automatically included when available

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
