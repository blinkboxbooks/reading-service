package com.blinkbox.books.reading.common.persistence

import java.net.URI

import com.blinkbox.books.reading.common.{ReadingPosition, CFI}

import scala.slick.driver.JdbcProfile
import com.blinkbox.books.slick.TablesContainer

import scala.slick.lifted
import scala.slick.lifted.MappedProjection

trait LibraryTables[Profile <: JdbcProfile] extends TablesContainer[Profile] {

  import driver.simple._

  implicit lazy val cfiColumnType = MappedColumnType.base[CFI, String](_.value, CFI)
  implicit lazy val urlColumnType = MappedColumnType.base[URI, String](_.toString, new URI(_))

  class LibraryItems(tag: Tag) extends Table[LibraryItem](tag, "library_items") {

    def isbn = column[String]("isbn", O.NotNull)
    def userId = column[Int]("user_id", O.NotNull)
    def sample = column[Boolean]("sample", O.NotNull)
    def cfi = column[Option[CFI]]("cfi")
    def progress = column[Option[Int]]("progress")
    def pk = primaryKey("library_items_id", (isbn, userId))

    def * = (isbn, userId, sample, cfi, progress) <> (LibraryItem.tupled, LibraryItem.unapply)
  }

  lazy val libraryItems = TableQuery[LibraryItems]

  private def getLibraryItem(userId: Column[Int], isbn: Column[String]): lifted.Query[LibraryItems, LibraryItem, Seq] =
    libraryItems.filter(b => b.isbn === isbn && b.userId === userId)

  def getLibraryItemBy = Compiled(getLibraryItem _)
}

object LibraryTables {
  def apply[Profile <: JdbcProfile](_driver: Profile) = new LibraryTables[Profile] {
    override val driver = _driver
  }
}
