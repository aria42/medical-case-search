package com.pragmaticideal.casesearch.api

import java.nio.file.Paths

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.{DirectoryReader}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{IndexSearcher}
import org.apache.lucene.store.FSDirectory
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.ScalatraServlet
import org.scalatra.json._
import org.scalatra.scalate.ScalateSupport

case class APIConfig(val luceneDir: String)

class CaseSearchAPIServlet extends ScalatraServlet with JacksonJsonSupport {

  implicit val jsonFormats: Formats = DefaultFormats

  val idxSearcher: IndexSearcher = {
    val jsonConfigPath = sys.env("server-config.json")
    require(jsonConfigPath != null,
      "must pass environmental variable 'server-config.json' to initialize service resource")
    val configStream = getClass.getResourceAsStream(jsonConfigPath)
    val json = scala.io.Source.fromInputStream(configStream).mkString
    val config = parse(json).extract[APIConfig]
    val idxReader = DirectoryReader.open(FSDirectory.open(Paths.get(config.luceneDir)))
    new IndexSearcher(idxReader)
  }

  val analyzer: Analyzer = new StandardAnalyzer()

  before() {
    contentType = formats("json")
  }

  case class ResultDoc(title: String)

  get("/search/:query") {
    params.get("query") match {
      case Some(inputQuery) =>
        // TODO(aria42) This just does a simple title search
        val query = new QueryParser("title", analyzer).parse(inputQuery)
        for  (searchHit <- idxSearcher.search(query, 1000).scoreDocs.toSeq) yield {
          val doc = idxSearcher.doc(searchHit.doc)
          ResultDoc(doc.get("title"))
        }
      case None =>
    }

  }
}

class WebappServlet extends ScalatraServlet with ScalateSupport {

  before() {
    contentType="text/html"
  }

  get("/") {
    ssp("/template/views/index.ssp")
  }
}