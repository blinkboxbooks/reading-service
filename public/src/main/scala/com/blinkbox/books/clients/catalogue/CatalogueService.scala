package com.blinkbox.books.clients.catalogue

import java.net.URI

import scala.concurrent.Future

case class BookDetails(coverImage: URI)

trait CatalogueService {
  def getBookDetails(isbn: String): Future[Option[BookDetails]]
}

class CatalogueServiceClient extends CatalogueService {
  override def getBookDetails(isbn: String): Future[Option[BookDetails]] = ???
}