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

  override def addBook(isbn: String, userId: Int, ownership: Ownership): Future[Unit] =
    catalogueService.getInfoFor(isbn).flatMap(_ => libraryStore.addOrUpdateLibraryItem(isbn, userId, ownership, allowAdminUpdate).transform(identity => Unit, {
      case e: DbStoreUpdateFailedException => new LibraryItemConflict(s"User ${userId} already has ${isbn} in library with the same or lower ownership type ($ownership)")
    }))
}

class LibraryItemConflict(msg: String, cause: Throwable = null) extends Exception(msg, cause)