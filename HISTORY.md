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
