package com.blinkbox.books.reading.admin

import com.blinkbox.books.clients.catalogue.CatalogueService
import com.blinkbox.books.reading.Ownership
import com.blinkbox.books.reading.persistence.{DbStoreUpdateFailedException, LibraryItem, LibraryStore}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait LibraryAdminService {
  def addBook(isbn: String, userId: Int, ownership: Ownership): Future[Unit]
}

class DefaultLibraryAdminService(libraryStore: LibraryStore, catalogueService: CatalogueService) extends LibraryAdminService {

  val allowAdminUpdate: (LibraryItem, Ownership) => Boolean = (item, ownership) => (ownership > item.ownership)

  override def addBook(isbn: String, userId: Int, ownership: Ownership): Future[Unit] = for {
    _ <- catalogueService.getInfoFor(isbn) // check the book is in catalogue before adding it to the library
    _ <- libraryStore.addLibraryItem(isbn, userId, ownership, allowAdminUpdate).transform(identity, {
      case e: DbStoreUpdateFailedException => new LibraryItemConflict(s"User ${userId} already has ${isbn} in library with the same or lower ownership type ($ownership)")
    })
  } yield ()
}

class LibraryItemConflict(msg: String, cause: Throwable = null) extends Exception(msg, cause)