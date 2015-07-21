package com.pragmaticideal.casesearch.api

import org.json4s.{JValue, DefaultFormats, Formats}
import org.scalatra.ScalatraServlet
import org.scalatra.json._

class CaseSearchAPIServlet extends ScalatraServlet with JacksonJsonSupport {

  implicit val jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
  }

  get("/search") {
    List(1,2,3,4,5,7,8, 9,10)
  }
}
