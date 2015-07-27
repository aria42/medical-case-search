package com.pragmaticideal.casesearch

import java.io._
import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveOutputStream}
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

  "Tar entry iterator" should "provide an entry over file entries" in {
    val tarDir = File.createTempFile("iotest", "tar")
    val tarOutputStream = new TarArchiveOutputStream(new FileOutputStream(tarDir))
    def writeTempFile(content: String): File = {
      val tmpFile = File.createTempFile("iotest-file","txt")
      tmpFile.deleteOnExit()
      val fileWriter = new FileWriter(tmpFile)
      fileWriter.write(content)
      fileWriter.close()
      tmpFile
    }
    tarDir.deleteOnExit()
    val expectedEntries = Map("a/b.txt" ->  "some a text", "b.txt" -> "some other text")
    for ((entryPath, txt) <- expectedEntries) {
      val file = writeTempFile(txt)
      tarOutputStream.putArchiveEntry(new TarArchiveEntry(file, entryPath))
      val fileBytes = scala.io.Source.fromFile(file).mkString.getBytes
      tarOutputStream.write(fileBytes)
      tarOutputStream.closeArchiveEntry()
      tarOutputStream.flush()
    }
    tarOutputStream.close()
    val tarEntries: Map[String, String] = IO.tarEntries(new FileInputStream(tarDir))
      .map {
        case IO.TarEntry(pathName, bytes) =>
          pathName -> new String(bytes)
      }.toMap
    tarEntries shouldBe expectedEntries
  }
}
