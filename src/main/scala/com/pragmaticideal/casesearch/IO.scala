package com.pragmaticideal.casesearch

import java.io._
import java.nio.charset.Charset
import java.util.zip.GZIPInputStream

import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveInputStream}
import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, Extraction, Formats}

import scala.util.control.Breaks

object IO {

  implicit val jsonFormats: Formats = DefaultFormats
  val charset = Charset.defaultCharset()

  def writeJsonValues[T <: scala.AnyRef](writer: Writer, vals: Iterator[T]): Unit = {
    for (v <- vals) {
      val jsonStr = compact(Extraction.decompose(v))
      writer.write(jsonStr)
      writer.write('\n')
    }
  }

  def readJsonValues[T](reader: Reader)(implicit mf: Manifest[T]): Iterator[T] = {
    val br = new BufferedReader(reader)
    def queueNext(): String = br.readLine()
    var queuedLine = queueNext()
    return new Iterator[T] {
      override def hasNext: Boolean = queuedLine != null

      override def next(): T = {
        val elem = parse(queuedLine).extract[T]
        queuedLine = queueNext()
        elem
      }
    }
  }

  /**
   * All files recursiely from input directory
   */
  def allFiles(dir: File, suffix: String = ""): Seq[File] = {
    require(dir.isDirectory)
    for {
      f <- dir.listFiles()
      c <- if (f.isDirectory) allFiles(f) else Seq(f)
      if c.getName.endsWith(suffix)
    } yield c
  }

  case class TarEntry(name: String, data: Array[Byte])

  /**
   * Make an iterator over the Tar file entries in a Gzipped Tar archive
   */
  def tarEntries(inputStream: InputStream): Iterator[TarEntry] = {
    val tarInput = new TarArchiveInputStream(inputStream)
    def nextFileEntry(): TarArchiveEntry = {
      val entry = tarInput.getNextTarEntry
      if (entry != null && entry.isDirectory) nextFileEntry
      else entry
    }
    var entry = nextFileEntry()
    new Iterator[TarEntry] {
      override def hasNext: Boolean = tarInput.getCurrentEntry != null

      override def next(): TarEntry = {
        val size = entry.getSize.toInt
        val buffer = new Array[Byte](size)
        var numTotalRead = 0
        val loop = new Breaks
        loop.breakable {
          while (true) {
            val read = tarInput.read(buffer, numTotalRead, size - numTotalRead)
            if (read < 0) {
              loop.break
            }
            numTotalRead += read
          }
        }
        require(numTotalRead == size, s"Entry not size promised $numTotalRead != $size")
        val ret = TarEntry(entry.getName, buffer)
        entry = nextFileEntry()
        ret
      }
    }
  }
}