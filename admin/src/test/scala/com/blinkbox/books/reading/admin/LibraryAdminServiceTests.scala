package com.blinkbox.books.reading.admin

import java.net.URI

import com.blinkbox.books.clients.catalogue.{CatalogueInfo, CatalogueInfoMissingException, CatalogueService}
import com.blinkbox.books.reading._
import com.blinkbox.books.reading.persistence.{LibraryItem, LibraryStore}
import com.blinkbox.books.spray.v2.{Image, Link}
import com.blinkbox.books.test.{FailHelper, MockitoSyrup}
import com.blinkbox.books.time.StoppedClock
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.FlatSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class LibraryAdminServiceTests extends FlatSpec with MockitoSyrup with ScalaFutures with FailHelper {

  "Library service" should "add a full book to the library when user does not have it" in new TestFixture {

    when(catalogueService.getInfoFor(Isbn)).thenReturn(Future.successful(TestBookCatalogueInfo))
    when(libraryStore.addBook(Isbn, UserId, Owned)).thenReturn(Future.successful(()))

    whenReady(service.addBook(Isbn, UserId, Owned)) { _ =>

      verify(catalogueService).getInfoFor(Isbn)
      verify(libraryStore).addBook(Isbn, UserId, Owned)
      verifyNoMoreInteractions(libraryStore)
      verifyNoMoreInteractions(catalogueService)
    }
  }

  it should "fail with CatalogueInfoMissingException when the book being added is not in Catalogue" in new TestFixture {
    when(catalogueService.getInfoFor(Isbn)).thenReturn(Future.failed(new CatalogueInfoMissingException("test exception")))

    val ex = failingWith[CatalogueInfoMissingException](service.addBook(Isbn, UserId, Owned))
    assert(ex.getMessage == "test exception")

    verify(catalogueService).getInfoFor(Isbn)
    verifyNoMoreInteractions(catalogueService)
    verifyZeroInteractions(libraryStore)
  }

  it should "fail with LibraryItemConflictException when the book being added is already in the library with the same ownership type" in new TestFixture {
    when(catalogueService.getInfoFor(Isbn)).thenReturn(Future.successful(TestBookCatalogueInfo))
    when(libraryStore.addBook(Isbn, UserId, Owned)).thenReturn(Future.failed(new LibraryItemConflict("test conflict")))

    failingWith[LibraryItemConflict](service.addBook(Isbn, UserId, Owned))
  }

  class TestFixture extends {
    val clock = StoppedClock()

    val Isbn = "9780141909837"
    val UserId = 1

    val sampleEpubLink = Link(SampleEpub, new URI("http://media.blinkboxbooks.com/9780/141/909/837/8c9771c05e504f836e8118804e02f64c.sample.epub"))
    val coverImageLink = Image(CoverImage, new URI("http://internal-media.mobcastdev.com/9780/141/909/837/1d067c7c7b1ef88ad580e99549e05ceb.png"))

    val TestBookCatalogueInfo = CatalogueInfo(Isbn, "Title", "Name Surname", "Surname, Name", coverImageLink.url, sampleEpubLink.url)

    val TestLibraryItem = LibraryItem(Isbn, UserId, Sample, Reading, Some(Cfi("cfi")), 10, DateTime.now, DateTime.now)

    val libraryStore = mock[LibraryStore]
    val catalogueService = mock[CatalogueService]

    val service = new DefaultLibraryAdminService(libraryStore, catalogueService)
  }
}
