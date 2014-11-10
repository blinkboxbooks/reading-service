package com.blinkbox.books.reading

import java.net.{URL, URI}

import akka.actor.ActorRefFactory
import com.blinkbox.books.auth.{Elevation, User}
import com.blinkbox.books.config.ApiConfig
import com.blinkbox.books.reading.common.{CFI, ReadingPosition, Book}
import com.blinkbox.books.spray.v2.`application/vnd.blinkbox.books.v2+json`
import com.blinkbox.books.spray.{BearerTokenAuthenticator, v2}
import com.blinkbox.books.test.MockitoSyrup
import com.blinkbox.books.time.{StoppedClock, TimeSupport}
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import spray.http.HttpHeaders.Authorization
import spray.http.OAuth2BearerToken
import spray.http.StatusCodes._
import spray.routing.{HttpService, RequestContext}
import spray.testkit.ScalatestRouteTest

import org.json4s.jackson.JsonMethods._

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ReadingApiTests extends FlatSpec with ScalatestRouteTest with MockitoSyrup with v2.JsonSupport {

  "Book details endpoint" should "return book details for a valid request" in new TestFixture {
    when(libraryService.getBook(TestBook.isbn, AuthenticatedUser.id)).thenReturn(Future.successful(Some(TestBook)))
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(AuthenticatedUser)))

    Get(s"/my/library/${TestBook.isbn}") ~> Authorization(OAuth2BearerToken(AccessToken)) ~> routes ~> check {
      assert(status == OK)
      assert(mediaType == `application/vnd.blinkbox.books.v2+json`)
      assert(pretty(render(parse(body.asString))) == TestBookJson)
    }
  }

  class TestFixture extends HttpService {

    val AccessToken = "accessToken"

    val AuthenticatedUser = User(AccessToken, claims = Map("sub" -> "urn:blinkbox:zuul:user:1", "sso/at" -> "ssoToken"))

    val links = Map(
      "epubFull" -> new URI("http://media.blinkboxbooks.com/9780/141/909/837/8c9771c05e504f836e8118804e02f64c.epub"),
      "epubSample" -> new URI("http://media.blinkboxbooks.com/9780/141/909/837/8c9771c05e504f836e8118804e02f64c.sample.epub"),
      "epubKey" -> new URI("https://keys.mobcastdev.com/9780/141/909/837/e237e27468c6b37a5679fab718a893e6.epub.9780141909837.key")
    )
    val TestBook = Book("9780141909837", isSample = false, ReadingPosition(CFI("someCfi"), 15), links)

    val TestBookJson =
      """{
        |  "isbn" : "9780141909837",
        |  "isSample" : false,
        |  "readingPosition" : {
        |    "cfi" : "someCfi",
        |    "percentage" : 15
        |  },
        |  "links" : {
        |    "epubFull" : "http://media.blinkboxbooks.com/9780/141/909/837/8c9771c05e504f836e8118804e02f64c.epub",
        |    "epubSample" : "http://media.blinkboxbooks.com/9780/141/909/837/8c9771c05e504f836e8118804e02f64c.sample.epub",
        |    "epubKey" : "https://keys.mobcastdev.com/9780/141/909/837/e237e27468c6b37a5679fab718a893e6.epub.9780141909837.key"
        |  }
        |}""".stripMargin

    val apiConfig = mock[ApiConfig]
    when(apiConfig.localUrl).thenReturn(new URL("http://localhost"))

    val authenticator = mock[BearerTokenAuthenticator]
    when(authenticator.withElevation(Elevation.Unelevated)).thenReturn(authenticator)

    val libraryService = mock[LibraryService]
    val testService = new ReadingApi(apiConfig, authenticator, libraryService)(system)

    def routes = testService.routes

    override implicit def actorRefFactory: ActorRefFactory = system
  }
}
