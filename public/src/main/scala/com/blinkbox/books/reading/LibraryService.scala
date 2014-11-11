package com.blinkbox.books.reading

import com.blinkbox.books.clients.catalogue.{CatalogueInfo, CatalogueService}
import com.blinkbox.books.reading.common._
import com.blinkbox.books.reading.common.persistence.{LibraryItem, LibraryStore}
import com.typesafe.scalalogging.slf4j.StrictLogging

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

trait LibraryService {
  def getBook(isbn: String, userId: Int): Future[Option[BookDetails]]
}

class DefaultLibraryService(
  libraryStore: LibraryStore,
  catalogueService: CatalogueService) extends LibraryService with StrictLogging {

  override def getBook(isbn: String, userId: Int): Future[Option[BookDetails]] = for {
      libItem <- libraryStore.getBook(userId, isbn)
      itemMediaLinks <- libraryStore.getBookMedia(isbn)
      catalogueInfo <- catalogueService.getInfoFor(isbn)
  } yield buildBookDetailsOptional(libItem, itemMediaLinks, catalogueInfo)

  def buildBookDetails(libItem: LibraryItem, itemMediaLinks: List[Link], catalogueInfo: CatalogueInfo): BookDetails = {
    val readingPosition = ReadingPosition(libItem.progressCfi, libItem.progressPercentage)
    val linksFromCatalogue = List(Link(CoverImage, catalogueInfo.coverImageUrl), Link(SampleEpub, catalogueInfo.sampleEpubUrl))
    val links = linksFromCatalogue ++ itemMediaLinks
    BookDetails(libItem.isbn, libItem.createdAt, libItem.sample, readingPosition, links)
  }

  def buildBookDetailsOptional = lift(buildBookDetails)

  private def lift[A, B, C, D](f: (A, B, C) => D): (Option[A], Option[B], Option[C]) => Option[D] = (a: Option[A], b: Option[B], c: Option[C]) => map3(a, b, c)(f)

  private def map3[A, B, C, D](a: Option[A], b: Option[B], c: Option[C])(f: (A, B, C) => D): Option[D] =
    for {
      aa <- a
      bb <- b
      cc <- c
    } yield f(aa, bb, cc)
}
