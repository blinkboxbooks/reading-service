package com.blinkbox.books.reading

import java.net.{URI, URL}

import akka.actor.ActorRefFactory
import com.blinkbox.books.auth.{Elevation, User}
import com.blinkbox.books.clients.catalogue.{CatalogueService, CatalogueInfoMissingException}
import com.blinkbox.books.config.ApiConfig
import com.blinkbox.books.reading.persistence.{LibraryStore, LibraryMediaMissingException}
import com.blinkbox.books.spray.BearerTokenAuthenticator.credentialsInvalidHeaders
import com.blinkbox.books.spray.v2.{Link, `application/vnd.blinkbox.books.v2+json`}
import com.blinkbox.books.spray.{BearerTokenAuthenticator, v2}
import com.blinkbox.books.test.MockitoSyrup
import com.blinkbox.books.time.{StoppedClock, TimeSupport}
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import spray.http.HttpHeaders.{Authorization, `WWW-Authenticate`}
import spray.http.StatusCodes._
import spray.http.{GenericHttpCredentials, MediaTypes, OAuth2BearerToken}
import spray.routing.AuthenticationFailedRejection.CredentialsRejected
import spray.routing.{AuthenticationFailedRejection, HttpService, RequestContext}
import spray.testkit.ScalatestRouteTest

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ReadingApiTests extends FlatSpec with ScalatestRouteTest with MockitoSyrup with v2.JsonSupport {

  "Book details endpoint" should "return book details for a valid request" in new TestFixture {
    when(libraryService.getBook(testBook.isbn)).thenReturn(Future.successful(Some(testBook)))
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(authenticatedUser)))

    Get(s"/my/library/${testBook.isbn}") ~> Authorization(OAuth2BearerToken(accessToken)) ~> routes ~> check {
      assert(status == OK)
      assert(mediaType == `application/vnd.blinkbox.books.v2+json`)
      assert(body.asString == testBookJson)
    }
  }

  it should "return entire user's library for a valid request" in new TestFixture {
    when(libraryService.getLibrary(25, 0)).thenReturn(Future.successful(List(testBook, testBook2)))
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(authenticatedUser)))
    Get("/my/library") ~> Authorization(OAuth2BearerToken(accessToken)) ~> routes ~> check {
      assert(status == OK)
      assert(mediaType == `application/vnd.blinkbox.books.v2+json`)
      assert(body.asString == libraryJson)
    }
  }

  it should "return samples in a user's library" in new TestFixture {
    when(libraryService.getSamples(25, 0)).thenReturn(Future.successful(List(testBook2)))
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(authenticatedUser)))
    Get("/my/library/samples") ~> Authorization(OAuth2BearerToken(accessToken)) ~> routes ~> check {
      assert(status == OK)
      assert(mediaType == `application/vnd.blinkbox.books.v2+json`)
      assert(body.asString == sampleJson)
    }
  }

  it should "return an empty library for a valid request of a user who does not have a library yet" in new TestFixture {
    when(libraryService.getLibrary(25, 0)).thenReturn(Future.successful(List.empty))
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(authenticatedUser)))
    Get("/my/library") ~> Authorization(OAuth2BearerToken(accessToken)) ~> routes ~> check {
      assert(status == OK)
      assert(mediaType == `application/vnd.blinkbox.books.v2+json`)
      assert(body.asString == """{"items":[]}""")
    }
  }

  it should "return a 201 when adding a new sample book to the library" in new TestFixture {

  }

  it should "return a 200 when adding an existing sample book to a user's library" in new TestFixture {

  }

  it should "return a 409 when adding a sample book that is a full book in a user's library" in new TestFixture {

  }

  it should "return a 400 bad request if an invalid ISBN is given" in new TestFixture {

  }

  it should "return an empty library for a valid request of a user who does not have samples in his library" in new TestFixture {
    when(libraryService.getSamples(25, 0)).thenReturn(Future.successful(List.empty))
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(authenticatedUser)))
    Get("/my/library/samples") ~> Authorization(OAuth2BearerToken(accessToken)) ~> routes ~> check {
      assert(status == OK)
      assert(mediaType == `application/vnd.blinkbox.books.v2+json`)
      assert(body.asString == """{"items":[]}""")
    }
  }

  it should "return 401 Unauthorized when user request has invalid access token" in new TestFixture {
    when(authenticator.apply(any[RequestContext]))
      .thenReturn(Future.successful(Left(AuthenticationFailedRejection(CredentialsRejected, credentialsInvalidHeaders))))

    Get(s"/my/library/${testBook.isbn}") ~> Authorization(GenericHttpCredentials("user", "Argy")) ~> routes ~> check {
      assert(status == Unauthorized &&
        header[`WWW-Authenticate`] == credentialsInvalidHeaders.headOption &&
        mediaType == MediaTypes.`text/plain`)
    }
  }

  it should "return 404 NotFound when user does not have the requested book in the library" in new TestFixture {
    when(libraryService.getBook(testBook.isbn)).thenReturn(Future.successful(None))
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(authenticatedUser)))

    Get(s"/my/library/${testBook.isbn}") ~> Authorization(OAuth2BearerToken(accessToken)) ~> routes ~> check {
      assert(status == NotFound)
      // TODO: check that the body contains correct instance of Error object once the RejectionHandler in common-spray supports it
      //assert(mediaType == `application/vnd.blinkbox.books.v2+json`)

    }
  }

  it should "return 500 Internal Error when media links of a book in user's library are missing" in new TestFixture {
    when(libraryService.getBook(testBook.isbn))
      .thenReturn(Future.failed(new LibraryMediaMissingException("test exception")))
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(authenticatedUser)))

    Get(s"/my/library/${testBook.isbn}") ~> Authorization(OAuth2BearerToken(accessToken)) ~> routes ~> check {
      assert(status == InternalServerError)
      assert(mediaType == `application/vnd.blinkbox.books.v2+json`)
      assert(body.asString == """{"code":"InternalServerError","developerMessage":"There was an internal server error."}""")
    }
  }

  it should "return 500 Internal Error when catalogue info of a book in user's library is missing" in new TestFixture {
    when(libraryService.getBook(testBook.isbn)).thenReturn(Future.failed(new CatalogueInfoMissingException("test exception")))
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(authenticatedUser)))

    Get(s"/my/library/${testBook.isbn}") ~> Authorization(OAuth2BearerToken(accessToken)) ~> routes ~> check {
      assert(status == InternalServerError)
      assert(mediaType == `application/vnd.blinkbox.books.v2+json`)
      assert(body.asString == """{"code":"InternalServerError","developerMessage":"There was an internal server error."}""")
    }
  }

  class TestFixture extends HttpService with TimeSupport {

    val clock = StoppedClock()

    val accessToken = "accessToken"

    implicit val authenticatedUser = User(accessToken, claims = Map("sub" -> "urn:blinkbox:zuul:user:1", "sso/at" -> "ssoToken"))

    val images = List(Image(CoverImage, new URI("http://media.blinkboxbooks.com/9780/141/909/837/cover.png")))

    val links = List(
      Link(FullEpub, new URI("http://media.blinkboxbooks.com/9780/141/909/837/8c9771c05e504f836e8118804e02f64c.epub")),
      Link(SampleEpub, new URI("http://media.blinkboxbooks.com/9780/141/909/837/8c9771c05e504f836e8118804e02f64c.sample.epub")),
      Link(EpubKey, new URI("https://keys.mobcastdev.com/9780/141/909/837/e237e27468c6b37a5679fab718a893e6.epub.9780141909837.key"))
    )
    val testBook = BookDetails("9780141909837", "Title", "Author", "Sortable Author", clock.now(), Full, Reading, ReadingPosition(Cfi("someCfi"), 15), images, links)
    // For brevity, I'm using the same sets of images and links
    val testBook2 = BookDetails("9780234123501", "Other Title", "Other Author", "Author, Other", clock.now(), Sample, Reading, ReadingPosition(Cfi("someCfi"), 30), images, links)

    val testBookJson = s"""{"isbn":"9780141909837","title":"Title","author":"Author","sortableAuthor":"Sortable Author","addedDate":"${clock.now()}","bookType":"Full","readingStatus":"Reading","readingPosition":{"cfi":"someCfi","percentage":15},"images":[{"rel":"CoverImage","url":"http://media.blinkboxbooks.com/9780/141/909/837/cover.png"}],"links":[{"rel":"EpubFull","url":"http://media.blinkboxbooks.com/9780/141/909/837/8c9771c05e504f836e8118804e02f64c.epub"},{"rel":"EpubSample","url":"http://media.blinkboxbooks.com/9780/141/909/837/8c9771c05e504f836e8118804e02f64c.sample.epub"},{"rel":"EpubKey","url":"https://keys.mobcastdev.com/9780/141/909/837/e237e27468c6b37a5679fab718a893e6.epub.9780141909837.key"}]}"""
    val testBook2Json = s"""{"isbn":"9780234123501","title":"Other Title","author":"Other Author","sortableAuthor":"Author, Other","addedDate":"${clock.now()}","bookType":"Sample","readingStatus":"Reading","readingPosition":{"cfi":"someCfi","percentage":30},"images":[{"rel":"CoverImage","url":"http://media.blinkboxbooks.com/9780/141/909/837/cover.png"}],"links":[{"rel":"EpubFull","url":"http://media.blinkboxbooks.com/9780/141/909/837/8c9771c05e504f836e8118804e02f64c.epub"},{"rel":"EpubSample","url":"http://media.blinkboxbooks.com/9780/141/909/837/8c9771c05e504f836e8118804e02f64c.sample.epub"},{"rel":"EpubKey","url":"https://keys.mobcastdev.com/9780/141/909/837/e237e27468c6b37a5679fab718a893e6.epub.9780141909837.key"}]}"""
    val libraryJson = s"""{"items":[${testBookJson},${testBook2Json}]}"""
    val sampleJson = s"""{"items":[${testBook2Json}]}"""
    val apiConfig = mock[ApiConfig]
    when(apiConfig.localUrl).thenReturn(new URL("http://localhost"))

    val authenticator = mock[BearerTokenAuthenticator]
    when(authenticator.withElevation(Elevation.Unelevated)).thenReturn(authenticator)

    val libraryService = mock[LibraryService]
    val testService = new ReadingApi(apiConfig, authenticator, libraryService)(system)

    def routes = testService.routes

    override implicit def actorRefFactory: ActorRefFactory = system
  }

  // To be used only to test the failing cases for when the catalogue service does not return the correct number of results
  class StoreFixture extends HttpService with TimeSupport {

    // Akka system
    override implicit def actorRefFactory: ActorRefFactory = system

    // Add a stopped clock
    override val clock = StoppedClock()

    // The library service with the mocked out stores
    val libraryStore = mock[LibraryStore]
    val catalogueService = mock[CatalogueService]
    val libraryService = new DefaultLibraryService(libraryStore, catalogueService)

    // Authenticator mock
    val accessToken = "accessToken"
    implicit val authenticatedUser = User(accessToken, claims = Map("sub" -> "urn:blinkbox:zuul:user:1", "sso/at" -> "ssoToken"))
    val authenticator = mock[BearerTokenAuthenticator]
    when(authenticator.withElevation(Elevation.Unelevated)).thenReturn(authenticator)

    // Setting up the Reading Api
    val apiConfig = mock[ApiConfig]
    when(apiConfig.localUrl).thenReturn(new URL("http://localhost"))
    val testService = new ReadingApi(apiConfig, authenticator, libraryService)(system)

    // Set the routes of this service to be that of the testService
    def routes = testService.routes
  }
}
