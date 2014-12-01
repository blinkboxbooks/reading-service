package com.blinkbox.books.reading.admin

import akka.actor.ActorRefFactory
import com.blinkbox.books.auth.Constraints.hasAnyRole
import com.blinkbox.books.auth.Elevation
import com.blinkbox.books.auth.UserRole._
import com.blinkbox.books.clients.catalogue.CatalogueInfoMissingException
import com.blinkbox.books.config.ApiConfig
import com.blinkbox.books.reading.{LibraryItemConflict, Owned, Ownership, Sample}
import com.blinkbox.books.spray.AuthDirectives.authenticateAndAuthorize
import com.blinkbox.books.spray.Directives.rootPath
import com.blinkbox.books.spray.MonitoringDirectives.monitor
import com.blinkbox.books.spray.v2.Implicits.throwableMarshaller
import com.blinkbox.books.spray.{BearerTokenAuthenticator, JsonFormats, url2uri, v2}
import com.typesafe.scalalogging.StrictLogging
import org.json4s.JsonAST.JString
import org.json4s._
import spray.http.{IllegalRequestException, StatusCodes}
import spray.routing.{ExceptionHandler, HttpService, ValidationRejection}

import scala.concurrent.ExecutionContext.Implicits.global

case class LibraryItemReq(isbn: String, ownership: Ownership)

class ReadingAdminApi(apiConfig: ApiConfig,
  authenticator: BearerTokenAuthenticator,
  libraryAdminService: LibraryAdminService)(implicit val actorRefFactory: ActorRefFactory) extends HttpService with v2.JsonSupport with StrictLogging {

  import ReadingAdminApi._

  implicit override val jsonFormats = JsonFormats.blinkboxFormat() + OwnershipSerializer

  val addFullBook = post {
    path("admin" / "users" / IntNumber / "library") { userId =>
      authenticateAndAuthorize(authenticator.withElevation(Elevation.Critical), hasAnyRole(CustomerServicesRep, CustomerServicesManager)) { repUser =>
        entity(as[LibraryItemReq]) { item =>
          onSuccess(libraryAdminService.addBook(item.isbn, userId, item.ownership)) { res =>
            complete(StatusCodes.NoContent)
          }
        }
      }
    }
  }

  val exceptionHandler = ExceptionHandler {
    case e: CatalogueInfoMissingException => reject(ValidationRejection(e.getMessage))
    case e: LibraryItemConflict => failWith(new IllegalRequestException(StatusCodes.Conflict, e.getMessage))
  }

  val routes = monitor(logger, throwableMarshaller) {
    handleExceptions(exceptionHandler) {
      rootPath(apiConfig.localUrl.path) {
        addFullBook
      }
    }
  }
}

object ReadingAdminApi {

  object OwnershipSerializer extends CustomSerializer[Ownership](_ => ({
    case JString("Owned") => Owned
    case JString("Sample") => Sample
    case JNull => null
  }, {
    case Owned => JString("Owned")
    case Sample => JString("Sample")
  }))
}