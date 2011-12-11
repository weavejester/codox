# Codox

A tool for generating documentation from Clojure source code.

## Usage

Include the following development dependency in your `project.clj` file:

    [codox "0.2.3"]

Then run:

    lein doc

This will generate API documentation in the "doc" subdirectory.

## Options

By default Codox looks for source files in the `src` subdirectory, but
you can change this by placing the following in your `project.clj`
file:

    :codox {:sources ["path/to/source"]}
