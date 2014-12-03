package com.blinkbox.books.reading

import akka.actor.{ActorSystem, Props}
import com.blinkbox.books.auth.{ZuulElevationChecker, ZuulTokenDecoder, ZuulTokenDeserializer}
import com.blinkbox.books.clients.catalogue.{DefaultCatalogueV1Service, DefaultClient}
import com.blinkbox.books.config.{ApiConfig, Configuration}
import com.blinkbox.books.logging.Loggers
import com.blinkbox.books.reading.persistence.{DbLibraryStore, DefaultDatabaseComponent}
import com.blinkbox.books.slick.MySQLDatabaseSupport
import com.blinkbox.books.spray.{BearerTokenAuthenticator, HttpServer, url2uri}
import com.blinkbox.books.time.SystemTimeSupport
import com.typesafe.scalalogging.StrictLogging
import spray.can.Http
import spray.routing.HttpServiceActor

import scala.util.control.ControlThrowable

class WebService(
  config: ApiConfig,
  authenticator: BearerTokenAuthenticator,
  libraryService: LibraryService) extends HttpServiceActor {

  implicit val executionContext = actorRefFactory.dispatcher

  val readingApi = new ReadingApi(config, authenticator, libraryService)

  override def receive: Receive = runRoute(readingApi.routes)
}

object Main extends App with Configuration with Loggers with StrictLogging with SystemTimeSupport {
  try {
    logger.info("Starting Reading service")
    val appConfig = AppConfig(config)

    implicit val system = ActorSystem("reading-service", config)
    implicit val ec = system.dispatcher
    implicit val startTimeout = appConfig.startTimeout
    sys.addShutdownHook(system.shutdown())

    // Database setup
    val dbComponent = new DefaultDatabaseComponent(appConfig.db)
    val libraryStore = new DbLibraryStore[MySQLDatabaseSupport](dbComponent.db, dbComponent.tables, dbComponent.exceptionFilter)

    // Catalogue service client setup
    val client = new DefaultClient(appConfig.catalogue) // TODO: configure separate execution context
    val catalogueService = new DefaultCatalogueV1Service(client)

    // Library service setup
    val libraryService = new DefaultLibraryService(libraryStore, catalogueService)

    // Service Http API setup
    val authenticator = new BearerTokenAuthenticator(
      new ZuulTokenDeserializer(new ZuulTokenDecoder(appConfig.auth.keysDir.getAbsolutePath)),
      new ZuulElevationChecker(appConfig.auth.sessionUrl.toString))

    val service = system.actorOf(Props(new WebService(appConfig.api, authenticator, libraryService)), "reading-service-public")

    val localUrl = appConfig.api.localUrl
    HttpServer(Http.Bind(service, localUrl.getHost, localUrl.effectivePort))

  } catch {
    case ex: ControlThrowable => throw ex
    case ex: Throwable =>
      logger.error("Error during initialization of the service", ex)
      System.exit(1)
  }
}