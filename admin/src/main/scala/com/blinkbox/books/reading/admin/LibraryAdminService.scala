package com.blinkbox.books.reading.admin

import com.blinkbox.books.clients.catalogue.CatalogueService
import com.blinkbox.books.reading.Ownership
import com.blinkbox.books.reading.persistence.LibraryStore

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait LibraryAdminService {
  def addBook(isbn: String, userId: Int, ownership: Ownership): Future[Unit]
}

class DefaultLibraryAdminService(libraryStore: LibraryStore, catalogueService: CatalogueService) extends LibraryAdminService {

  override def addBook(isbn: String, userId: Int, ownership: Ownership): Future[Unit] = for {
    _ <- catalogueService.getInfoFor(isbn)
    _ <- libraryStore.addBook(isbn, userId, ownership)
  } yield ()
}