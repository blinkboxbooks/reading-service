package com.blinkbox.books.clients.catalogue

import java.net.URI

import com.blinkbox.books.spray.v1
import com.blinkbox.books.spray.v1.Version1JsonSupport
import spray.httpx.RequestBuilding.Get

import scala.concurrent.{ExecutionContext, Future}

case class CatalogueInfo(id: String, title: String, author: String, sortableAuthor: String, coverImageUrl: URI, sampleEpubUrl: URI)
case class ContributorInfo(id: String, displayName: String, sortName: String)
case class BookInfo(id: String, title: String, images: List[v1.Image], links: List[v1.Link])
case class BulkCatalogueInfo(`type`: String, numberOfResults: Int, offset: Int, count: Int, items: List[BookInfo])
case class BulkContributorInfo(numberOfResults: Int, items: List[ContributorInfo])
case class BulkBookInfo(numberOfResults: Int, items: List[BookInfo])

trait CatalogueService {
  def getInfoFor(isbn: String): Future[CatalogueInfo]
  def getBulkInfoFor(isbns: List[String]): Future[List[CatalogueInfo]]
}

trait CatalogueV1Service {
  def getBookInfo(isbn: String): Future[BookInfo]
  def getBulkBookInfo(isbns: List[String]): Future[BulkBookInfo]
  def getContributorInfo(contributorId: String): Future[ContributorInfo]
  def getBulkContributorInfo(contributorIds: List[String]): Future[BulkContributorInfo]
}

class DefaultCatalogueV1Service(client: Client)(implicit ec: ExecutionContext) extends CatalogueService with CatalogueV1Service with Version1JsonSupport {

  override def getInfoFor(isbn: String): Future[CatalogueInfo] = for {
    bookInfo <- getBookInfo(isbn)
    contributorId = extractContributorId(bookInfo.links) getOrElse { throw new CatalogueInfoMissingException(s"Contributor missing for $isbn") }
    coverImageUrl = extractCoverImageUrl(bookInfo.images) getOrElse { throw new CatalogueInfoMissingException(s"Cover image missing for $isbn") }
    sampleEpubUrl = extractSampleEpubUrl(bookInfo.links) getOrElse { throw new CatalogueInfoMissingException(s"Sample ePub missing for $isbn") }
    contributorInfo <- getContributorInfo(contributorId)
  } yield CatalogueInfo(isbn, bookInfo.title, contributorInfo.displayName, contributorInfo.sortName, coverImageUrl, sampleEpubUrl)

  override def getBulkInfoFor(isbns: List[String]): Future[List[CatalogueInfo]] = getBulkBookInfo(isbns).flatMap(buildBulkCatalogueInfo(_))

  private def extractContributorId(links: List[v1.Link]): Option[String] =
    links.find(_.rel == "urn:blinkboxbooks:schema:contributor") flatMap { l =>
      l.targetGuid.map(_.split(":").last)
    }

  // TODO: if it is missing, will it be actually missing or have "Unknown" in src field as the docs seem to indicate?
  private def extractCoverImageUrl(images: List[v1.Image]): Option[URI] =
    images.find(_.rel == "urn:blinkboxbooks:image:cover").map(l => new URI(l.src))

  // TODO: if it is missing, will it be actually missing or have "Unknown" in href field as the docs seem to indicate?
  private def extractSampleEpubUrl(links: List[v1.Link]): Option[URI] =
    links.find(_.rel == "urn:blinkboxbooks:schema:samplemedia").map(l => new URI(l.href))

  override def getBookInfo(isbn: String): Future[BookInfo] = {
    val req = Get(s"${client.config.url}/catalogue/books/$isbn")
    client.dataRequest[BookInfo](req, credentials = None).transform(identity, {
      case e: NotFoundException =>
        new CatalogueInfoMissingException(s"Catalogue does not have a book with isbn: $isbn", e)
    })
  }

  def getBulkBookInfo(isbns: List[String]): Future[BulkBookInfo] = {
    if (isbns.isEmpty) { Future.successful(BulkBookInfo(0, List.empty[BookInfo])) }
    else {
      val isbnQueryString = isbns.mkString(start = "id=", sep = "&id=", end = "")
      val req = Get(s"${client.config.url}/catalogue/books?$isbnQueryString")
      client.dataRequest[BulkBookInfo](req, credentials = None).transform(identity, {
        case e: NotFoundException =>
          new CatalogueInfoMissingException(s"Catalogue does not have a book with the following isbns: $isbns", e)
      })
    }
  }

  override def getContributorInfo(contributorId: String): Future[ContributorInfo] = {
    val req = Get(s"${client.config.url}/catalogue/contributors/$contributorId")
    client.dataRequest[ContributorInfo](req, credentials = None).transform(identity, {
      case e: NotFoundException =>
        new CatalogueInfoMissingException(s"Catalogue does not have a contributor with id: $contributorId", e)
    })
  }

  override def getBulkContributorInfo(contributorIds: List[String]): Future[BulkContributorInfo] = {
    val queryString = contributorIds.map(id => s"id=${id}").foldRight("")((a,b) => s"${a}&${b}")
    val req = Get(s"${client.config.url}/catalogue/contributors?$queryString")
    client.dataRequest[BulkContributorInfo](req, credentials = None).transform(identity, {
      case e: NotFoundException =>
        new CatalogueInfoMissingException(s"Catalogue does not have a contributor with id: $contributorIds", e)
    })
  }

  private def buildBulkCatalogueInfo(bulkBookInfo: BulkBookInfo): Future[List[CatalogueInfo]] = {
    val extractContId: (BookInfo) => String = b => extractContributorId(b.links) getOrElse { throw new CatalogueInfoMissingException(s"Contributor missing for ${b.id}") }
    val extractCoverImgUrl: (BookInfo) => URI = b => extractCoverImageUrl(b.images) getOrElse { throw new CatalogueInfoMissingException(s"Cover image missing for $b.id") }
    val extractSampleUrl: (BookInfo) => URI = b => extractSampleEpubUrl(b.links) getOrElse { throw new CatalogueInfoMissingException(s"Sample ePub missing for ${b.id}") }
    val extractContributorDisplayName: (BookInfo, List[ContributorInfo]) => String = (b, c) => c.find(c => c.id == extractContId(b)).get.displayName
    val contributorIds = bulkBookInfo.items.map(extractContId)
    for {
      contributorIds <- getBulkContributorInfo(contributorIds)
      list = bulkBookInfo.items.map(b => CatalogueInfo(b.id, b.title, extractContributorDisplayName(b, contributorIds.items), extractContributorDisplayName(b, contributorIds.items), extractCoverImgUrl(b), extractSampleUrl(b)))
    } yield list
  }
}

class CatalogueInfoMissingException(msg: String, cause: Throwable = null) extends Exception(msg, cause)