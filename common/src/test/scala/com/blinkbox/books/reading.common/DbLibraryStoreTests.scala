package com.blinkbox.books.reading.common

import java.net.URI

import com.blinkbox.books.reading.common.persistence._
import com.blinkbox.books.slick.{DatabaseComponent, TablesContainer, H2DatabaseSupport}
import com.blinkbox.books.test.{FailHelper, MockitoSyrup}
import com.blinkbox.books.time.{StoppedClock, TimeSupport}
import org.h2.jdbc.JdbcSQLException
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.time.{Millis, Span}

import scala.slick.driver.H2Driver
import scala.slick.jdbc.JdbcBackend.Database

import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class DbLibraryStoreTests extends FlatSpec with MockitoSyrup with ScalaFutures with FailHelper with TimeSupport {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(3000, Millis), interval = Span(100, Millis))

  override implicit val clock = StoppedClock()

  "Library store" should "retrieve a book in user's library" in new PopulatedDbFixture {
    db.withSession { implicit session =>
      whenReady(libraryStore.getBook(2, ISBN1)) { item =>
        assert(item == Some(libItem3))
      }
    }
  }

  it should "return None for a book that is not in user's library" in new PopulatedDbFixture {
    db.withSession { implicit session =>
      whenReady(libraryStore.getBook(1, "nonexistent-isbn")) { item =>
       assert(item == None)
      }
    }
  }

  it should "retrieve book media links for a valid isbn" in new PopulatedDbFixture {
    db.withSession { implicit session =>
      whenReady(libraryStore.getBookMedia(ISBN1)) { media =>
        val expectedLinks = List(Link(libItem1EpubKeyLink.`type`, libItem1EpubKeyLink.uri), Link(libItem1EpubLink.`type`, libItem1EpubLink.uri))
        assert(media == Some(expectedLinks))
      }
    }
  }

  it should "return None when there are no media links for a book" in new PopulatedDbFixture {
    db.withSession { implicit session =>
      whenReady(libraryStore.getBookMedia("nonexistent-book")) { media =>
        assert(media == None)
      }
    }
  }

  class EmptyDbFixture extends TestDbComponent {
    import tables.driver.simple._

    db.withSession { implicit session =>
      val ddl = tables.libraryItems.ddl ++ tables.libraryItemMedia.ddl
      try { ddl.drop } catch { case _: JdbcSQLException => /* Do nothing */ }
      ddl.create
    }

    val libraryStore = new DbLibraryStore[H2DatabaseSupport](db, tables, exceptionFilter)
  }

  class PopulatedDbFixture extends EmptyDbFixture {
    import tables.driver.simple._

    val createdAt = clock.now()
    val updatedAt = clock.now()
    val cfi = CFI("some cfi")
    val percentage = 0

    val ISBN1 = "9780141909837"
    val ISBN2 = "9780141909838"

    val libItem1 = LibraryItem(ISBN1, 1, sample = false, cfi, percentage, createdAt, updatedAt)
    val libItem2 = LibraryItem(ISBN2, 1, sample = false, cfi, percentage, createdAt, updatedAt)
    val libItem3 = LibraryItem(ISBN1, 2, sample = false, cfi, percentage, createdAt, updatedAt)

    val libItem1EpubLink = LibraryItemLink(ISBN1, FullEpub, new URI("http://media.blinkboxbooks.com/9780/141/909/837/8c9771c05e504f836e8118804e02f64c.epub"))
    val libItem2EpubLink = LibraryItemLink(ISBN2, FullEpub, new URI("http://media.blinkboxbooks.com/9780/141/909/838/6e8118804e02f64c8c9771c05e504f83.epub"))

    val libItem1EpubKeyLink = LibraryItemLink(ISBN1, EpubKey, new URI("https://keys.mobcastdev.com/9780/141/909/837/e237e27468c6b37a5679fab718a893e6.epub.9780141909837.key"))
    val libItem2EpubKeyLink = LibraryItemLink(ISBN2, EpubKey, new URI("https://keys.mobcastdev.com/9780/141/909/838/5679fab718a893e6e237e27468c6b37a.epub.9780141909838.key"))

    db.withSession { implicit session =>
      tables.libraryItems ++= List(libItem1, libItem2, libItem3)
      tables.libraryItemMedia ++= List(libItem1EpubLink, libItem1EpubKeyLink, libItem2EpubLink, libItem2EpubKeyLink)
    }
  }

  trait TestDbComponent extends DatabaseComponent {
    override val DB = new H2DatabaseSupport
    override type Tables = TablesContainer[DB.Profile]

    override def db = {
      val threadId = Thread.currentThread.getId
      Database.forURL(s"jdbc:h2:mem:library$threadId;DB_CLOSE_DELAY=-1;MODE=MYSQL;DATABASE_TO_UPPER=FALSE", driver = "org.h2.Driver")
    }

    override val driver = H2Driver
    override val tables = LibraryTables[DB.Profile](driver)
  }
}
