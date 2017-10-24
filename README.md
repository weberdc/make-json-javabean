# Make JSON JavaBean

Author: **Derek Weber**

Last updated: **2017-10-24**

Simple app to take a TSV of property names and types (and optional corresponding
field names) and turn them into a JavaBean class, avoiding the mind-numbing task
of creating the class yourself.

Requirements:
 + Java Development Kit 1.8
 + [FasterXML](http://wiki.fasterxml.com/JacksonHome) (Apache 2.0 licence)
 + [jcommander](http://jcommander.org) (Apache 2.0 licence)

Built with [Gradle 4.2](http://gradle.org).

## To Build

The Gradle wrapper has been included, so it's not necessary for you to have Gradle
installed - it will install itself as part of the build process. All that is required is
the Java Development Kit.

By running

`$ ./gradlew installDist` or `$ gradlew.bat installDist`

you will create an installable copy of the app in `PROJECT_ROOT/build/make-json-javabean`.

Use the target `distZip` to make a distribution in `PROJECT_ROOT/build/distributions`.

## Usage
If you've just downloaded the binary distribution, do this from within the unzipped
archive (i.e. in the `make-json-javabean` directory). Otherwise, if you've just built
the app from source, do this from within `PROJECT_ROOT/build/install/make-json-javabean`:
<pre>
Usage: bin/make-json-javabean[.bat] [options]
  Options:
    -f, --fields-file
      File of field information
    -c, --fqclassname
      Fully qualified name of class to generate
      Default: org.dcw.FooBar
    -g, --getters
      Generate getters (default: false)
      Default: false
    -h, -?, --help
      Help (default: false)
      Default: false
    -j, --javadoc
      Generate javadoc (default: false)
      Default: false
    -s, --setters
      Generate setters (default: false)
      Default: false
</pre>

The input file should be similar to the one included: `data/test1.tsv`. Each line
corresponds to a different property and consists of the JSON property name, a Java
type for the property's value (this will be used in the generated source, so use
a valid Java type), and, optionally, a field name to be used in place of the JSON
property name, in case it clashes with a known Java keyword (e.g., Twitter user
objects have a field `protected`, which clashes with the corresponding Java
keyword). Output is written to `stdout`, so redirect it to a file as desired.

It is likely that some further editing may be required, but it'll save you a lot
of typing.

Enjoy.
