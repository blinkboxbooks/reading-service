package com.blinkbox.books.reading

import java.net.URI

import com.blinkbox.books.clients.catalogue.{BookDetails, CatalogueService}
import com.blinkbox.books.reading.common.{ReadingPosition, Book}
import com.blinkbox.books.reading.common.persistence.{LibraryItem, LibraryStore}
import com.typesafe.scalalogging.slf4j.StrictLogging

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

trait LibraryService {
  def getBook(isbn: String, userId: Int): Future[Option[Book]]
}

class DefaultLibraryService(
  libraryStore: LibraryStore,
  catalogueService: CatalogueService) extends LibraryService with StrictLogging {

  override def getBook(isbn: String, userId: Int): Future[Option[Book]] = for {
      libItem <- libraryStore.getBook(userId, isbn)
      bookDetails <- catalogueService.getBookDetails(isbn)
  } yield buildBookOptional(libItem, bookDetails)

  def buildBook(libItem: LibraryItem, bookDetails: BookDetails): Book = {
    val readingPosition = ReadingPosition(libItem.progressCfi, libItem.progressPercentage)
    val links = Map.empty[String, URI]
    Book(libItem.isbn, libItem.sample, readingPosition, links)
  }

  def buildBookOptional = lift(buildBook)

  def lift[A, B, C](f: (A, B) => C): (Option[A], Option[B]) => Option[C] = (a: Option[A], b: Option[B]) => map2(a, b)(f)

  def map2[A, B, C](a: Option[A], b: Option[B])(f: (A, B) => C): Option[C] =
    for {
      aa <- a
      bb <- b
    } yield f(aa, bb)
}
