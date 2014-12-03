package com.blinkbox.books.reading.admin

import com.blinkbox.books.clients.catalogue.CatalogueService
import com.blinkbox.books.reading.Ownership
import com.blinkbox.books.reading.persistence.{LibraryItem, LibraryStore}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait LibraryAdminService {
  def addBook(isbn: String, userId: Int, ownership: Ownership): Future[Unit]
}

class DefaultLibraryAdminService(libraryStore: LibraryStore, catalogueService: CatalogueService) extends LibraryAdminService {

  def allowAdminUpdate: (LibraryItem, Ownership) => Boolean = (item, ownership) =>
    if (ownership <= item.ownership) throw new LibraryItemConflict(s"User ${item.userId} already has ${item.isbn} in library with the same or lower ownership type ($ownership)")
    else true

  override def addBook(isbn: String, userId: Int, ownership: Ownership): Future[Unit] = for {
    _ <- catalogueService.getInfoFor(isbn) // check the book is in catalogue before adding it to the library
    _ <- libraryStore.addBook(isbn, userId, ownership, allowAdminUpdate)
  } yield ()
}

class LibraryItemConflict(msg: String, cause: Throwable = null) extends Exception(msg, cause)