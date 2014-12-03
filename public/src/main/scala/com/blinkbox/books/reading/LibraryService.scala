package com.blinkbox.books.reading

import com.blinkbox.books.auth.User
import com.blinkbox.books.clients.catalogue._
import com.blinkbox.books.reading.persistence.{LibraryItem, LibraryStore}
import com.blinkbox.books.spray.v2.{Image, Link}
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait LibraryService {
  def getBook(isbn: String)(implicit user: User): Future[Option[BookDetails]]
  def getLibrary(count: Int, offset: Int)(implicit user: User): Future[List[BookDetails]]
  def getSamples(count: Int, offset: Int)(implicit user: User): Future[List[BookDetails]]
  def addSample(isbn: String)(implicit user: User): Future[SampleResult]
}

class DefaultLibraryService(
  libraryStore: LibraryStore,
  catalogueService: CatalogueService) extends LibraryService with StrictLogging {

  override def getLibrary(count: Int, offset: Int)(implicit user: User): Future[List[BookDetails]] = for {
    library <- libraryStore.getLibrary(count, offset, user.id)
    isbns = library.map(_.isbn)
    itemMediaLinks <- libraryStore.getBooksMedia(isbns, user.id)
    catalogueInfo <- catalogueService.getBulkInfoFor(isbns, user.id)
    list = library.map { b => buildBookDetails(b, itemMediaLinks.get(b.isbn).get, catalogueInfo.filter(c => c.id == b.isbn).head) }
  } yield list

  override def getSamples(count: Int, offset: Int)(implicit user: User): Future[List[BookDetails]] = for {
    library <- libraryStore.getSamples(count, offset, user.id)
    isbns = library.map(_.isbn)
    itemMediaLinks <- libraryStore.getBooksMedia(isbns, user.id)
    catalogueInfo <- catalogueService.getBulkInfoFor(isbns, user.id)
    list = library.map { b => buildBookDetails(b, itemMediaLinks.get(b.isbn).get, catalogueInfo.filter(c => c.id == b.isbn).head) }
  } yield list

  override def addSample(isbn: String)(implicit user: User): Future[SampleResult] =
    catalogueService.getInfoFor(isbn).flatMap( _ =>
      libraryStore.getBook(isbn, user.id).flatMap {
      case Some(item) =>
        if (item.ownership == Sample) Future.successful(SampleAlreadyExists)
        else throw LibraryConflictException(s"Sample cannot be added as $isbn exists as a full book")
      case None =>
        libraryStore.addSample(isbn, user.id).map( _ => SampleAdded)
    }).recover {
      case e: CatalogueInfoMissingException =>
        val message = s"$isbn was not found in the Catalogue Service when adding sample"
        logger.error(message, e)
        throw new BadRequestException(message, e)
    }

  override def getBook(isbn: String)(implicit user: User): Future[Option[BookDetails]] = {
    val libItemFuture = libraryStore.getBook(isbn, user.id)
    val itemMediaLinksFuture = libraryStore.getBookMedia(isbn)
    val catalogueInfoFuture = catalogueService.getInfoFor(isbn)
    for {
      libItem <- libItemFuture
      itemMediaLinks <- itemMediaLinksFuture
      catalogueInfo <- catalogueInfoFuture
    } yield buildBookDetailsOptional(libItem, itemMediaLinks, catalogueInfo)
  }

  def buildBookDetails(libItem: LibraryItem, libraryMediaLinks: List[Link], catalogueInfo: CatalogueInfo): BookDetails = {
    val readingPosition = ReadingPosition(libItem.progressCfi, libItem.progressPercentage)
    val images = List(Image(CoverImage, catalogueInfo.coverImageUrl))
    val catalogueLinks = List(Link(SampleEpub, catalogueInfo.sampleEpubUrl))
    val links = catalogueLinks ++ libraryMediaLinks

    BookDetails(
      libItem.isbn,
      catalogueInfo.title,
      catalogueInfo.author,
      catalogueInfo.sortableAuthor,
      libItem.createdAt,
      libItem.ownership,
      libItem.readingStatus,
      readingPosition,
      images,
      links)
  }

  def buildBookDetailsOptional(libItem: Option[LibraryItem], libraryMediaLinks: List[Link], catalogueInfo: CatalogueInfo): Option[BookDetails] =
    libItem.map(item => buildBookDetails(item, libraryMediaLinks, catalogueInfo))
}
