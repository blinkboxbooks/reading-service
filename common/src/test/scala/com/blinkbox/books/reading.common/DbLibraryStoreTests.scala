package com.blinkbox.books.reading.common

import com.blinkbox.books.reading.common.persistence.{LibraryItem, DbLibraryStore, LibraryTables}
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

  "Library store" should "retrieve a book in users library" in new PopulatedDbFixture {
    db.withSession { implicit session =>
      whenReady(libraryStore.getBook(2, "1")) { item =>
        assert(item == Some(libItem3))
      }
    }
  }

  it should "return None for a book that is not in users library" in new PopulatedDbFixture {
    db.withSession { implicit session =>
      whenReady(libraryStore.getBook(1, "3")) { item =>
       assert(item == None)
      }
    }
  }

  class EmptyDbFixture extends TestDbComponent {
    import tables.driver.simple._

    db.withSession { implicit session =>
      val ddl = tables.libraryItems.ddl
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

    val libItem1 = LibraryItem("1", 1, sample = false, cfi, percentage, createdAt, updatedAt)
    val libItem2 = LibraryItem("2", 1, sample = false, cfi, percentage, createdAt, updatedAt)
    val libItem3 = LibraryItem("1", 2, sample = false, cfi, percentage, createdAt, updatedAt)

    db.withSession { implicit session =>
      tables.libraryItems ++= List(libItem1, libItem2, libItem3)
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
