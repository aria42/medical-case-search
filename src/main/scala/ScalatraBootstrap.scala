
import com.pragmaticideal.casesearch.api.CaseSearchAPIServlet
import org.scalatra._
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context.mount(new CaseSearchAPIServlet, "/api/0.1/*")
  }
}
