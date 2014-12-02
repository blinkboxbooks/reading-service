package com.blinkbox.books.reading.persistence

import java.net.URI

import com.blinkbox.books.reading._
import com.blinkbox.books.slick.TablesContainer
import org.joda.time.{DateTimeZone, DateTime}

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
  implicit lazy val bookTypeColumnType = MappedColumnType.base[Ownership, Int](
    {
      case Owned => 0
      case Sample => 1
    },
    {
      case 0 => Owned
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
  class OwnershipTypes(tag: Tag) extends Table[(Ownership, String)](tag, "ownership_types") {
    def id = column[Ownership]("id")
    def ownership = column[String]("type")

    override def * = (id, ownership)
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
    def ownership = column[Ownership]("ownership_type_id")
    def readingStatus = column[ReadingStatus]("reading_status_id")
    def progressCfi = column[Option[Cfi]]("progress_cfi", DBType("VARCHAR(255)"))
    def progressPercentage = column[Int]("progress_percentage")
    def createdAt = column[DateTime]("created_at")
    def updatedAt = column[DateTime]("updated_at")

    def pk = primaryKey("pk_library_items", (isbn, userId))
    def fk1 = foreignKey("fk_library_items_ownership_types", ownership, ownershipTypes)(_.id)
    def fk2 = foreignKey("fk_library_items_reading_statuses", readingStatus, readingStatuses)(_.id)

    def * = (isbn, userId, ownership, readingStatus, progressCfi, progressPercentage, createdAt, updatedAt) <> (LibraryItem.tupled, LibraryItem.unapply)
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

  lazy val ownershipTypes = TableQuery[OwnershipTypes]
  lazy val mediaTypes = TableQuery[MediaTypes]
  lazy val readingStatuses = TableQuery[ReadingStatuses]
  lazy val libraryItems = TableQuery[LibraryItems]
  lazy val libraryMedia = TableQuery[LibraryMedia]

  private def getLibraryItem(userId: Column[Int], isbn: Column[String]): lifted.Query[LibraryItems, LibraryItem, Seq] =
    libraryItems.filter(b => b.isbn === isbn && b.userId === userId)

  private def getUserLibrary(count: ConstColumn[Long], offset: ConstColumn[Long], userId: Column[Int]):  lifted.Query[LibraryItems, LibraryItem, Seq] =
    libraryItems.filter(_.userId === userId).drop(offset).take(count)

  private def getUserLibraryByBookType(count: ConstColumn[Long], offset: ConstColumn[Long], userId: Column[Int], t: Column[BookType]):  lifted.Query[LibraryItems, LibraryItem, Seq] =
    libraryItems.filter(b => b.userId === userId && b.bookType === t).drop(offset).take(count)

  private def getLibraryItemMedia(isbn: Column[String]): lifted.Query[LibraryMedia, LibraryItemLink, Seq] =
    libraryMedia.filter(_.isbn === isbn)

  def getBulkLibraryItemMedia(isbns: List[String]): lifted.Query[LibraryMedia, LibraryItemLink, Seq] =
    libraryMedia.filter(_.isbn inSet isbns)

  def addSample(isbn: String, userId: Int)(implicit session: Session) = {
    val now = DateTime.now(DateTimeZone.UTC)
    libraryItems += LibraryItem(isbn, userId, Sample, NotStarted, Cfi("/6/4/2/1:0"), 0, now, now)
  }

  lazy val getLibraryItemBy = Compiled(getLibraryItem _)
  lazy val getLibraryItemLinkFor = Compiled(getLibraryItemMedia _)
  lazy val getUserLibraryById = Compiled(getUserLibrary _)
  lazy val getUserLibraryByBookTypeWithId = Compiled(getUserLibraryByBookType _)
}

object LibraryTables {
  def apply[Profile <: JdbcProfile](_driver: Profile) = new LibraryTables[Profile] {
    override val driver = _driver
  }
}
