package com.blinkbox.books.reading.admin

import scala.concurrent.Future

trait LibraryAdminService {
  def addBook(isbn: String, userId: Int, ownership: Ownership): Future[Unit]
}

class DefaultLibraryAdminService extends LibraryAdminService {
  override def addBook(isbn: String, userId: Int, ownership: Ownership): Future[Unit] = ???
}

sealed trait Ownership
object Owned extends Ownership
object Sample extends Ownership

class LibraryItemConflict(msg: String, cause: Throwable = null) extends Exception(msg, cause)