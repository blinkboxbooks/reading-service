package com.blinkbox.books.clients.catalogue

import akka.actor.{ActorRefFactory, ActorSystem}
import akka.util.Timeout
import com.blinkbox.books.clients.ClientConfig
import spray.client.pipelining._
import spray.http.StatusCodes._
import spray.http._
import spray.httpx.UnsuccessfulResponseException
import spray.httpx.unmarshalling._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Basic API for Spray client requests.
 */
trait Client {
  val config: ClientConfig
  def unitRequest(req: HttpRequest, credentials: Option[HttpCredentials]): Future[Unit]
  def dataRequest[T : FromResponseUnmarshaller](req: HttpRequest, credentials: Option[HttpCredentials]): Future[T]
}

/**
 * Trait that provides common implementation of request handling and response parsing.
 */
trait SprayClient extends Client {
  val system: ActorSystem
  val ec: ExecutionContext

  implicit lazy val timeout = Timeout(config.timeout)
  implicit lazy val sys = system
  implicit lazy val executionContext = ec

  protected def doSendReceive(implicit refFactory: ActorRefFactory, ec: ExecutionContext): SendReceive = sendReceive(refFactory, ec)

  protected lazy val unitIfSuccessful = { resp: HttpResponse =>
    if (resp.status.isSuccess) () else throw new UnsuccessfulResponseException(resp)
  }

  protected def basePipeline(credentials: Option[HttpCredentials]): SendReceive = credentials match {
    case Some(creds) => addCredentials(creds) ~> doSendReceive
    case None => doSendReceive
  }

  protected def unitPipeline(credentials: Option[HttpCredentials]) = basePipeline(credentials) ~> unitIfSuccessful

  protected def dataPipeline[T : FromResponseUnmarshaller](credentials: Option[HttpCredentials]) =
    basePipeline(credentials) ~> unmarshal[T]

  override def unitRequest(req: HttpRequest, credentials: Option[HttpCredentials]): Future[Unit] =
    unitPipeline(credentials)(req)
      .transform(identity, exceptionTransformer)

  override def dataRequest[T : FromResponseUnmarshaller](req: HttpRequest, credentials: Option[HttpCredentials]): Future[T] =
    dataPipeline(credentials).apply(req)
      .transform(identity, exceptionTransformer)

  def exceptionTransformer: Throwable => Throwable = {
    case ex: UnsuccessfulResponseException =>
      ex.response.status match {
        case BadRequest => new BadRequestException(ex.response.entity.asString, ex)
        case NotFound => new NotFoundException(ex.response.entity.asString, ex)
        case other => ex
      }
    case other => other
  }
}

/**
* Concrete implementation of Catalogue service client.
*/
class DefaultClient(val config: ClientConfig)(implicit val ec: ExecutionContext, val system: ActorSystem) extends SprayClient

// Exceptions raised by client API.
class BadRequestException(val error: String, cause: Throwable = null) extends RuntimeException(error, cause)
class NotFoundException(val error: String, cause: Throwable = null) extends RuntimeException(error, cause)
class UnauthorizedException(val error: String, val challenge: HttpChallenge, cause: Throwable = null) extends RuntimeException(error, cause)
class LibraryItemConflictException(val error: String, cause: Throwable = null) extends RuntimeException(error, cause)