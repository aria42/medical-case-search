package com.pragmaticideal.casesearch.offline

import java.io.{FileInputStream, InputStreamReader, File}
import java.nio.file.Paths
import java.util.concurrent.{TimeUnit, Executors}
import java.util.zip.GZIPInputStream
import com.pragmaticideal.casesearch.Model.{ResultDoc, ResearchArticle}
import com.pragmaticideal.casesearch.IO
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.store._

import org.apache.lucene.document.{TextField, Document, Field}
import org.apache.lucene.index.{IndexWriterConfig, IndexWriter}

import scala.io.Source

object LuceneIndexer {

  case class Config(
    fileList: File = null,
    outputIndex: File = null,
    numThreads: Int = 1
  )

  def copyAll(from: Directory, to: Directory) {
    for (path <- from.listAll()) {
      to.copyFrom(from, path, path, IOContext.DEFAULT)
    }
  }

  def indexDoc(article: ResearchArticle): Document = {
    val doc = new Document()
    doc.add(new Field("title", article.title, TextField.TYPE_STORED))
    doc.add(new Field("journalTitle", article.journalTitle, TextField.TYPE_STORED))
    doc.add(new Field("year", article.pubYear.toString, TextField.TYPE_STORED))
    for (author <- article.author) {
      doc.add(new Field("author", s"${author.firstName} ${author.lastName}", TextField.TYPE_STORED))
    }
    for (phrase <- article.keyPhrases) {
      doc.add(new Field("keyPhrase", phrase, TextField.TYPE_STORED))
    }
    for (section <- article.abstractSections) {
      doc.add(new Field(s"${ResultDoc.abstractSectionPrefix}${section.title}", section.text, TextField.TYPE_STORED))
    }
    doc
  }

  def idxConfig = new IndexWriterConfig(new StandardAnalyzer())

  class IndexWorker(val workerId: Int, val dataFiles: Seq[File]) extends Runnable {

    val ramDir = new RAMDirectory()

    override def run(): Unit = {
      println(s"Indexing ${dataFiles.length} files")
      val idxWriter = new IndexWriter(ramDir, idxConfig)
      val allArticles = dataFiles.iterator.flatMap { file =>
        val reader = new InputStreamReader(new GZIPInputStream(new FileInputStream(file)))
        IO.readJsonValues[ResearchArticle](reader)
      }
      for (article <- allArticles) {
        val doc = indexDoc(article)
        idxWriter.addDocument(doc)
        if (idxWriter.numDocs() % 1000  == 0) {
          println(s"Worker $workerId indexed ${idxWriter.numDocs()} docs")
        }
      }
      idxWriter.close()
    }
  }

  def run(config: Config): Unit = {
    // Split work across input files
    val allFiles = Source.fromFile(config.fileList).getLines.map(new java.io.File(_)).toList
    val groupSize = allFiles.length / config.numThreads
    val groups = allFiles.grouped(groupSize).toList
    val workers = groups.zipWithIndex.map { case (group, idx) => new IndexWorker(idx, group) }
    // Create pool of works to index in their own ram directory
    val pool = Executors.newFixedThreadPool(config.numThreads)
    val futures = workers.map(pool.submit)
    futures.foreach(_.get())
    pool.shutdownNow()
    // Once down merge indices into a single index
    // this requires 2x mem but is faster than merge to disk
    // in special case of one thread, no extra memory
    val memDirs = workers.map(_.ramDir).toArray
    val masterMemDir = if (workers.size > 1) {
      val mergedDir = new RAMDirectory()
      val mergedIdxWriter = new IndexWriter(mergedDir, idxConfig)
      mergedIdxWriter.addIndexes(memDirs: _*)
      mergedIdxWriter.close()
      mergedDir
    } else {
      memDirs(0)
    }
    // Copy the merged mem index to disk in one scan
    val fsDir = FSDirectory.open(Paths.get(config.outputIndex.getAbsolutePath))
    copyAll(masterMemDir, fsDir)
    System.exit(0)
  }

  def main(args: Array[String]) {
    val parser = new scopt.OptionParser[Config]("lucene-indexer") {
      opt[File]('i', "inputFiles")
        .required()
        .action((fileList, c) => c.copy(fileList = fileList))
        .text("File containing one-path-per-line")
      opt[File]('o', "outputIndex")
        .required()
        .action((o, c) => c.copy(outputIndex = o))
        .validate(f => {
          if (f.exists()) failure(s"Directory ${f.getAbsolutePath} already exists")
          else if (!f.mkdirs()) failure(s"Couldn't create directory ${f.getAbsolutePath}")
          else success
        })
    }
    parser.parse(args, Config()).foreach(run)
  }

}
