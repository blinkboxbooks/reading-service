package com.blinkbox.books.reading.admin

import java.net.URL

import com.blinkbox.books.auth.{Elevation, User, UserRole}
import com.blinkbox.books.clients.catalogue.CatalogueInfoMissingException
import com.blinkbox.books.config.ApiConfig
import com.blinkbox.books.reading.{Owned, Ownership}
import com.blinkbox.books.reading.admin.ReadingAdminApi.OwnershipSerializer
import com.blinkbox.books.spray.BearerTokenAuthenticator._
import com.blinkbox.books.spray.v2._
import com.blinkbox.books.spray.{BearerTokenAuthenticator, JsonFormats, v2}
import com.blinkbox.books.test.MockitoSyrup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.FlatSpec
import spray.http.HttpHeaders.{Authorization, `WWW-Authenticate`}
import spray.http.OAuth2BearerToken
import spray.http.StatusCodes._
import spray.routing.AuthenticationFailedRejection.CredentialsRejected
import spray.routing.{AuthenticationFailedRejection, RequestContext}
import spray.testkit.ScalatestRouteTest

import scala.concurrent.Future

class LibraryAdminApiTests extends FlatSpec with ScalatestRouteTest with MockitoSyrup {

  "Add book endpoint" should "return 204 No Content for a valid request" in new TestFixture {
    when(libraryAdminService.addBook(any[String], any[Int], any[Ownership])).thenReturn(Future.successful(()))
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(AuthenticatedUser)))

    Post("/admin/users/123/library", LibraryItemReq(Isbn, Owned)) ~> Authorization(OAuth2BearerToken(AccessToken)) ~> routes ~> check {
      assert(status == NoContent)
      assert(entity.isEmpty)

      verify(libraryAdminService).addBook(Isbn, 123, Owned)
    }
  }

  it should "return 400 Bad Request when the book is not in catalogue" in new TestFixture {
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(AuthenticatedUser)))
    when(libraryAdminService.addBook(any[String], any[Int], any[Ownership])).thenReturn(Future.failed(new CatalogueInfoMissingException("test exception")))

    Post("/admin/users/123/library", LibraryItemReq(Isbn, Owned)) ~> Authorization(OAuth2BearerToken(AccessToken)) ~> routes ~> check {
      assert(status == BadRequest)
      //TODO: check the returned Error object when common-spray adds support for Rejection -> Error conversion.

      verify(libraryAdminService).addBook(Isbn, 123, Owned)
    }
  }

  it should "return 401 Unauthorized when user adding the book is not critically elevated" in new TestFixture {
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Left(AuthenticationFailedRejection(CredentialsRejected, unverifiedIdentityHeaders))))

    Post("/admin/users/123/library", LibraryItemReq(Isbn, Owned)) ~> Authorization(OAuth2BearerToken(AccessToken)) ~> routes ~> check {
      assert(status == Unauthorized)
      assert(header[`WWW-Authenticate`] == unverifiedIdentityHeaders.headOption)

      verifyNoMoreInteractions(libraryAdminService)
    }
  }

  it should "return 403 Forbidden when user adding the book does not have either CSR/CSM role" in new TestFixture {
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(UserWithoutRequiredRole)))

    Post("/admin/users/123/library", LibraryItemReq(Isbn, Owned)) ~> Authorization(OAuth2BearerToken(AccessToken)) ~> routes ~> check {
      assert(status == Forbidden)

      verifyNoMoreInteractions(libraryAdminService)
    }
  }

  it should "return 409 Conflict when the book being added is already in user's library" in new TestFixture {
    when(authenticator.apply(any[RequestContext])).thenReturn(Future.successful(Right(AuthenticatedUser)))
    when(libraryAdminService.addBook(any[String], any[Int], any[Ownership])).thenReturn(Future.failed(new LibraryItemConflict("test exception")))

    Post("/admin/users/123/library", LibraryItemReq(Isbn, Owned)) ~> Authorization(OAuth2BearerToken(AccessToken)) ~> routes ~> check {
      assert(status == Conflict)
      assert(mediaType == `application/vnd.blinkbox.books.v2+json`)
      val expectedError = v2.Error("Conflict", Some("The request could not be processed because of conflict in the request, such as an edit conflict."))
      assert(responseAs[v2.Error] == expectedError)

      verify(libraryAdminService).addBook(Isbn, 123, Owned)
    }
  }

  class TestFixture extends v2.JsonSupport {

    implicit override val jsonFormats = JsonFormats.blinkboxFormat() + OwnershipSerializer

    val Isbn = "1234567890123"
    val AccessToken = "accessToken"

    val AuthenticatedUser = User(AccessToken, claims = Map("sub" -> "urn:blinkbox:zuul:user:1", "bb/rol" -> Array(UserRole.CustomerServicesRep)))
    val UserWithoutRequiredRole = User(AccessToken, claims = Map("sub" -> "urn:blinkbox:zuul:user:2"))

    val authenticator = mock[BearerTokenAuthenticator]
    when(authenticator.withElevation(Elevation.Critical)).thenReturn(authenticator)

    val apiConfig = mock[ApiConfig]
    when(apiConfig.localUrl).thenReturn(new URL("http://localhost"))

    val libraryAdminService = mock[LibraryAdminService]
    val testService = new ReadingAdminApi(apiConfig, authenticator, libraryAdminService)(system)

    def routes = testService.routes
  }
}
