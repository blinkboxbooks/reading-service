package com.blinkbox.books.reading

import com.blinkbox.books.clients.catalogue.{CatalogueInfo, CatalogueService}
import com.blinkbox.books.reading.common._
import com.blinkbox.books.reading.common.persistence.{LibraryItem, LibraryStore}
import com.blinkbox.books.spray.v2.Link
import com.typesafe.scalalogging.slf4j.StrictLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait LibraryService {
  def getBook(isbn: String, userId: Int): Future[Option[BookDetails]]
}

class DefaultLibraryService(
  libraryStore: LibraryStore,
  catalogueService: CatalogueService) extends LibraryService with StrictLogging {

  override def getBook(isbn: String, userId: Int): Future[Option[BookDetails]] = {
    val libItemFuture = libraryStore.getBook(userId, isbn)
    val itemMediaLinksFuture = libraryStore.getBookMedia(isbn)
    val catalogueInfoFuture = catalogueService.getInfoFor(isbn)
    for {
      libItem <- libItemFuture
      itemMediaLinks <- itemMediaLinksFuture
      catalogueInfo <- catalogueInfoFuture
    } yield buildBookDetailsOptional(libItem, itemMediaLinks, catalogueInfo)
  }

  def buildBookDetailsOptional(libItem: Option[LibraryItem], libraryMediaLinks: List[Link], catalogueInfo: CatalogueInfo): Option[BookDetails] =
    libItem map { item =>
      val readingPosition = ReadingPosition(item.progressCfi, item.progressPercentage)
      val images = List(Image(CoverImage, catalogueInfo.coverImageUrl))
      val catalogueLinks = List(Link(SampleEpub, catalogueInfo.sampleEpubUrl))
      val links = catalogueLinks ++ libraryMediaLinks

      BookDetails(
        item.isbn,
        catalogueInfo.title,
        catalogueInfo.sortableTitle,
        catalogueInfo.author,
        item.createdAt,
        item.`type`,
        item.readingStatus,
        readingPosition,
        images,
        links)
    }
}
