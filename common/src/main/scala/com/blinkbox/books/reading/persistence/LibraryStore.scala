package com.blinkbox.books.reading.persistence

import com.blinkbox.books.config.DatabaseConfig
import com.blinkbox.books.reading.Sample
import com.blinkbox.books.slick.{DatabaseComponent, DatabaseSupport, MySQLDatabaseSupport, TablesContainer}
import com.blinkbox.books.spray.v2.Link
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.slick.driver.MySQLDriver
import scala.slick.jdbc.JdbcBackend.Database

trait LibraryStore {
  def getBook(isbn: String, userId: Int): Future[Option[LibraryItem]]
  def getBookMedia(isbn: String): Future[List[Link]]
  def getBooksMedia(isbns: List[String], userId: Int): Future[Map[String, List[Link]]]
  def getLibrary(count: Int, offset: Int, userId: Int): Future[List[LibraryItem]]
  def getSamples(count: Int, offset: Int, userId: Int): Future[List[LibraryItem]]
  def addSample(isbn: String, userId: Int): Future[Unit]
}

class DbLibraryStore[DB <: DatabaseSupport](db: DB#Database, tables: LibraryTables[DB#Profile], exceptionFilter: DB#ExceptionFilter)(implicit val ec: ExecutionContext) extends LibraryStore with StrictLogging {

  import tables._
  import driver.simple._

  override def getBook(isbn: String, userId: Int): Future[Option[LibraryItem]] = Future {
    db.withSession { implicit session =>
      tables.getLibraryItemBy(userId, isbn).firstOption
    }
  }

  override def getBookMedia(isbn: String): Future[List[Link]] = Future {
    db.withSession { implicit session =>
      val links = tables.getLibraryItemLinkFor(isbn).list
      if (links.isEmpty) throw new LibraryMediaMissingException(s"media (full ePub & key URLs) for $isbn does not exist")
      else links.map(l => Link(l.mediaType, l.uri))
    }
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
      tables.getUserLibraryByBookTypeWithId(count, offset, userId, Sample).list
    }
  }

  override def addSample(isbn: String, userId: Int): Future[Unit] = Future {
    db.withSession { implicit session =>
      tables.addSample(isbn, userId)
    }
  }
}

class LibraryMediaMissingException(msg: String) extends Exception(msg, null)

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