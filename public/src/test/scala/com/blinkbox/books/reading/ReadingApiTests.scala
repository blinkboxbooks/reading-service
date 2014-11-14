package com.blinkbox.books.reading

import java.net.{URL, URI}

import akka.actor.ActorRefFactory
import com.blinkbox.books.auth.{Elevation, User}
import com.blinkbox.books.clients.catalogue.CatalogueInfoMissingException
import com.blinkbox.books.config.ApiConfig
import com.blinkbox.books.reading.common._
import com.blinkbox.books.reading.common.persistence.LibraryMediaMissingException
import com.blinkbox.books.spray.v2.Link
import com.blinkbox.books.spray.v2.`application/vnd.blinkbox.books.v2+json`
import com.blinkbox.books.spray.{BearerTokenAuthenticator, v2}
import com.blinkbox.books.spray.BearerTokenAuthenticator.credentialsInvalidHeaders
import com.blinkbox.books.test.MockitoSyrup
import com.blinkbox.books.time.{StoppedClock, TimeSupport}
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import spray.http.HttpHeaders.{`WWW-Authenticate`, Authorization}
import spray.http.{MediaTypes, GenericHttpCredentials, OAuth2BearerToken}
import spray.http.StatusCodes._
import spray.routing.AuthenticationFailedRejection.CredentialsRejected
import spray.routing.{AuthenticationFailedRejection, HttpService, RequestContext}
import spray.testkit.ScalatestRouteTest

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ReadingApiTests extends FlatSpec with ScalatestRouteTest with MockitoSyrup with v2.JsonSupport {

  "Book details endpoint" should "return book details for a valid request" in new TestFixture {
    when(libraryService.getBook(TestBook.isbn, AuthenticatedUser.id)).thenReturn(Future.successful(Some(TestBook)))
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(AuthenticatedUser)))

    Get(s"/my/library/${TestBook.isbn}") ~> Authorization(OAuth2BearerToken(AccessToken)) ~> routes ~> check {
      assert(status == OK)
      assert(mediaType == `application/vnd.blinkbox.books.v2+json`)
      assert(body.asString == TestBookJson)
    }
  }

  it should "return 401 Unauthorized when user request has invalid access token" in new TestFixture {
    when(authenticator.apply(any[RequestContext]))
      .thenReturn(Future.successful(Left(AuthenticationFailedRejection(CredentialsRejected, credentialsInvalidHeaders))))

    Get(s"/my/library/${TestBook.isbn}") ~> Authorization(GenericHttpCredentials("god", "Argy")) ~> routes ~> check {
      assert(status == Unauthorized &&
        header[`WWW-Authenticate`] == credentialsInvalidHeaders.headOption &&
        mediaType == MediaTypes.`text/plain`)
    }
  }

  // will work once https://git.mobcastdev.com/Platform/common-spray/pull/38 is accepted
  ignore should "return 500 Internal Error when there media links of a book in user's library is missing" in new TestFixture {
    when(libraryService.getBook(TestBook.isbn,AuthenticatedUser.id))
      .thenReturn(Future.failed(new LibraryMediaMissingException("test exception")))
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(AuthenticatedUser)))

    Get(s"/my/library/${TestBook.isbn}") ~> Authorization(OAuth2BearerToken(AccessToken)) ~> routes ~> check {
      assert(status == InternalServerError)
      assert(mediaType == `application/vnd.blinkbox.books.v2+json`)
      assert(body.asString == """{"code":"InternalServerError","developerMessage":"There was an internal server error."}""")
    }
  }

  // will work once https://git.mobcastdev.com/Platform/common-spray/pull/38 is accepted
  ignore should "return 500 Internal Error when there catalogue info of a book in user's library is missing" in new TestFixture {
    when(libraryService.getBook(TestBook.isbn,AuthenticatedUser.id))
      .thenReturn(Future.failed(new CatalogueInfoMissingException("test exception")))
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(AuthenticatedUser)))

    Get(s"/my/library/${TestBook.isbn}") ~> Authorization(OAuth2BearerToken(AccessToken)) ~> routes ~> check {
      assert(status == InternalServerError)
      assert(mediaType == `application/vnd.blinkbox.books.v2+json`)
      assert(body.asString == """{"code":"InternalServerError","developerMessage":"There was an internal server error."}""")
    }
  }

  class TestFixture extends HttpService with TimeSupport {

    val clock = StoppedClock()

    val AccessToken = "accessToken"

    val AuthenticatedUser = User(AccessToken, claims = Map("sub" -> "urn:blinkbox:zuul:user:1", "sso/at" -> "ssoToken"))

    val images = List(Image(CoverImage, new URI("http://media.blinkboxbooks.com/9780/141/909/837/cover.png")))

    val links = List(
      Link(FullEpub, new URI("http://media.blinkboxbooks.com/9780/141/909/837/8c9771c05e504f836e8118804e02f64c.epub")),
      Link(SampleEpub, new URI("http://media.blinkboxbooks.com/9780/141/909/837/8c9771c05e504f836e8118804e02f64c.sample.epub")),
      Link(EpubKey, new URI("https://keys.mobcastdev.com/9780/141/909/837/e237e27468c6b37a5679fab718a893e6.epub.9780141909837.key"))
    )
    val TestBook = BookDetails("9780141909837", "Title", "Sortable Title", "Author", clock.now(), Full, Reading, ReadingPosition(CFI("someCfi"), 15), images, links)

    val TestBookJson = s"""{"isbn":"9780141909837","title":"Title","sortableTitle":"Sortable Title","author":"Author","addedDate":"${clock.now()}","bookType":"Full","readingStatus":"Reading","readingPosition":{"cfi":"someCfi","percentage":15},"images":[{"rel":"CoverImage","url":"http://media.blinkboxbooks.com/9780/141/909/837/cover.png"}],"links":[{"rel":"EpubFull","url":"http://media.blinkboxbooks.com/9780/141/909/837/8c9771c05e504f836e8118804e02f64c.epub"},{"rel":"EpubSample","url":"http://media.blinkboxbooks.com/9780/141/909/837/8c9771c05e504f836e8118804e02f64c.sample.epub"},{"rel":"EpubKey","url":"https://keys.mobcastdev.com/9780/141/909/837/e237e27468c6b37a5679fab718a893e6.epub.9780141909837.key"}]}"""
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
