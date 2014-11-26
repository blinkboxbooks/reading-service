package com.blinkbox.books.reading

import akka.actor.{ActorRefFactory, ActorSystem, Props}
import com.blinkbox.books.config.{ApiConfig, Configuration}
import com.blinkbox.books.logging.Loggers
import com.blinkbox.books.reading.persistence.{DbLibraryStore, DefaultDatabaseComponent}
import com.blinkbox.books.slick.MySQLDatabaseSupport
import com.blinkbox.books.spray.{HealthCheckHttpService, HttpServer, url2uri}
import com.typesafe.scalalogging.StrictLogging
import spray.can.Http
import spray.http.Uri.Path
import spray.routing.HttpServiceActor

class WebService(config: ApiConfig) extends HttpServiceActor {

  implicit val executionContext = actorRefFactory.dispatcher

  val healthService = new HealthCheckHttpService {
    override val basePath: Path = Path("/")

    override implicit def actorRefFactory: ActorRefFactory = context
  }

  val readingAdminApi = new ReadingAdminApi(config)

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

    val service = system.actorOf(Props(new WebService(appConfig.api)), "reading-service-public")

    val localUrl = appConfig.api.localUrl
    HttpServer(Http.Bind(service, localUrl.getHost, localUrl.effectivePort))

  } catch {
    case ex: Throwable =>
      logger.error("Error during initialization of the service", ex)
      System.exit(1)
  }
}
