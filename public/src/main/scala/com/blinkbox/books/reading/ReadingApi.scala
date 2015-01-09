package com.blinkbox.books.reading

import akka.actor.ActorRefFactory
import com.blinkbox.books.auth.Elevation.Unelevated
import com.blinkbox.books.auth.User
import com.blinkbox.books.clients.catalogue.LibraryItemConflictException
import com.blinkbox.books.config.ApiConfig
import com.blinkbox.books.spray.Directives.{paged, rootPath}
import com.blinkbox.books.spray.MonitoringDirectives.monitor
import com.blinkbox.books.spray.v2.Implicits.throwableMarshaller
import com.blinkbox.books.spray.v2.RejectionHandler.ErrorRejectionHandler
import com.blinkbox.books.spray.v2.Relation
import com.blinkbox.books.spray.{ElevatedContextAuthenticator, JsonFormats, url2uri, v2}
import com.typesafe.scalalogging.StrictLogging
import spray.http.StatusCodes._
import spray.http.{IllegalRequestException, StatusCodes}
import spray.routing._

import scala.concurrent.ExecutionContext.Implicits.global

class ReadingApi(
  apiConfig: ApiConfig,
  authenticator: ElevatedContextAuthenticator[User],
  libraryService: LibraryService)(implicit val actorRefFactory: ActorRefFactory) extends HttpService with v2.JsonSupport with StrictLogging {

  import ReadingApi._

  val defaultPageSize = 25
  implicit override val jsonFormats = JsonFormats.blinkboxFormat() ++ bookDetailsSerializers

  val getLibrary = get {
    path("my" / "library") {
      authenticate(authenticator.withElevation(Unelevated)) { implicit user =>
        paged(defaultPageSize) { page =>
          onSuccess(libraryService.getLibrary(page.count, page.offset)) { res =>
            val items = Map("items" -> res)
            complete(OK, items)
          }
        }
      }
    }
  }

  val handleSamples = path("my" / "library" / "samples") {
    get {
      authenticate(authenticator.withElevation(Unelevated)) { implicit user =>
        paged(defaultPageSize) { page =>
          onSuccess(libraryService.getSamples(page.count, page.offset)) { res =>
            val items = Map("items" -> res)
            complete(OK, items)
          }
        }
      }
    } ~
    post {
      authenticate(authenticator.withElevation(Unelevated)) { implicit user =>
        entity(as[LibraryItemIsbn]) { req =>
          validate(Isbn.pattern.matcher(req.isbn).matches, "Isbn must be 13 digits long and start with the number 9") {
            onSuccess(libraryService.addSample(req.isbn)) {
              case SampleAlreadyExists => complete(OK)
              case SampleAdded => complete(StatusCodes.Created)
            }
          }
        }
      }
    }
  }

  val getBookDetails = rejectEmptyResponse {
    get {
      path("my" / "library" / Isbn) { isbn =>
        authenticate(authenticator.withElevation(Unelevated)) { implicit user =>
          onSuccess(libraryService.getBook(isbn)) { res =>
            complete(res)
          }
        }
      }
    }
  }

  val routes = monitor(logger, throwableMarshaller) {
    handleExceptions(exceptionHandler) {
      handleRejections(ErrorRejectionHandler) {
        rootPath(apiConfig.localUrl.path) {
          getBookDetails ~ getLibrary ~ handleSamples
        }
      }
    }
  }

  private lazy val exceptionHandler = ExceptionHandler {
    case e: LibraryItemConflictException => failWith(new IllegalRequestException(Conflict, e.getMessage))
  }
}

object ReadingApi {
  import org.json4s.JsonDSL._
  import org.json4s._

  /** Matcher for ISBN. */
  val Isbn = """^([0-9]{13})$""".r

  object ReadingPositionSerializer extends CustomSerializer[ReadingPosition](format =>
    ({
      case JObject(List((cfi, JString(cfiPos)), (percentage, JInt(percentagePos)))) =>
        ReadingPosition(Some(Cfi(cfiPos)), percentagePos.intValue())
      case JObject(List((percentage, JInt(percentagePos)))) =>
        ReadingPosition(None, percentagePos.intValue())
    }, {
      case ReadingPosition(Some(Cfi(cfi)), position) => ("cfi" -> cfi) ~ ("percentage" -> position)
      case ReadingPosition(None, position) => "percentage" -> position
    })
  )

  object MediaTypeSerializer extends CustomSerializer[Relation](_ => ({
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

  val bookDetailsSerializers = List(ReadingPositionSerializer, MediaTypeSerializer, OwnershipSerializer, ReadingStatusSerializer)

  case class LibraryItemIsbn(isbn: String)
}
