
import java.io.File
import java.nio.file.Paths

import com.pragmaticideal.casesearch.api.CaseSearchAPIServlet
import org.apache.lucene.index.{DirectoryReader, IndexReader}
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.FSDirectory
import org.scalatra._
import javax.servlet.ServletContext

import org.json4s._
import org.json4s.DefaultFormats._
import org.json4s.jackson._

class ScalatraBootstrap extends LifeCycle {

  implicit val formats: Formats = DefaultFormats

  override def init(context: ServletContext) {
    context.mount(new CaseSearchAPIServlet, "/api/0.1/*")
  }
}
