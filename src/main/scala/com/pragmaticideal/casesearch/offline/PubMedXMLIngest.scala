package com.pragmaticideal.casesearch.offline

import java.io.{InputStreamReader, FileInputStream, FileOutputStream, File}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}
import com.pragmaticideal.casesearch.model.{ResearchArticle, AbstractSection, Author}
import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveInputStream}

import scala.util.control.Breaks
import scala.xml.{Elem, XML, Node}

/**
 * This class is meant to go from the XML PubMed open-access subnet
 * to compact internal representation binary, suitable for either batch jobs or indexing.
 * It's about 100x smaller than the raw XML and only stores, what we currently 'think' we need.
 * The core input is assumed to be article archives
 */
object PubMedXMLIngest {

  val suffix = ".nxml"
  val docTypeRegex = "<!DOCTYPE.*?>"

  def loadXML(source: scala.io.Source): Elem = {
    val sml = source.mkString.replaceAll(docTypeRegex,"")
    XML.loadString(sml)
  }

  def allFiles(dir: File): Seq[File] = {
    require(dir.isDirectory)
    for {
      f <- dir.listFiles()
      c <- if (f.isDirectory) allFiles(f) else Seq(f)
      if c.getName.endsWith(suffix)
    } yield c
  }

  def time[R](msg: String)(block: => R): R = {
    val t0 = System.currentTimeMillis()
    val result = block
    val t1 = System.currentTimeMillis()
    println(s"$msg Elapsed time: " + (t1 - t0) + "ms")
    result
  }

  def cascadedSearch[T](xs: Seq[T], preds: Seq[T => Boolean]): Option[T] = {
    for (p <- preds; x <- xs) {
      if (p(x)) {
        return Some(x)
      }
    }
    None
  }

  def hasAttributeFn(key: String, `val`: String): Node => Boolean = e => e \@ key == `val`

  def fromXML(elem: Elem): ResearchArticle = {
    val journalTitle: String = (elem \\ "journal-title").head.text
    val pmcId = (elem \\ "article-meta" \\ "article-id")
      .find(_ \@ "pub-id-type" == "pmc")
      .get.text
    val articleTitle = (elem \\ "article-title").head.text
    val pubElem = cascadedSearch((elem \\ "pub-date"),
      Seq("ppub", "collection", "epub").map(hasAttributeFn("pub-type", _)) ++
        Seq("print", "electronic").map(hasAttributeFn("publication-format", _))
    ).get
    val year = (pubElem \ "year").text.trim.toInt
    val authors = (elem \\ "contrib-group" \\ "contrib" \\ "name")
      .map(e => Author((e \ "surname").text, (e \ "given-names").text))
    val abstractSections = for {
      sec <- elem \\ "abstract" \\ "sec"
      sectionTitle = (sec \ "title").text.replaceAll(":\\s*$","")
      sectionText = (sec \ "p").map(_.text).mkString("\n")
    } yield AbstractSection(sectionTitle, sectionText)
    val keyPhrases = (elem \\ "kwd").map(_.text)
    new ResearchArticle(journalTitle, pmcId, articleTitle, year, authors.toList,
      abstractSections.toList, keyPhrases.toList)
  }

  def gzipTarFile(path: String): Iterator[String] = {
    val gzipInput = new GZIPInputStream(new FileInputStream(new File(path)))
    val tarInput = new TarArchiveInputStream(gzipInput)
    def nextFileEntry(): TarArchiveEntry = {
      val entry = tarInput.getNextTarEntry
      if (entry != null && entry.isDirectory) nextFileEntry
      else entry
    }
    var entry = nextFileEntry()
    new Iterator[String] {
      override def hasNext: Boolean = tarInput.getCurrentEntry != null

      override def next(): String = {
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
        entry = nextFileEntry()
        new String(buffer)
      }
    }
  }

  case class Opts(
    val inputPaths: Seq[File] = Seq(),
    val outputDir: File = null,
    val batchSize: Int = 5000
  )

  def run(opts: Opts): Unit = {
    val it = opts.inputPaths.iterator.flatMap(f => gzipTarFile(f.getAbsolutePath))
    for ((group, cnt) <- it.grouped(opts.batchSize).zipWithIndex) {
      val outPath = new File(opts.outputDir, s"articles-$cnt.binary.gz")
      val gzipOut = new GZIPOutputStream(new FileOutputStream(outPath))
      val articles = time("Read XML and extract") {
        group.seq.par.map { xmlStr =>
          val xml = loadXML(scala.io.Source.fromString(xmlStr))
          if (xml \@ "article-type" == "research-article") {
            try {
              Some(fromXML(xml))
            } catch {
              case e: Exception =>
                println("Bad XML, skppping")
                None
            }
          } else {
            None
          }
        }.seq.filter(_.isDefined).map(_.get)
      }
      var numWritten = 0
      time("Writing articles") {
        for (a <- articles) {
          try {
            gzipOut.write(a.toBytes)
            numWritten += 1
          } catch {
            case t: Throwable =>
              println("Error serializing article")
          }
        }
      }
      gzipOut.close()
      println(s"Done with $outPath")
    }
  }

  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[Opts]("pubmed-ingest") {
      opt[Seq[File]]('i', "input")
        .required()
        .valueName("<f1>, <f2>,...")
        .text("List of input pubmed .tar.gz xml files")
        .action((fs, opts) => opts.copy(inputPaths = fs))
        .validate { fs =>
          if (fs.forall(f => f.exists &&  f.isFile)) success
          else failure("Not all input files exists")
        }
      opt[File]('o', "output")
        .required()
        .text("Output directory to write part files")
        .action((f, opts) => {
          f.mkdirs()
          opts.copy(outputDir = f)
        })
        .validate { f =>
        if (!f.exists ||  f.isDirectory) success
        else failure("Output is a pre-existing file (not a directory)")
      }
      opt[Int]('b', "batch-size")
        .action((batchSize, opts) => opts.copy(batchSize = batchSize))
        .text("How big each output file should be")
        .validate { batchSize =>
          if (batchSize > 0) success
          else failure("batch-size needs to be positive")
      }
    }
    parser.parse(args, Opts()).foreach(run)
  }
}
