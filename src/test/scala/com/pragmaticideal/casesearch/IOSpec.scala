package com.pragmaticideal.casesearch

import java.io._
import org.scalatest.{Matchers, FlatSpec}

object DAO {
  case class Person(val fname: String, val lname: String)
}

class IOSpec extends FlatSpec with Matchers {
  "JSON read/write" should "roundtrip data" in {
    val data = List(DAO.Person("a","b"), DAO.Person("c", "d"), DAO.Person("e","f"))
    val f = File.createTempFile("person","json")
    f.deleteOnExit()
    val os = new FileWriter(f)
    IO.writeJsonValues(os, data.iterator)
    os.close()
    val is = new FileReader(f)
    val read = IO.readJsonValues[DAO.Person](is).toList
    is.close()
    read shouldBe data
  }
}
