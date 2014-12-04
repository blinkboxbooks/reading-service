package com.blinkbox.books.reading.persistence

import java.net.URI

import com.blinkbox.books.config.DatabaseConfig
import com.blinkbox.books.reading._
import com.blinkbox.books.slick.{DatabaseComponent, DatabaseSupport, MySQLDatabaseSupport, TablesContainer}
import com.blinkbox.books.spray.v2.Link
import com.blinkbox.books.time.Clock
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.slick.driver.MySQLDriver
import scala.slick.jdbc.JdbcBackend.Database

trait LibraryStore {
  def addOrUpdateLibraryItem(isbn: String, userId: Int, bookOwnership: Ownership, allowUpdate: (LibraryItem, Ownership) => Boolean): Future[DbAddStatus]
  def getLibraryItem(isbn: String, userId: Int): Future[Option[LibraryItem]]
  def getBookMedia(isbn: String): Future[List[Link]]
  def getBooksMedia(isbns: List[String], userId: Int): Future[Map[String, List[Link]]]
  def getLibrary(count: Int, offset: Int, userId: Int): Future[List[LibraryItem]]
  def getSamples(count: Int, offset: Int, userId: Int): Future[List[LibraryItem]]
}

class DbLibraryStore[DB <: DatabaseSupport](db: DB#Database, tables: LibraryTables[DB#Profile], exceptionFilter: DB#ExceptionFilter)(implicit val ec: ExecutionContext, clock: Clock) extends LibraryStore with StrictLogging {

  import tables._
  import driver.simple._

  override def addOrUpdateLibraryItem(isbn: String, userId: Int, bookOwnership: Ownership, allowUpdate: (LibraryItem, Ownership) => Boolean): Future[DbAddStatus] = Future {
    val now = clock.now
    db.withTransaction { implicit session =>
      tables.getLibraryItemBy(userId, isbn).firstOption match {
        case Some(item) if (allowUpdate(item, bookOwnership)) =>
          val updatedItem = item.copy(ownership = bookOwnership).copy(updatedAt = now)
          tables.getLibraryItemBy(userId, isbn).update(updatedItem)
          ItemUpdated
        case Some(item) =>
          val errorMessage = s"Could not update book for user $userId from ownership with ${item.ownership} to ownership $bookOwnership for isbn $isbn"
          throw new DbStoreUpdateFailedException(errorMessage)
        case None =>
          val newItem = LibraryItem(isbn, userId, bookOwnership, NotStarted, progressCfi = None, progressPercentage = 0, now, now)
          tables.libraryItems += newItem
          ItemAdded
      }
    }
  }

  override def getLibraryItem(isbn: String, userId: Int): Future[Option[LibraryItem]] = Future {
    db.withSession { implicit session =>
      tables.getLibraryItemBy(userId, isbn).firstOption
    }
  }

  override def getBookMedia(isbn: String): Future[List[Link]] = Future.successful {
//    db.withSession { implicit session =>
//      val links = tables.getLibraryItemLinkFor(isbn).list
//      if (links.isEmpty) throw new LibraryMediaMissingException(s"media (full ePub & key URLs) for $isbn does not exist")
//      else links.map(l => Link(l.mediaType, l.uri))
//    }

    // TODO: This is a temporary stub that must be changed later down the line
      List[Link](
        Link(SampleEpub, new URI("http://example.com/sample")),
        Link(FullEpub, new URI("http://example.com/full"))
      )
  }

  override def getBooksMedia(isbns: List[String], userId: Int): Future[Map[String, List[Link]]] = Future {
    if (isbns.isEmpty) { Map.empty }
    else db.withSession { implicit session =>
      val links = tables.getBulkLibraryItemMedia(isbns).list
      if (links.isEmpty) throw new LibraryMediaMissingException(s"media (full ePub & key URLs) for $isbns does not exist")
      else {
        val map = links.groupBy(_.isbn).map { case (k, list) => ( k -> list.map(l => Link(l.mediaType, l.uri)))}
        if (map.size < isbns.size) {
          val errorMessage = s"Cannot find media links for all the books that belong to userId ${userId}"
          logger.error(errorMessage)
          throw new LibraryMediaMissingException(errorMessage)
        }
        map
      }
    }
  }

  override def getLibrary(count: Int, offset: Int, userId: Int): Future[List[LibraryItem]] = Future {
    db.withSession { implicit session =>
      tables.getUserLibraryById(count, offset, userId).list
    }
  }

  override def getSamples(count: Int, offset: Int, userId: Int): Future[List[LibraryItem]] = Future {
    db.withSession { implicit session =>
      tables.getUserLibraryByOwnershipWithId(count, offset, userId, Sample).list
    }
  }

}

trait DbAddStatus
case object ItemAdded extends DbAddStatus
case object ItemUpdated extends DbAddStatus

class LibraryMediaMissingException(msg: String, cause: Throwable = null) extends Exception(msg, cause)

case class DbStoreUpdateFailedException(message: String) extends Exception(message)

class DefaultDatabaseComponent(config: DatabaseConfig) extends DatabaseComponent {

  override type Tables = TablesContainer[DB.Profile]

  override val driver = MySQLDriver
  override val DB = new MySQLDatabaseSupport
  // TODO: build using datasource
  override val db = Database.forURL(
    driver = "com.mysql.jdbc.Driver",
    url = config.jdbcUrl,
    user = config.user,
    password = config.pass)
  override val tables = LibraryTables[DB.Profile](driver)
}
