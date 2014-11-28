package com.blinkbox.books.reading.admin

import akka.actor.{ActorRefFactory, ActorSystem, Props}
import com.blinkbox.books.auth.{ZuulElevationChecker, ZuulTokenDecoder, ZuulTokenDeserializer}
import com.blinkbox.books.clients.catalogue.{DefaultCatalogueV1Service, DefaultClient}
import com.blinkbox.books.config.{ApiConfig, Configuration}
import com.blinkbox.books.logging.Loggers
import com.blinkbox.books.reading.persistence.{DbLibraryStore, DefaultDatabaseComponent}
import com.blinkbox.books.slick.MySQLDatabaseSupport
import com.blinkbox.books.spray.{BearerTokenAuthenticator, HealthCheckHttpService, HttpServer, url2uri}
import com.typesafe.scalalogging.StrictLogging
import spray.can.Http
import spray.http.Uri.Path
import spray.routing.HttpServiceActor

class WebService(
  config: ApiConfig,
  authenticator: BearerTokenAuthenticator,
  libraryAdminService: LibraryAdminService) extends HttpServiceActor {

  implicit val executionContext = actorRefFactory.dispatcher

  val healthService = new HealthCheckHttpService {
    override val basePath: Path = Path("/")

    override implicit def actorRefFactory: ActorRefFactory = context
  }

  val readingAdminApi = new ReadingAdminApi(config, authenticator, libraryAdminService)

  override def receive: Receive = runRoute(healthService.routes ~ readingAdminApi.routes)
}

object Main extends App with Configuration with Loggers with StrictLogging {
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

    // Service setup
    val authenticator = new BearerTokenAuthenticator(
      new ZuulTokenDeserializer(new ZuulTokenDecoder(appConfig.auth.keysDir.getAbsolutePath)),
      new ZuulElevationChecker(appConfig.auth.sessionUrl.toString))

    val libraryAdminService = new DefaultLibraryAdminService(libraryStore, catalogueService)

    val service = system.actorOf(Props(new WebService(appConfig.api, authenticator, libraryAdminService)), "reading-service-admin")

    val localUrl = appConfig.api.localUrl
    HttpServer(Http.Bind(service, localUrl.getHost, localUrl.effectivePort))

  } catch {
    case ex: Throwable =>
      logger.error("Error during initialization of the service", ex)
      System.exit(1)
  }
}
