package com.pragmaticideal.casesearch

import java.io.{FileInputStream, InputStream, File}

import scala.pickling.Defaults._
import scala.pickling.binary._
import scala.util.Try

package object model {

  class ResearchArticle(
    val journalTitle: String,
    val pmcId: String,
    val title: String,
    val pubYear: Int,
    val author: List[Author],
    val abstractSections: List[AbstractSection],
    val keyPhrases: List[String] = List.empty
  )
  {
    def toBytes: Array[Byte] = this.pickle.value
  }

  case class AbstractSection(title: String, text: String)

  case class Author(
    firstName: String,
    lastName: String
  )

  object ResearchArticle {
    def fromInputStream(inputStream: InputStream): Iterator[ResearchArticle] = {
      val pickle = BinaryPickle(inputStream)
      Iterator
        .continually(Try(pickle.unpickle[ResearchArticle]).toOption)
        .takeWhile(_.isDefined)
        .map(_.get)
    }

    def fromFile(f: File): Iterator[ResearchArticle] = {
      fromInputStream(new FileInputStream(f))
    }
  }

}
