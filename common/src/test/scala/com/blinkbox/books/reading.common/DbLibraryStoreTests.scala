package com.blinkbox.books.reading

import java.net.URI

import com.blinkbox.books.reading.persistence._
import com.blinkbox.books.slick.{DatabaseComponent, H2DatabaseSupport, TablesContainer}
import com.blinkbox.books.spray.v2.Link
import com.blinkbox.books.test.{FailHelper, MockitoSyrup}
import com.blinkbox.books.time.{StoppedClock, TimeSupport}
import org.h2.jdbc.JdbcSQLException
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.time.{Millis, Span}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.slick.driver.H2Driver
import scala.slick.jdbc.JdbcBackend.Database

@RunWith(classOf[JUnitRunner])
class DbLibraryStoreTests extends FlatSpec with MockitoSyrup with ScalaFutures with FailHelper with TimeSupport {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(3000, Millis), interval = Span(100, Millis))

  override implicit val clock = StoppedClock()

  "Library store" should "retrieve a book in user's library" in new PopulatedDbFixture {
    db.withSession { implicit session =>
      whenReady(libraryStore.getBook(ISBN1, 2)) { item =>
        assert(item == Some(libItem3))
      }
    }
  }

  it should "retrieve all books in a user's library" in new PopulatedDbFixture {
    db.withSession { implicit session =>
      whenReady(libraryStore.getLibrary(count, offset, 1)) { items =>
        assert(items == List(libItem1, libItem2))
      }
    }
  }

  it should "return an empty list for a user's library if he has no items" in new PopulatedDbFixture {
    db.withSession { implicit session =>
      whenReady(libraryStore.getLibrary(count, offset, 9001)) { items =>
        assert(items.isEmpty)
      }
    }
  }

  it should "return None for a book that is not in user's library" in new PopulatedDbFixture {
    db.withSession { implicit session =>
      whenReady(libraryStore.getBook("nonexistent-isbn", 2)) { item =>
       assert(item == None)
      }
    }
  }

  it should "retrieve book media links for a valid isbn" in new PopulatedDbFixture {
    db.withSession { implicit session =>
      whenReady(libraryStore.getBookMedia(ISBN1)) { media =>
        val expectedLinks = List(Link(libItem1EpubKeyLink.mediaType, libItem1EpubKeyLink.uri), Link(libItem1EpubLink.mediaType, libItem1EpubLink.uri))
        assert(media == expectedLinks)
      }
    }
  }

  it should "throw LibraryMediaMissingException when there are no media links for a book" in new PopulatedDbFixture {
    db.withSession { implicit session =>
      failingWith[LibraryMediaMissingException](libraryStore.getBookMedia("nonexistent-book"))
    }
  }

  it should "map BookType and ReadingStatus to and from integers" in new PopulatedDbFixture {
    import tables.driver.simple._

    db.withSession { implicit session =>
      assert(tables.libraryItems.list == List(libItem1, libItem2, libItem3, libItem4))
    }
  }

  class EmptyDbFixture extends TestDbComponent {
    import tables.driver.simple._

    db.withSession { implicit session =>
      val ddl = tables.libraryItems.ddl ++ tables.libraryMedia.ddl ++ tables.bookTypes.ddl ++ tables.mediaTypes.ddl ++ tables.readingStatuses.ddl
      try { ddl.drop } catch { case _: JdbcSQLException => /* Do nothing */ }
      ddl.create
    }

    val libraryStore = new DbLibraryStore[H2DatabaseSupport](db, tables, exceptionFilter)
  }

  class PopulatedDbFixture extends EmptyDbFixture {
    import tables.driver.simple._

    val createdAt = clock.now()
    val updatedAt = clock.now()
    val cfi = Cfi("some cfi")
    val percentage = 0

    val ISBN1 = "9780141909837"
    val ISBN2 = "9780141909838"

    val libItem1 = LibraryItem(ISBN1, 1, Full, NotStarted, cfi, percentage, createdAt, updatedAt)
    val libItem2 = LibraryItem(ISBN2, 1, Full, Reading, cfi, percentage, createdAt, updatedAt)
    val libItem3 = LibraryItem(ISBN1, 2, Full, Finished, cfi, percentage, createdAt, updatedAt)
    val libItem4 = LibraryItem(ISBN2, 2, Sample, Reading, cfi, percentage, createdAt, updatedAt)

    val libItem1EpubLink = LibraryItemLink(ISBN1, FullEpub, new URI("http://media.blinkboxbooks.com/9780/141/909/837/8c9771c05e504f836e8118804e02f64c.epub"), DateTime.now, DateTime.now)
    val libItem2EpubLink = LibraryItemLink(ISBN2, FullEpub, new URI("http://media.blinkboxbooks.com/9780/141/909/838/6e8118804e02f64c8c9771c05e504f83.epub"), DateTime.now, DateTime.now)

    val libItem1EpubKeyLink = LibraryItemLink(ISBN1, EpubKey, new URI("https://keys.mobcastdev.com/9780/141/909/837/e237e27468c6b37a5679fab718a893e6.epub.9780141909837.key"), DateTime.now, DateTime.now)
    val libItem2EpubKeyLink = LibraryItemLink(ISBN2, EpubKey, new URI("https://keys.mobcastdev.com/9780/141/909/838/5679fab718a893e6e237e27468c6b37a.epub.9780141909838.key"), DateTime.now, DateTime.now)

    db.withSession { implicit session =>
      tables.bookTypes ++= List((Full, "Full"), (Sample, "Sample"))
      tables.mediaTypes ++= List((EpubKey, "EpubKey"), (FullEpub, "FullEpub"))
      tables.readingStatuses ++= List((NotStarted, "NotStarted"), (Reading, "Reading"), (Finished, "Finished"))
      tables.libraryItems ++= List(libItem1, libItem2, libItem3, libItem4)
      tables.libraryMedia ++= List(libItem1EpubLink, libItem1EpubKeyLink, libItem2EpubLink, libItem2EpubKeyLink)
    }
  }

  trait TestDbComponent extends DatabaseComponent {
    override val DB = new H2DatabaseSupport
    override type Tables = TablesContainer[DB.Profile]

    val count = 10
    val offset = 0

    override def db = {
      val threadId = Thread.currentThread.getId
      Database.forURL(s"jdbc:h2:mem:library$threadId;DB_CLOSE_DELAY=-1;MODE=MYSQL;DATABASE_TO_UPPER=FALSE", driver = "org.h2.Driver")
    }

    override val driver = H2Driver
    override val tables = LibraryTables[DB.Profile](driver)
  }
}
