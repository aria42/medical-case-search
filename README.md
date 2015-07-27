# Technology Choices

* [Scalatra](http://www.scalatra.org): The Scala server is Scalatra; broadly similar to Sinatra in ruby land. It's not a large framework like Rails, but is reasonable out of the box. It can either be used in container Java WebAps or in standalone server mode.
* [Typescript](http://www.typescriptlang.org): The client code is Typescript, typed Javascript transpiles to Javascript. It's the language that Angular 2.0 is written in and has good IDE/Editor support.
    
# Entry point
    
* Server: Easiest way to start a server is via `JettyLauncher` which does a stand-alone Jetty server on port 8081. You need to pass in an environmental variable for `server-config.json` which maps to the `APIConfig` class and has information about where the Lucene index is and other future knobs. 
* Client: Install the typescript compiler (`npm -g install tsc`). Use `tsc -w` to launch the TypeScript watching compiler in `/src/main/webapp/typescript`.
  
# Offline Indexing Pipeline

* `PubMedXMLIngest` takes the FTP dump of PubMed open articles and creates JSON representations of the sub-set we care about
* `LuceneIndexer` creates a Lucene index
 
# Getting Started With Data
 
* In Dropbox, I have a pre-built [Lucene index](not-yet) over the PubMed 

# Server pieces
 
* `CaseSearchAPIServlet`: Search API
* `WebappServlet`: Webapp (HTML templates, CSS, package JS, etc.)   
 

# Tests

* Server: Run `sbt test`
* Client: None yet. 