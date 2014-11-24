package com.blinkbox.books.clients.catalogue

import java.net.{URI, URL}

import akka.actor.{ActorRefFactory, ActorSystem}
import com.blinkbox.books.reading.ClientConfig
import com.blinkbox.books.spray.v1
import com.blinkbox.books.spray.v1.`application/vnd.blinkboxbooks.data.v1+json`
import com.blinkbox.books.test.{FailHelper, MockitoSyrup}
import org.json4s.JsonAST.JValue
import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.FlatSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.time.{Millis, Seconds, Span}
import spray.client.pipelining._
import spray.http.StatusCodes._
import spray.http._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class CatalogueServiceClientTests extends FlatSpec with ScalaFutures with FailHelper with MockitoSyrup {

  // Settings for whenReady/Waiter.
  implicit val defaultPatience = PatienceConfig(timeout = scaled(Span(5, Seconds)), interval = scaled(Span(5, Millis)))

  "Catalogue service client" should "return book info for a valid ISBN" in new TestFixture {
    provideJsonResponse(OK, bookResponse(TestBookInfo.id, TestBookInfo.title, TestBookInfo.images, TestBookInfo.links))
    whenReady(service.getBookInfo(TestBookIsbn)) { res =>
      assert(res == TestBookInfo)
    }
  }

  it should "throw NotFoundException if the book is not in the catalogue" in new TestFixture {
    provideErrorResponse(new NotFoundException("book not there"))

    val ex = failingWith[CatalogueInfoMissingException](service.getBookInfo("Non-existent-isbn"))
    assert(ex.getMessage == "Catalogue does not have a book with isbn: Non-existent-isbn")
  }

  it should "return contributor info for a valid ISBN" in new TestFixture {
    provideJsonResponse(OK, contributorResponse(TestContributorId, TestContributorInfo.displayName, TestContributorInfo.sortName))
    whenReady(service.getContributorInfo(TestContributorId)) { res =>
      assert(res == TestContributorInfo)
    }
  }

  it should "throw NotFoundException if the contributor info is not in the catalogue" in new TestFixture {
    provideErrorResponse(new NotFoundException("contributor not there"))

    val ex = failingWith[CatalogueInfoMissingException](service.getContributorInfo("Non-existent-contributor-id"))
    assert(ex.getMessage == "Catalogue does not have a contributor with id: Non-existent-contributor-id")
  }

  "Catalogue service" should "return book and contributor info for a valid ISBN" in new TestFixture {
    val firstResponse = (OK, bookResponse(TestBookInfo.id, TestBookInfo.title, TestBookInfo.images, TestBookInfo.links))
    val secondResponse = (OK, contributorResponse(TestContributorId, TestContributorInfo.displayName, TestContributorInfo.sortName))
    provideJsonResponses(List(firstResponse, secondResponse))

    whenReady(service.getInfoFor(TestBookIsbn)) { res =>
      assert(res == CatalogueInfo(TestBookIsbn, TestBookInfo.title, TestContributorInfo.displayName,  TestContributorInfo.sortName, new URI(TestBookCoverImage.src), new URI(TestBookInfo.links.filter(_.rel.endsWith("samplemedia")).head.href)))
    }
  }

  it should "return book info for multiple valid ISBNs" in new TestFixture {
    val book1response = bookResponseJson(TestBookInfo.id, TestBookInfo.title, TestBookInfo.images, TestBookInfo.links)
    val book2response = bookResponseJson(SecondBookTestInfo.id, SecondBookTestInfo.title, SecondBookTestInfo.images, SecondBookTestInfo.links)
    provideJsonResponse(OK, bulkResponse(List(book1response, book2response)))

    whenReady(service.getBulkBookInfo(List(TestBookIsbn, SecondTestBookIsbn))) { res =>
      assert(res == BulkBookInfo(2, List(TestBookInfo, SecondBookTestInfo)))
    }
  }

  it should "return catalogue info for multiple valid contributor IDs" in new TestFixture {
    val contributor1response = contributorResponseJson(TestContributorInfo.id, TestContributorInfo.displayName, TestContributorInfo.sortName)
    val contributor2response = contributorResponseJson(TestContributor2Info.id, TestContributor2Info.displayName, TestContributor2Info.sortName)
    provideJsonResponse(OK, bulkResponse(List(contributor1response, contributor2response)))

    whenReady(service.getBulkContributorInfo(List(TestContributorId, TestContributor2Id))) { res =>
      assert(res == BulkContributorInfo(2, List(TestContributorInfo, TestContributor2Info)))
    }
  }

  it should "throw CatalogueInfoMissingException if the response does not have contributor link" in new TestFixture {
    provideJsonResponse(OK, bookResponse(TestBookInfo.id, TestBookInfo.title, TestBookInfo.images, TestBookInfo.links.filterNot(_.rel.endsWith("contributor"))))

    val ex = failingWith[CatalogueInfoMissingException](service.getInfoFor(TestBookIsbn))
    assert(ex.getMessage == s"Contributor missing for $TestBookIsbn")
  }

  it should "throw CatalogueInfoMissingException if the response does not have a cover image link" in new TestFixture {
    provideJsonResponse(OK, bookResponse(TestBookInfo.id, TestBookInfo.title, List.empty[v1.Image], TestBookInfo.links))

    val ex = failingWith[CatalogueInfoMissingException](service.getInfoFor(TestBookIsbn))
    assert(ex.getMessage == s"Cover image missing for $TestBookIsbn")
  }

  it should "throw CatalogueInfoMissingException if the response does not have sample ePub link" in new TestFixture {
    provideJsonResponse(OK, bookResponse(TestBookInfo.id, TestBookInfo.title, TestBookInfo.images, TestBookInfo.links.filterNot(_.rel.endsWith("samplemedia"))))

    val ex = failingWith[CatalogueInfoMissingException](service.getInfoFor(TestBookIsbn))
    assert(ex.getMessage == s"Sample ePub missing for $TestBookIsbn")
  }
}

