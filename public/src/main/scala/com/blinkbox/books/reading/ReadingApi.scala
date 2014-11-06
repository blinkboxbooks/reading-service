package com.blinkbox.books.reading

import com.blinkbox.books.auth.User
import com.blinkbox.books.config.ApiConfig
import com.blinkbox.books.spray.Directives.rootPath
import com.blinkbox.books.spray.MonitoringDirectives.monitor
import com.blinkbox.books.spray.{ElevatedContextAuthenticator, url2uri}
import org.slf4j.LoggerFactory
import spray.routing.Route
import spray.routing.Directives._
import spray.http.StatusCodes._

class ReadingApi(
  apiConfig: ApiConfig,
  authenticator: ElevatedContextAuthenticator[User],
  libraryService: LibraryService) {

  val log = LoggerFactory.getLogger(classOf[ReadingApi])

  val getBookDetails: Route = path("dummy") {
    get {
      complete(OK)
    }
  }

  val routes = monitor(log) {
    rootPath(apiConfig.localUrl.path) {
      getBookDetails
    }
  }
}
