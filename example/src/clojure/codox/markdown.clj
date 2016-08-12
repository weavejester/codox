(ns codox.markdown
  "## Markdown - headers, text and inline markup

  This is an example of a namespace written in markdown, rather than the usual
  plaintext format.

  We're going to cover all the standard formatting, like *italics*, **bolds**,
  `:inline-code` ~~strikethrough~~ and \"smart quotes\" -- so we can check the
  styling in the docs.

  ## Lists

  ### Unordered lists

  - Let's check
  - unordered
  - lists

  ### Ordered lists

  1. And let's also check
  2. ordered
  2. lists

  ## Code blocks

  We should also check code block formatting:

      (defn foo [x]
        (+ x 1))

  ## Links

  [Inline links](http://example.com) and [reference links][1].

  [1]: http://example.com

  ## Extensions

  ### URLs

  Extensions like auto-linked URLs (http://example.com) and tabular data:

  ### Tables

  foo | bar
  ----|----
   1  |  2
   3  |  4

  ### Definition lists

  foo
  : a variable

  :bar
  : a keyword definition

  baz
  : a longer description that goes on for several lines, demonstrating that
    terms in a definition list can have definitions that span multiple lines.

  ### Wikilinks

  We can also use wikilinks to reference existing vars, like [[example/foo]] or
  the [[Foop]] protocol, or to reference namespaces, like [[codox.example]].

  ### Abbreviations

  Any abbreviations, like HTML or HTTP, that have corresponding abbreviations,
  will be marked with `<abbr>` tags.

  *[HTML]: Hyper Text Markup Language
  *[HTTP]: Hyper Text Transfer Protocol"
  {:doc/format :markdown
   :deprecated "1.3"})


(defn some-function
  "Some function defined in the namespace."
  {:doc/format :markdown}
  [x])
