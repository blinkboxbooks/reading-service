package com.blinkbox.books.reading

import akka.actor.ActorRefFactory
import com.blinkbox.books.config.ApiConfig
import com.blinkbox.books.spray.Directives.rootPath
import com.blinkbox.books.spray.MonitoringDirectives.monitor
import com.blinkbox.books.spray.v2
import com.blinkbox.books.spray.v2.Implicits.throwableMarshaller
import com.blinkbox.books.spray.url2uri
import com.typesafe.scalalogging.StrictLogging
import spray.routing.HttpService

import scala.concurrent.ExecutionContext.Implicits.global

case class Bar(value: String = "bar")

class ReadingAdminApi(apiConfig: ApiConfig)(implicit val actorRefFactory: ActorRefFactory) extends HttpService with v2.JsonSupport with StrictLogging {

  val foo = get {
    path("admin" / "library" / "foo") {
      complete(Bar())
    }
  }

  val routes = monitor(logger, throwableMarshaller) {
    rootPath(apiConfig.localUrl.path) {
      foo
    }
  }
}