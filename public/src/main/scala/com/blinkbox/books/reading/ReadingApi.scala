package com.blinkbox.books.reading

import akka.actor.ActorRefFactory
import com.blinkbox.books.auth.User
import com.blinkbox.books.auth.Elevation.Unelevated
import com.blinkbox.books.config.ApiConfig
import com.blinkbox.books.reading.common.{CFI, ReadingPosition}
import com.blinkbox.books.spray.Directives.rootPath
import com.blinkbox.books.spray.MonitoringDirectives.monitor
import com.blinkbox.books.spray.v2.JsonFormats
import com.blinkbox.books.spray.{v2, ElevatedContextAuthenticator, url2uri}
import org.slf4j.LoggerFactory
import spray.http.StatusCodes._
import spray.routing._

import scala.concurrent.ExecutionContext.Implicits.global

class ReadingApi(
  apiConfig: ApiConfig,
  authenticator: ElevatedContextAuthenticator[User],
  libraryService: LibraryService)(implicit val actorRefFactory: ActorRefFactory) extends HttpService with v2.JsonSupport {

  import ReadingApi._

  val log = LoggerFactory.getLogger(classOf[ReadingApi])

  implicit override val jsonFormats = JsonFormats.blinkboxFormat() + ReadingPositionSerializer

  val getBookDetails = get {
    path("my" / "library" / Isbn) { isbn =>
      authenticate(authenticator.withElevation(Unelevated)) { user =>
        onSuccess(libraryService.getBook(isbn, user.id)) { res =>
          complete(OK, res)
        }
      }
    }
  }

  val routes = monitor(log) {
    rootPath(apiConfig.localUrl.path) {
      getBookDetails
    }
  }
}

object ReadingApi {
  import org.json4s._
  import org.json4s.JsonDSL._

  /** Matcher for ISBN. */
  val Isbn = """^(\d{13})$""".r

  object ReadingPositionSerializer extends CustomSerializer[ReadingPosition](format =>
    (PartialFunction.empty, {
      case ReadingPosition(CFI(cfi), position) => ("cfi" -> cfi) ~ ("percentage" -> position)
    })
  )
}
