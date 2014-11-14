package com.blinkbox.books.clients.catalogue

import java.net.URI

import scala.concurrent.Future

case class CatalogueInfo(title: String, sortableTitle: String, author: String, coverImageUrl: URI, sampleEpubUrl: URI)

trait CatalogueService {
  def getInfoFor(isbn: String): Future[CatalogueInfo]
}

class CatalogueServiceClient extends CatalogueService {
  override def getInfoFor(isbn: String): Future[CatalogueInfo] = ???
}

class CatalogueInfoMissingException(msg: String) extends Exception(msg, null)