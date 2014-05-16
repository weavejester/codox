(ns codox.markdown
  "This is an example of a namespace written in markdown, rather than the usual
  plaintext format.

  We're going to cover all the standard formatting, like *italics*, **bolds**,
  `:inline-code` ~~strikethrough~~ and \"smart quotes\" -- so we can check the
  styling in the docs.

  - Let's check
  - unordered
  - lists

  1. And let's also check
  2. ordered
  2. lists

  We should also check code block formatting:

      (defn foo [x]
        (+ x 1))

  And extensions like auto-linked URLs (http://example.com) and tabular data:

  foo | bar
  ----|----
   1  |  2 
   3  |  4

  We can also use wikilinks to reference existing vars, like [[example/foo]] or
  the [[Foop]] protocol."
  {:doc/format :markdown})


(defn some-function
  "Some function defined in the namespace."
  {:doc/format :markdown}
  [x])
