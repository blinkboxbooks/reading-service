package com.blinkbox.books.reading.common.persistence

import com.blinkbox.books.config.DatabaseConfig
import com.blinkbox.books.reading.common.Link
import com.blinkbox.books.slick.{DatabaseComponent, DatabaseSupport, MySQLDatabaseSupport, TablesContainer}
import com.typesafe.scalalogging.slf4j.StrictLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.slick.driver.MySQLDriver
import scala.slick.jdbc.JdbcBackend.Database

trait LibraryStore {
  def getBook(userId: Int, isbn: String): Future[Option[LibraryItem]]
  def getBookMedia(isbn: String): Future[Option[List[Link]]]
}

class DbLibraryStore[DB <: DatabaseSupport](db: DB#Database, tables: LibraryTables[DB#Profile], exceptionFilter: DB#ExceptionFilter)(implicit val ec: ExecutionContext) extends LibraryStore with StrictLogging {

  import tables._
  import driver.simple._

  override def getBook(userId: Int, isbn: String): Future[Option[LibraryItem]] = Future {
    db.withSession { implicit session =>
      tables.getLibraryItemBy(userId, isbn).firstOption
    }
  }

  override def getBookMedia(isbn: String): Future[Option[List[Link]]] = Future {
    db.withSession { implicit session =>
      val links = tables.getLibraryItemLinkFor(isbn).list
      if (links.isEmpty) None else Some(links.map(l => Link(l.`type`, l.uri)))
    }
  }
}

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