package com.blinkbox.books.reading.common.persistence

import java.net.URI

import com.blinkbox.books.reading.common.{Link, ReadingPosition, Book, CFI}

import scala.slick.driver.JdbcProfile
import com.blinkbox.books.slick.TablesContainer


trait LibraryTables[Profile <: JdbcProfile] extends TablesContainer[Profile] {

  import driver.simple._

  implicit lazy val cfiColumnType = MappedColumnType.base[CFI, String](_.value, CFI)
  implicit lazy val urlColumnType = MappedColumnType.base[URI, String](_.toString, new URI(_))

  lazy val books = TableQuery[Books]

  class Books(tag: Tag) extends Table[Book](tag, "library_items") {

    def isbn = column[String]("isbn", O.NotNull)
    def userId = column[Int]("user_id", O.NotNull)
    def sample = column[Boolean]("sample", O.NotNull)
    def cfi = column[CFI]("cfi")
    def readingPercentage = column[Int]("reading_percentage")

    def pk = primaryKey("library_items_id", (isbn, userId))

    def readingPosition = (cfi, readingPercentage) <> (ReadingPosition.tupled, ReadingPosition.unapply)

    def * = (isbn, userId, sample, readingPosition) <> (Book.tupled, Book.unapply)
  }

  class Links(tag: Tag) extends Table[Link](tag, "tags") {

    def isbn = column[String]("isbn", O.NotNull)
    def userId = column[Int]("user_id", O.NotNull)
    def name = column[String]("name", O.NotNull)
    def link = column[URI]("link", O.NotNull)

    def bookForeignKey = (isbn, userId)

    def book = foreignKey("book_fk", (isbn, userId), books)(t => (t.isbn, t.userId))

    override def * = (isbn, name, link) <> (Link.tupled, Link.unapply)
  }

}
