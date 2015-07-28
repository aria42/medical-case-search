package com.pragmaticideal.casesearch

import scala.collection.JavaConversions._
import org.apache.lucene.document.Document

object Model {
  class ResearchArticle(
    val journalTitle: String,
    val pmcId: String,
    val title: String,
    val pubYear: Int,
    val author: List[Author],
    val abstractSections: List[AbstractSection],
    val keyPhrases: List[String] = List.empty
  )

  case class AbstractSection(title: String, text: String)

  case class Author(firstName: String, lastName: String)

  case class ResultSnippet(
      title: String,
      authors: List[String],
      snippet: String,
      journalTitle: String,
      year: Int
  )

  object ResultSnippet {
    def fromResultDoc(doc: ResultDoc): ResultSnippet = {
      val introTxt = doc.abstractSections.headOption.map(_.text).getOrElse("")
      val snippet = introTxt.substring(0, Math.min(introTxt.length, 100))
      ResultSnippet(doc.title, doc.authors, snippet, doc.journalTitle, doc.year)
    }
    def fromLuceneDoc(doc: Document): ResultSnippet = {
      fromResultDoc(ResultDoc.fromLuceneDoc(doc))
    }
  }

  case class ResultDoc(title: String,
                       abstractSections: List[AbstractSection],
                       authors: List[String],
                       journalTitle: String,
                       year: Int)

  object ResultDoc {

    val abstractSectionPrefix = "abstract-section-"

    def fromLuceneDoc(doc: Document): ResultDoc = {
      val authors = doc.getFields("author").map(_.stringValue()).toList
      val journalTitle = doc.get("journalTitle")
      val pubYear = doc.get("year").toInt
      val abstractSections = for {
        f <- doc.getFields.toList
        if (f.name().startsWith(abstractSectionPrefix))
        sectionTitle = f.name().substring(abstractSectionPrefix.length)
      } yield AbstractSection(sectionTitle, f.stringValue())
      ResultDoc(doc.get("title"), abstractSections, authors, journalTitle, pubYear)
    }
  }
}
