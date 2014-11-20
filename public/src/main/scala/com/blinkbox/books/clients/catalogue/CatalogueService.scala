package com.blinkbox.books.clients.catalogue

import java.net.URI

import com.blinkbox.books.spray.v1
import com.blinkbox.books.spray.v1.Version1JsonSupport
import spray.httpx.RequestBuilding.Get

import scala.concurrent.{ExecutionContext, Future}

case class CatalogueInfo(title: String, author: String, sortableAuthor: String, coverImageUrl: URI, sampleEpubUrl: URI)
case class ContributorInfo(displayName: String, sortName: String)
case class BookInfo(title: String, images: List[v1.Image], links: List[v1.Link])

trait CatalogueService {
  def getInfoFor(isbn: String): Future[CatalogueInfo]
}

trait CatalogueV1Service {
  def getBookInfo(isbn: String): Future[BookInfo]
  def getContributorInfo(contributorId: String): Future[ContributorInfo]
}

class DefaultCatalogueV1Service(client: Client)(implicit ec: ExecutionContext) extends CatalogueService with CatalogueV1Service with Version1JsonSupport {

  override def getInfoFor(isbn: String): Future[CatalogueInfo] = for {
    bookInfo <- getBookInfo(isbn)
    contributorId = extractContributorId(bookInfo.links) getOrElse { throw new CatalogueInfoMissingException(s"Contributor missing for $isbn") }
    coverImageUrl = extractCoverImageUrl(bookInfo.images) getOrElse { throw new CatalogueInfoMissingException(s"Cover image missing for $isbn") }
    sampleEpubUrl = extractSampleEpubUrl(bookInfo.links) getOrElse { throw new CatalogueInfoMissingException(s"Sample ePub missing for $isbn") }
    contributorInfo <- getContributorInfo(contributorId)
  } yield CatalogueInfo(bookInfo.title, contributorInfo.displayName, contributorInfo.sortName, coverImageUrl, sampleEpubUrl)

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

  override def getContributorInfo(contributorId: String): Future[ContributorInfo] = {
    val req = Get(s"${client.config.url}/catalogue/contributors/$contributorId")
    client.dataRequest[ContributorInfo](req, credentials = None).transform(identity, {
      case e: NotFoundException =>
        new CatalogueInfoMissingException(s"Catalogue does not have a contributor with id: $contributorId", e)
    })
  }
}

class CatalogueInfoMissingException(msg: String, cause: Throwable = null) extends Exception(msg, cause)