class TestFixture extends MockitoSyrup with CatalogueV1Responses {

  val clientConfig = ClientConfig(new URL("https://myfavoritewebsite.com"), 1.second)
  val mockSendReceive = mock[SendReceive]

  lazy val client = new SprayClient {
    override val config: ClientConfig = clientConfig
    override val system: ActorSystem = ActorSystem("test-system")
    override val ec: ExecutionContext = global

    override def doSendReceive(implicit refFactory: ActorRefFactory, ec: ExecutionContext): SendReceive = mockSendReceive
  }

  val service = new DefaultCatalogueV1Service(client)

  val TestBookIsbn = "9780007197545"
  val SecondTestBookIsbn = "9780107195745"
  val TestBookCoverImage = v1.Image("urn:blinkboxbooks:image:cover", "http://media.blinkboxbooks.com/9780/007/197/545/cover.png")
  val TestBookLinks = List(
    v1.Link("urn:blinkboxbooks:schema:contributor", "/service/catalogue/contributors/4809fa392bf32dcc92206f5cf30882611e05d97b", Some("Nikki Gemmell"), Some("urn:blinkboxbooks:id:contributor:4809fa392bf32dcc92206f5cf30882611e05d97b")),
    v1.Link("urn:blinkboxbooks:schema:synopsis", "/service/catalogue/books/9780007197545/synopsis", Some("Synopsis"), Some("urn:blinkboxbooks:id:synopsis:9780007197545")),
    v1.Link("urn:blinkboxbooks:schema:publisher", "/service/catalogue/publishers/479", Some("HarperCollins Publishers"), Some("urn:blinkboxbooks:id:publisher:479")),
    v1.Link("urn:blinkboxbooks:schema:bookpricelist", "/service/catalogue/prices?book=9780007197545", Some("Price"), None),
    v1.Link("urn:blinkboxbooks:schema:samplemedia", "http://internal-media.mobcastdev.com/9780/007/197/545/71d24849440f5ee414fd5e7f2dad2cbb.sample.epub", Some("Sample"), None)
  )
  val TestBookInfo = BookInfo(TestBookIsbn, "With My Body", List(TestBookCoverImage), TestBookLinks)
  val SecondBookTestInfo = BookInfo(SecondTestBookIsbn, "Moby Dick", List(TestBookCoverImage), TestBookLinks)

