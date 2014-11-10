package com.blinkbox.books.reading.common.persistence

import java.net.URI

import com.blinkbox.books.reading.common.CFI
import org.joda.time.DateTime

import scala.slick.ast.ColumnOption.DBType
import scala.slick.driver.JdbcProfile
import com.blinkbox.books.slick.TablesContainer

import scala.slick.lifted

trait LibraryTables[Profile <: JdbcProfile] extends TablesContainer[Profile] {

  import driver.simple._

  implicit lazy val cfiColumnType = MappedColumnType.base[CFI, String](_.value, CFI)
  implicit lazy val urlColumnType = MappedColumnType.base[URI, String](_.toString, new URI(_))
  implicit lazy val MediaTypeColumnType = MappedColumnType.base[MediaType, String](
    {
      case EpubKey => "EpubKey"
      case FullEpub => "FullEpub"
    },
    {
      case "EpubKey" => EpubKey
      case "FullEpub" => FullEpub
    }
  )

  class LibraryItems(tag: Tag) extends Table[LibraryItem](tag, "library_items") {

    def isbn = column[String]("isbn", DBType("CHAR(13)"))
    def userId = column[Int]("user_id", O.NotNull)
    def sample = column[Boolean]("sample", O.NotNull)
    def progressCfi = column[CFI]("progress_cfi")
    def progressPercentage = column[Int]("progress_percentage")
    def createdAt = column[DateTime]("created_at")
    def updatedAt = column[DateTime]("updated_at")
    def pk = primaryKey("library_items_id", (isbn, userId))

    def * = (isbn, userId, sample, progressCfi, progressPercentage, createdAt, updatedAt) <> (LibraryItem.tupled, LibraryItem.unapply)
  }

  class LibraryItemMedia(tag: Tag) extends Table[LibraryItemLink](tag, "library_item_links") {
    def isbn = column[String]("isbn", DBType("CHAR(13)"))
    def linkType = column[MediaType]("media_type")
    def uri = column[URI]("uri")
    def pk = primaryKey("library_item_links_isbn", (isbn, linkType))

    def * = (isbn, linkType, uri) <> (LibraryItemLink.tupled, LibraryItemLink.unapply)
  }

  lazy val libraryItems = TableQuery[LibraryItems]
  lazy val libraryItemMedia = TableQuery[LibraryItemMedia]

  private def getLibraryItem(userId: Column[Int], isbn: Column[String]): lifted.Query[LibraryItems, LibraryItem, Seq] =
    libraryItems.filter(b => b.isbn === isbn && b.userId === userId)

  private def getLibraryItemMedia(isbn: Column[String]): lifted.Query[LibraryItemMedia, LibraryItemLink, Seq] =
    libraryItemMedia.filter(_.isbn === isbn)

  lazy val getLibraryItemBy = Compiled(getLibraryItem _)
  lazy val getLibraryItemLinkFor = Compiled(getLibraryItemMedia _)
}

object LibraryTables {
  def apply[Profile <: JdbcProfile](_driver: Profile) = new LibraryTables[Profile] {
    override val driver = _driver
  }
}
