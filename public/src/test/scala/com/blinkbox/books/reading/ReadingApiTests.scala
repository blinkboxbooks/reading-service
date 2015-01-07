package com.blinkbox.books.reading

import java.net.{URI, URL}

import akka.actor.ActorRefFactory
import com.blinkbox.books.auth.{Elevation, User}
import com.blinkbox.books.clients.catalogue.{CatalogueInfoMissingException, LibraryItemConflictException}
import com.blinkbox.books.config.ApiConfig
import com.blinkbox.books.reading.ReadingApi.LibraryItemIsbn
import com.blinkbox.books.reading.persistence.LibraryMediaMissingException
import com.blinkbox.books.spray.BearerTokenAuthenticator.credentialsInvalidHeaders
import com.blinkbox.books.spray.v2.{Error, Image, Link, `application/vnd.blinkbox.books.v2+json`}
import com.blinkbox.books.spray.{BearerTokenAuthenticator, JsonFormats, v2}
import com.blinkbox.books.test.MockitoSyrup
import com.blinkbox.books.time.StoppedClock
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import spray.http.HttpHeaders.{Authorization, `WWW-Authenticate`}
import spray.http.StatusCodes._
import spray.http.{GenericHttpCredentials, OAuth2BearerToken, StatusCodes}
import spray.routing.AuthenticationFailedRejection.CredentialsRejected
import spray.routing.{AuthenticationFailedRejection, HttpService, RequestContext}
import spray.testkit.ScalatestRouteTest

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ReadingApiTests extends FlatSpec with ScalatestRouteTest with MockitoSyrup with v2.JsonSupport {

  implicit override val jsonFormats = JsonFormats.blinkboxFormat() ++ ReadingApi.bookDetailsSerializers

  "Book details endpoint" should "return book details for a valid request" in new TestFixture {
    when(libraryService.getBook(testBook.isbn)).thenReturn(Future.successful(Some(testBook)))
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(authenticatedUser)))

    Get(s"/my/library/${testBook.isbn}") ~> Authorization(OAuth2BearerToken(accessToken)) ~> routes ~> check {
      assert(status == OK)
      assert(mediaType == `application/vnd.blinkbox.books.v2+json`)
      assert(responseAs[BookDetails] == testBook)
    }
  }

  it should "return entire user's library for a valid request" in new TestFixture {
    when(libraryService.getLibrary(25, 0)).thenReturn(Future.successful(List(testBook, testBook2)))
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(authenticatedUser)))
    Get("/my/library") ~> Authorization(OAuth2BearerToken(accessToken)) ~> routes ~> check {
      assert(status == OK)
      assert(mediaType == `application/vnd.blinkbox.books.v2+json`)
      assert(responseAs[Map[String, List[BookDetails]]] == Map("items" -> List(testBook, testBook2)))
    }
  }

  it should "return book details without cfi if book's reading status is NotStarted" in new TestFixture {
    when(libraryService.getBook(unreadBook.isbn)).thenReturn(Future.successful(Some(unreadBook)))
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(authenticatedUser)))

    Get(s"/my/library/${unreadBook.isbn}") ~> Authorization(OAuth2BearerToken(accessToken)) ~> routes ~> check {
      assert(status == OK)
      assert(mediaType == `application/vnd.blinkbox.books.v2+json`)
      assert(responseAs[BookDetails] == unreadBook)
    }
  }
  
  it should "return a 201 when adding a new sample book to the library" in new TestFixture {
    when(libraryService.addSample(testBook.isbn)).thenReturn(Future.successful(SampleAdded))
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(authenticatedUser)))

    val request = LibraryItemIsbn(testBook.isbn)
    Post(s"/my/library/samples", request) ~> Authorization(OAuth2BearerToken(accessToken)) ~> routes ~> check {
      assert(status == StatusCodes.Created)
    }
  }

  it should "return a 200 when adding an existing sample book to a user's library" in new TestFixture {
    when(libraryService.addSample(testBook.isbn)).thenReturn(Future.successful(SampleAlreadyExists))
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(authenticatedUser)))

    val request = LibraryItemIsbn(testBook.isbn)
    Post(s"/my/library/samples", request) ~> Authorization(OAuth2BearerToken(accessToken)) ~> routes ~> check {
      assert(status == OK)
    }
  }

  it should "return a 409 when adding a sample book that is a full book in a user's library" in new TestFixture {
    when(libraryService.addSample(testBook.isbn)).thenReturn(Future.failed(new LibraryItemConflictException("blah")))
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(authenticatedUser)))

    val request = LibraryItemIsbn(testBook.isbn)
    Post(s"/my/library/samples", request) ~> Authorization(OAuth2BearerToken(accessToken)) ~> routes ~> check {
      assert(status == Conflict)
      assert(mediaType == `application/vnd.blinkbox.books.v2+json`)
      assert(responseAs[Error] == Error("Conflict", Some("The request could not be processed because of conflict in the request, such as an edit conflict.")))
    }
  }

  it should "return a 400 bad request if an invalid ISBN is given" in new TestFixture {
    val isbn = "invalid"
    val request = LibraryItemIsbn(isbn)
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(authenticatedUser)))
    Post(s"/my/library/samples", request) ~> Authorization(OAuth2BearerToken(accessToken)) ~> routes ~> check {
      assert(status == BadRequest)
      assert(mediaType == `application/vnd.blinkbox.books.v2+json`)
      assert(responseAs[Error] == Error("BadRequest", Some("Isbn must be 13 digits long and start with the number 9")))
    }
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
      assert(status == Unauthorized)
      assert(header[`WWW-Authenticate`] == credentialsInvalidHeaders.headOption)
      assert(mediaType == `application/vnd.blinkbox.books.v2+json`)
      assert(responseAs[Error] == Error("Unauthorized", Some("The supplied authentication is invalid")))
    }
  }

  it should "return 404 NotFound when user does not have the requested book in the library" in new TestFixture {
    when(libraryService.getBook(testBook.isbn)).thenReturn(Future.successful(None))
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(authenticatedUser)))

    Get(s"/my/library/${testBook.isbn}") ~> Authorization(OAuth2BearerToken(accessToken)) ~> routes ~> check {
      assert(status == NotFound)
      assert(mediaType == `application/vnd.blinkbox.books.v2+json`)
      assert(responseAs[Error] == Error("NotFound", Some("The requested resource could not be found but may be available again in the future.")))
    }
  }

  it should "return 500 Internal Error when media links of a book in user's library are missing" in new TestFixture {
    when(libraryService.getBook(testBook.isbn))
      .thenReturn(Future.failed(new LibraryMediaMissingException("test exception")))
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(authenticatedUser)))

    Get(s"/my/library/${testBook.isbn}") ~> Authorization(OAuth2BearerToken(accessToken)) ~> routes ~> check {
      assert(status == InternalServerError)
      assert(mediaType == `application/vnd.blinkbox.books.v2+json`)
      assert(responseAs[Error] == Error("InternalServerError", Some("There was an internal server error.")))
    }
  }

  it should "return 500 Internal Error when catalogue info of a book in user's library is missing" in new TestFixture {
    when(libraryService.getBook(testBook.isbn)).thenReturn(Future.failed(new CatalogueInfoMissingException("test exception")))
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(authenticatedUser)))

    Get(s"/my/library/${testBook.isbn}") ~> Authorization(OAuth2BearerToken(accessToken)) ~> routes ~> check {
      assert(status == InternalServerError)
      assert(mediaType == `application/vnd.blinkbox.books.v2+json`)
      assert(responseAs[Error] == Error("InternalServerError", Some("There was an internal server error.")))
    }
  }

  "Library endpoint" should "return entire user's library for a valid request" in new TestFixture {
    when(libraryService.getLibrary(25, 0)).thenReturn(Future.successful(List(testBook, testBook2)))
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(authenticatedUser)))
    Get("/my/library") ~> Authorization(OAuth2BearerToken(accessToken)) ~> routes ~> check {
      assert(status == OK)
      assert(mediaType == `application/vnd.blinkbox.books.v2+json`)
      assert(responseAs[Map[String, List[BookDetails]]] == Map("items" -> List(testBook, testBook2)))
    }
  }

  it should "return an empty library for a valid request of a user who does not have a library yet" in new TestFixture {
    when(libraryService.getLibrary(25, 0)).thenReturn(Future.successful(List.empty))
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(authenticatedUser)))
    Get("/my/library") ~> Authorization(OAuth2BearerToken(accessToken)) ~> routes ~> check {
      assert(status == OK)
      assert(mediaType == `application/vnd.blinkbox.books.v2+json`)
      assert(responseAs[Map[String, List[BookDetails]]] == Map("items" -> List.empty))
    }
  }

  class TestFixture extends HttpService {
    val clock = StoppedClock()

    val accessToken = "accessToken"
    implicit val authenticatedUser = User(accessToken, claims = Map("sub" -> "urn:blinkbox:zuul:user:1", "sso/at" -> "ssoToken"))

    val images = List(Image(CoverImage, new URI("http://media.blinkboxbooks.com/cover.png")))
    val links = List(
      Link(FullEpub, new URI("http://media.blinkboxbooks.com/full.epub")),
      Link(SampleEpub, new URI("http://media.blinkboxbooks.com/sample.epub")),
      Link(EpubKey, new URI("https://keys.mobcastdev.com/epub.key"))
    )

    val unreadBook = BookDetails("9780141909836", "Title", "Author", "Sortable Author", clock.now(), Owned, NotStarted, ReadingPosition(None, 0), images, links)
    val testBook = BookDetails("9780141909837", "Title", "Author", "Sortable Author", clock.now(), Owned, Reading, ReadingPosition(Some(Cfi("someCfi")), 15), images, links)
    val testBook2 = BookDetails("9780234123501", "Other Title", "Other Author", "Author, Other", clock.now(), Owned, Reading, ReadingPosition(Some(Cfi("someCfi")), 30), images, links)

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