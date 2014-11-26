package com.blinkbox.books.reading

import akka.actor.{ActorSystem, Props}
import com.blinkbox.books.config.{ApiConfig, Configuration}
import com.blinkbox.books.logging.Loggers
import com.blinkbox.books.reading.common.persistence.{DbLibraryStore, DefaultDatabaseComponent}
import com.blinkbox.books.slick.MySQLDatabaseSupport
import com.blinkbox.books.spray.{HttpServer, url2uri}
import com.typesafe.scalalogging.slf4j.StrictLogging
import spray.can.Http
import spray.routing.HttpServiceActor

import scala.util.control.ControlThrowable

class WebService(config: ApiConfig) extends HttpServiceActor {

  implicit val executionContext = actorRefFactory.dispatcher

  val readingApi = new ReadingAdminApi(config)

  override def receive: Receive = runRoute(readingApi.routes)
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
    case ex: ControlThrowable => throw ex
    case ex: Throwable =>
      logger.error("Error during initialization of the service", ex)
      System.exit(1)
  }
}