  val TestContributorId = "4809fa392bf32dcc92206f5cf30882611e05d97b"
  val TestContributor2Id = "8409fa392bf32dcc92206f5cf30882611e05db79"
  val TestContributorInfo = ContributorInfo(TestContributorId, "Nikki Gemmell", "Gemmell, Nikki")
  val TestContributor2Info = ContributorInfo(TestContributor2Id, "Author Name", "Name, Author")

  def provideJsonResponse(statusCode: StatusCode, content: String): Unit = {
    val resp = HttpResponse(statusCode, HttpEntity(`application/vnd.blinkboxbooks.data.v1+json`, content))
    when(mockSendReceive.apply(any[HttpRequest])).thenReturn(Future.successful(resp))
  }

  def provideJsonResponses(responses: List[(StatusCode, String)]): Unit = {
    require(responses.nonEmpty, "Need 1 or more responses")
    val resps = responses.map(t => HttpResponse(t._1, HttpEntity(`application/vnd.blinkboxbooks.data.v1+json`, t._2)))
    val ongoingStubbing = when(mockSendReceive.apply(any[HttpRequest]))
    resps.foldLeft(ongoingStubbing)((os, resp) => os.thenReturn(Future.successful(resp)))
  }

  def provideErrorResponse(e: Throwable): Unit =
    when(mockSendReceive.apply(any[HttpRequest])).thenReturn(Future.failed(e))
}

trait CatalogueV1Responses {

  import org.json4s.JsonDSL._
  import org.json4s.jackson.JsonMethods._

  def bookResponseJson(isbn: String, title: String, images: List[v1.Image], links: List[v1.Link]) = {
    ("type" -> "urn:blinkboxbooks:schema:book") ~
      ("guid" -> s"urn:blinkboxbooks:id:book:$isbn") ~
      ("id" -> isbn) ~
      ("title" -> title) ~
      ("publicationDate" -> "2014-11-17") ~
      ("sampleEligible" -> true) ~
      ("images" ->
        images.map { img =>
          ("rel" -> img.rel) ~ 
          ("src" -> img.src)
        }) ~
      ("links" ->
        links.map { l =>
          ("rel" -> l.rel) ~
          ("href" -> l.href) ~
          ("title" -> l.title) ~
          ("targetGuid" -> l.targetGuid)
        })
  }

  def bookResponse(isbn: String, title: String, images: List[v1.Image], links: List[v1.Link]): String =
    compact(render(bookResponseJson(isbn, title, images, links)))

  def contributorResponseJson(id: String, displayName: String, sortName: String): JValue = {
    ("type" -> "urn:blinkboxbooks:schema:contributor") ~
      ("guid" -> s"urn:blinkboxbooks:id:contributor:$id") ~
      ("id" -> id) ~
      ("displayName" -> displayName) ~
      ("sortName" -> sortName) ~
      ("bookCount" -> 1) ~
      ("biography" -> null) ~
      ("links" -> List(
        ("rel" -> "urn:blinkboxbooks:schema:books") ~
          ("href" -> s"/service/catalogue/books?contributor=$id") ~
          ("title" -> "Books for contributor")
        )
      )
  }

  def contributorResponse(id: String, displayName: String, sortName: String): String = {
    compact(render(contributorResponseJson(id, displayName, sortName)))
  }

  def bulkResponse(bookResponses: List[JValue]): String = {
    val json = ("type" -> "urn:blinkboxbooks:schema:list") ~
      ("numberOfResults" -> bookResponses.size) ~
      ("offset" -> "0") ~
      ("count" -> bookResponses.size) ~
      ("items" -> bookResponses)

    compact(render(json))
  }
}
