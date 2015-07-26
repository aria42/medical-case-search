package com.pragmaticideal.casesearch

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

  case class AbstractSection(title: String, text: String)

  case class Author(
    firstName: String,
    lastName: String
  )
}
