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
  implicit lazy val LibraryMediaTypeColumnType = MappedColumnType.base[LibraryMediaLinkType, Int](
    {
      case EpubKey => 0
      case FullEpub => 1
    },
    {
      case 0 => EpubKey
      case 1 => FullEpub
    }
  )
  implicit lazy val bookTypeColumnType = MappedColumnType.base[BookType, Int](
    {
      case Full => 0
      case Sample => 1
    },
    {
      case 0 => Full
      case 1 => Sample
    }
  )
  implicit lazy val readingStatusColumnType = MappedColumnType.base[ReadingStatus, Int](
    {
      case NotStarted => 0
      case Reading => 1
      case Finished => 2
    },
    {
      case 0 => NotStarted
      case 1 => Reading
      case 2 => Finished
    }
  )

  // Reference tables
  class BookTypes(tag: Tag) extends Table[(BookType, String)](tag, "book_types") {
    def id = column[BookType]("id")
    def bookType = column[String]("type")

    override def * = (id, bookType)
  }

  class ReadingStatuses(tag: Tag) extends Table[(ReadingStatus, String)](tag, "reading_statuses") {
    def id = column[ReadingStatus]("id")
    def status = column[String]("status")

    override def * = (id, status)
  }

  class MediaTypes(tag: Tag) extends Table[(LibraryMediaLinkType, String)](tag, "media_types") {
    def id = column[LibraryMediaLinkType]("id")
    def mediaType = column[String]("type")

    override def * = (id, mediaType)
  }

  class LibraryItems(tag: Tag) extends Table[LibraryItem](tag, "library_items") {

    def isbn = column[String]("isbn", DBType("CHAR(13)"))
    def userId = column[Int]("user_id")
    def bookType = column[BookType]("book_type_id")
    def readingStatus = column[ReadingStatus]("reading_status_id")
    def progressCfi = column[Cfi]("progress_cfi", DBType("VARCHAR(255)"))
    def progressPercentage = column[Int]("progress_percentage")
    def createdAt = column[DateTime]("created_at")
    def updatedAt = column[DateTime]("updated_at")

    def pk = primaryKey("pk_library_items", (isbn, userId))
    def fk1 = foreignKey("fk_library_items_book_types", bookType, bookTypes)(_.id)
    def fk2 = foreignKey("fk_library_items_reading_statuses", readingStatus, readingStatuses)(_.id)

    def * = (isbn, userId, bookType, readingStatus, progressCfi, progressPercentage, createdAt, updatedAt) <> (LibraryItem.tupled, LibraryItem.unapply)
  }

  class LibraryMedia(tag: Tag) extends Table[LibraryItemLink](tag, "library_media") {
    def isbn = column[String]("isbn", DBType("CHAR(13)"))
    def linkType = column[LibraryMediaLinkType]("media_type_id")
    def uri = column[URI]("uri")
    def createdAt = column[DateTime]("created_at")
    def updatedAt = column[DateTime]("updated_at")

    def pk = primaryKey("pk_library_media", (isbn, linkType))
    def fk = foreignKey("fk_library_media_media_types", linkType, mediaTypes)(_.id)

    def * = (isbn, linkType, uri, createdAt, updatedAt) <> (LibraryItemLink.tupled, LibraryItemLink.unapply)
  }

  lazy val bookTypes = TableQuery[BookTypes]
  lazy val mediaTypes = TableQuery[MediaTypes]
  lazy val readingStatuses = TableQuery[ReadingStatuses]
  lazy val libraryItems = TableQuery[LibraryItems]
  lazy val libraryMedia = TableQuery[LibraryMedia]

  private def getLibraryItem(userId: Column[Int], isbn: Column[String]): lifted.Query[LibraryItems, LibraryItem, Seq] =
    libraryItems.filter(b => b.isbn === isbn && b.userId === userId)

  private def getUserLibrary(count: ConstColumn[Long], offset: ConstColumn[Long], userId: Column[Int]):  lifted.Query[LibraryItems, LibraryItem, Seq] =
    libraryItems.withFilter(b => b.userId === userId).drop(offset).take(count)

  private def getLibraryItemMedia(isbn: Column[String]): lifted.Query[LibraryMedia, LibraryItemLink, Seq] =
    libraryMedia.filter(_.isbn === isbn)

  def getBulkLibraryItemMedia(isbns: List[String]): lifted.Query[LibraryMedia, LibraryItemLink, Seq] =
    libraryMedia.filter(_.isbn inSet isbns)

  lazy val getLibraryItemBy = Compiled(getLibraryItem _)
  lazy val getLibraryItemLinkFor = Compiled(getLibraryItemMedia _)
  lazy val getUserLibraryById = Compiled(getUserLibrary _)
}

object LibraryTables {
  def apply[Profile <: JdbcProfile](_driver: Profile) = new LibraryTables[Profile] {
    override val driver = _driver
  }
}
