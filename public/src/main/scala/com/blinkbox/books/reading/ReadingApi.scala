package com.blinkbox.books.reading

import akka.actor.ActorRefFactory
import com.blinkbox.books.auth.Elevation.Unelevated
import com.blinkbox.books.auth.User
import com.blinkbox.books.config.ApiConfig
import com.blinkbox.books.reading.common._
import com.blinkbox.books.spray.Directives.rootPath
import com.blinkbox.books.spray.MonitoringDirectives.monitor
import com.blinkbox.books.spray.{ElevatedContextAuthenticator, JsonFormats, url2uri, v2}
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
  implicit override val jsonFormats = JsonFormats.blinkboxFormat() + ReadingPositionSerializer + MediaTypeSerializer + BookTypeSerializer + ReadingStatusSerializer + BookDetailsSerializer

  val getBookDetails = get {
    path("my" / "library" / Isbn) { isbn =>
      authenticate(authenticator.withElevation(Unelevated)) { user =>
        onSuccess(libraryService.getBook(isbn, user.id)) { res =>
          complete(res)
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
  import org.json4s.FieldSerializer._

  /** Matcher for ISBN. */
  val Isbn = """^(\d{13})$""".r

  object ReadingPositionSerializer extends CustomSerializer[ReadingPosition](format =>
    (PartialFunction.empty, {
      case ReadingPosition(Cfi(cfi), position) => ("cfi" -> cfi) ~ ("percentage" -> position)
    })
  )

  object MediaTypeSerializer extends CustomSerializer[LinkType](_ => ({
    case JString("CoverImage") => CoverImage
    case JString("EpubSample") => SampleEpub
    case JString("EpubFull") => FullEpub
    case JString("EpubKey") => EpubKey
    case JNull => null
  }, {
    case CoverImage => JString("CoverImage")
    case SampleEpub => JString("EpubSample")
    case FullEpub => JString("EpubFull")
    case EpubKey => JString("EpubKey")
  }))

  object BookTypeSerializer extends CustomSerializer[BookType](_ => ({
    case JString("Full") => Full
    case JString("Sample") => Sample
  }, {
    case Full => JString("Full")
    case Sample => JString("Sample")
  }))

  object ReadingStatusSerializer extends CustomSerializer[ReadingStatus](_ => ({
      case JString("NotStarted") => NotStarted
      case JString("Reading") => Reading
      case JString("Finished") => Finished
    },
    {
      case NotStarted => JString("NotStarted")
      case Reading => JString("Reading")
      case Finished => JString("Finished")
    }))

  val BookDetailsSerializer = FieldSerializer[BookDetails](
    renameTo("createdAt", "addedDate"),
    renameFrom("addedDate", "createdAt")
  )
}
