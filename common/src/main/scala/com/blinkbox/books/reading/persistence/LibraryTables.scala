package com.blinkbox.books.reading.persistence

import java.net.URI

import com.blinkbox.books.reading._
import com.blinkbox.books.slick.TablesContainer
import org.joda.time.DateTime

import scala.slick.ast.ColumnOption.DBType
import scala.slick.driver.JdbcProfile
import scala.slick.lifted

trait LibraryTables[Profile <: JdbcProfile] extends TablesContainer[Profile] {

  import driver.simple._

  implicit lazy val cfiColumnType = MappedColumnType.base[Cfi, String](_.value, Cfi)
  implicit lazy val urlColumnType = MappedColumnType.base[URI, String](_.toString, new URI(_))
  implicit lazy val LibraryMediaTypeColumnType = MappedColumnType.base[LibraryMediaLinkType, String](
    {
      case EpubKey => "EpubKey"
      case FullEpub => "FullEpub"
    },
    {
      case "EpubKey" => EpubKey
      case "FullEpub" => FullEpub
    }
  )
  implicit lazy val bookTypeColumnType = MappedColumnType.base[BookType, String](
    {
      case Full => "Full"
      case Sample => "Sample"
    },
    {
      case "Full" => Full
      case "Sample" => Sample
    }
  )
  implicit lazy val readingStatusColumnType = MappedColumnType.base[ReadingStatus, String](
    {
      case NotStarted => "NotStarted"
      case Reading => "Reading"
      case Finished => "Finished"
    },
    {
      case "NotStarted" => NotStarted
      case "Reading" => Reading
      case "Finished" => Finished
    }
  )

  class LibraryItems(tag: Tag) extends Table[LibraryItem](tag, "library_items") {

    def isbn = column[String]("isbn", DBType("CHAR(13)"))
    def userId = column[Int]("user_id")
    def bookType = column[BookType]("book_type")
    def readingStatus = column[ReadingStatus]("reading_status")
    def progressCfi = column[Cfi]("progress_cfi")
    def progressPercentage = column[Int]("progress_percentage")
    def createdAt = column[DateTime]("created_at")
    def updatedAt = column[DateTime]("updated_at")
    def pk = primaryKey("library_items_id", (isbn, userId))

    def * = (isbn, userId, bookType, readingStatus, progressCfi, progressPercentage, createdAt, updatedAt) <> (LibraryItem.tupled, LibraryItem.unapply)
  }

  class LibraryItemMedia(tag: Tag) extends Table[LibraryItemLink](tag, "library_item_links") {
    def isbn = column[String]("isbn", DBType("CHAR(13)"))
    def linkType = column[LibraryMediaLinkType]("media_type")
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

  private def getUserLibrary(count: ConstColumn[Long], offset: ConstColumn[Long], userId: Column[Int]):  lifted.Query[LibraryItems, LibraryItem, Seq] =
    libraryItems.withFilter(b => b.userId === userId).drop(offset).take(count)

  def getBulkLibraryItemMedia(isbns: List[String]): lifted.Query[LibraryItemMedia, LibraryItemLink, Seq] =
    libraryItemMedia.filter(_.isbn inSet isbns)

  lazy val getLibraryItemBy = Compiled(getLibraryItem _)
  lazy val getLibraryItemLinkFor = Compiled(getLibraryItemMedia _)
  lazy val getUserLibraryById = Compiled(getUserLibrary _)
}

object LibraryTables {
  def apply[Profile <: JdbcProfile](_driver: Profile) = new LibraryTables[Profile] {
    override val driver = _driver
  }
}
