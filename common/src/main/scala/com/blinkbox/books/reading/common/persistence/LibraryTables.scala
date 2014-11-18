package com.blinkbox.books.reading.common.persistence

import java.net.URI

import com.blinkbox.books.reading.common._
import com.blinkbox.books.slick.TablesContainer
import org.joda.time.DateTime

import scala.slick.ast.ColumnOption.DBType
import scala.slick.driver.JdbcProfile
import scala.slick.lifted

trait LibraryTables[Profile <: JdbcProfile] extends TablesContainer[Profile] {

  import driver.simple._

  implicit lazy val cfiColumnType = MappedColumnType.base[Cfi, String](_.value, Cfi)
  implicit lazy val urlColumnType = MappedColumnType.base[URI, String](_.toString, new URI(_))
  implicit lazy val LibraryMediaTypeColumnType = MappedColumnType.base[LibraryMediaLinkType, Int](
    {
      case EpubKey => 1
      case FullEpub => 2
    },
    {
      case 1 => EpubKey
      case 2 => FullEpub
    }
  )
  implicit lazy val bookTypeColumnType = MappedColumnType.base[BookType, Int](
    {
      case Full => 1
      case Sample => 2
    },
    {
      case 1 => Full
      case 2 => Sample
    }
  )
  implicit lazy val readingStatusColumnType = MappedColumnType.base[ReadingStatus, Int](
    {
      case NotStarted => 1
      case Reading => 2
      case Finished => 3
    },
    {
      case 1 => NotStarted
      case 2 => Reading
      case 3 => Finished
    }
  )

  class LibraryItems(tag: Tag) extends Table[LibraryItem](tag, "library_item") {

    def isbn = column[String]("isbn", DBType("CHAR(13)"))
    def userId = column[Int]("user_id")
    def bookType = column[BookType]("book_type")
    def readingStatus = column[ReadingStatus]("reading_status")
    def progressCfi = column[Cfi]("progress_cfi")
    def progressPercentage = column[Int]("progress_percentage")
    def createdAt = column[DateTime]("created_at")
    def updatedAt = column[DateTime]("updated_at")
    def pk = primaryKey("pk_library_item", (isbn, userId))

    def * = (isbn, userId, bookType, readingStatus, progressCfi, progressPercentage, createdAt, updatedAt) <> (LibraryItem.tupled, LibraryItem.unapply)
  }

  class LibraryMedia(tag: Tag) extends Table[LibraryItemLink](tag, "library_media") {
    def isbn = column[String]("isbn", DBType("CHAR(13)"))
    def linkType = column[LibraryMediaLinkType]("media_type")
    def uri = column[URI]("uri")
    def createdAt = column[DateTime]("created_at")
    def updatedAt = column[DateTime]("updated_at")
    def pk = primaryKey("pk_library_media", (isbn, linkType))

    def * = (isbn, linkType, uri, createdAt, updatedAt) <> (LibraryItemLink.tupled, LibraryItemLink.unapply)
  }

  lazy val libraryItems = TableQuery[LibraryItems]
  lazy val libraryMedia = TableQuery[LibraryMedia]

  private def getLibraryItem(userId: Column[Int], isbn: Column[String]): lifted.Query[LibraryItems, LibraryItem, Seq] =
    libraryItems.filter(b => b.isbn === isbn && b.userId === userId)

  private def getLibraryItemMedia(isbn: Column[String]): lifted.Query[LibraryMedia, LibraryItemLink, Seq] =
    libraryMedia.filter(_.isbn === isbn)

  lazy val getLibraryItemBy = Compiled(getLibraryItem _)
  lazy val getLibraryItemLinkFor = Compiled(getLibraryItemMedia _)
}

object LibraryTables {
  def apply[Profile <: JdbcProfile](_driver: Profile) = new LibraryTables[Profile] {
    override val driver = _driver
  }
}
