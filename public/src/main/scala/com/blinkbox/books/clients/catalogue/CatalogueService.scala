package com.blinkbox.books.clients.catalogue

import java.net.URI

import scala.concurrent.Future

case class CatalogueInfo(coverImageUrl: URI, sampleEpubUrl: URI)

trait CatalogueService {
  def getInfoFor(isbn: String): Future[Option[CatalogueInfo]]
}

class CatalogueServiceClient extends CatalogueService {
  override def getInfoFor(isbn: String): Future[Option[CatalogueInfo]] = ???
}