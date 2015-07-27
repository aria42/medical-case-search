import com.pragmaticideal.casesearch.api.{WebappServlet, CaseSearchAPIServlet}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext

object JettyLauncher { // this is my entry object as specified in sbt project definition
  def main(args: Array[String]) {
    val port = sys.env.get("PORT").map(_.toInt).getOrElse(8081)
    // Setup context
    val context = new WebAppContext()
    context.setContextPath("/")
    context.setResourceBase("src/main/webapp/WEB-INF")
    context.addServlet(classOf[CaseSearchAPIServlet], "/api/0.1/*")
    context.addServlet(classOf[WebappServlet], "/app/*")
    context.setParentLoaderPriority(true)
    // Start server and join
    val server = new Server(port)
    server.setHandler(context)
    server.start
    server.join
  }
}