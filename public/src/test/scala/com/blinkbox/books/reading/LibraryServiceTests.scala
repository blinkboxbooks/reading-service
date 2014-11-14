package com.blinkbox.books.reading

import java.net.URI

import com.blinkbox.books.clients.catalogue.{CatalogueInfoMissingException, CatalogueInfo, CatalogueService}
import com.blinkbox.books.reading.common._
import com.blinkbox.books.reading.common.persistence.{LibraryMediaMissingException, LibraryItem, LibraryStore}
import com.blinkbox.books.spray.v2.Link
import com.blinkbox.books.test.{FailHelper, MockitoSyrup}
import com.blinkbox.books.time.{StoppedClock, TimeSupport}
import org.junit.runner.RunWith
import org.scalatest.FlatSpec

import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class LibraryServiceTests extends FlatSpec with MockitoSyrup with ScalaFutures with FailHelper {

  "Library service" should "return book details" in new TestFixture {
    when(catalogueService.getInfoFor(ISBN)).thenReturn(Future.successful(CatalogueInfo(coverImageLink.url, sampleEpubLink.url)))
    when(libraryStore.getBook(any[Int], any[String])).thenReturn(Future.successful(Some(TestLibraryItem)))
    when(libraryStore.getBookMedia(ISBN)).thenReturn(Future.successful(List(fullEpubLink, epubKeyLink)))

    whenReady(service.getBook(ISBN, User)) { res =>
      assert(res == Some(TestBookDetails))
    }

    verify(catalogueService).getInfoFor(ISBN)
    verify(libraryStore).getBook(User, ISBN)
    verify(libraryStore).getBookMedia(ISBN)

    verifyNoMoreInteractions(catalogueService)
    verifyNoMoreInteractions(libraryStore)
  }

  it should "return None if user does not have it in his library" in new TestFixture {
    when(catalogueService.getInfoFor(ISBN)).thenReturn(Future.successful(CatalogueInfo(coverImageLink.url, sampleEpubLink.url)))
    when(libraryStore.getBook(any[Int], any[String])).thenReturn(Future.successful(None))
    when(libraryStore.getBookMedia(ISBN)).thenReturn(Future.successful(List(fullEpubLink, epubKeyLink)))

    whenReady(service.getBook(ISBN, User)) { res =>
      assert(res == None)
    }

    verify(catalogueService).getInfoFor(ISBN)
    verify(libraryStore).getBook(User, ISBN)
    verify(libraryStore).getBookMedia(ISBN)

    verifyNoMoreInteractions(catalogueService)
    verifyNoMoreInteractions(libraryStore)
  }

  it should "return LibraryMediaMissing exception if library does not have media for a book in the library" in new TestFixture {
    when(catalogueService.getInfoFor(ISBN)).thenReturn(Future.successful(CatalogueInfo(coverImageLink.url, sampleEpubLink.url)))
    when(libraryStore.getBook(any[Int], any[String])).thenReturn(Future.successful(Some(TestLibraryItem)))
    when(libraryStore.getBookMedia(ISBN)).thenReturn(Future.failed(new LibraryMediaMissingException("expected exception")))

    failingWith[LibraryMediaMissingException](service.getBook(ISBN, User))

    verify(catalogueService).getInfoFor(ISBN)
    verify(libraryStore).getBook(User, ISBN)
    verify(libraryStore).getBookMedia(ISBN)

    verifyNoMoreInteractions(catalogueService)
    verifyNoMoreInteractions(libraryStore)
  }

  it should "return CatalogueInfoMissing exception if catalogue does not have book info for a book in the library" in new TestFixture {
    when(catalogueService.getInfoFor(ISBN)).thenReturn(Future.failed(new CatalogueInfoMissingException("expected exception")))
    when(libraryStore.getBook(any[Int], any[String])).thenReturn(Future.successful(Some(TestLibraryItem)))
    when(libraryStore.getBookMedia(ISBN)).thenReturn(Future.successful(List(fullEpubLink, epubKeyLink)))

    failingWith[CatalogueInfoMissingException](service.getBook(ISBN, User))

    verify(catalogueService).getInfoFor(ISBN)
    verify(libraryStore).getBook(User, ISBN)
    verify(libraryStore).getBookMedia(ISBN)

    verifyNoMoreInteractions(catalogueService)
    verifyNoMoreInteractions(libraryStore)
  }

  class TestFixture extends TimeSupport {
    val clock = StoppedClock()

    val User = 1
    val ISBN = "9780141909837"
    val Progress = ReadingPosition(CFI("someCfi"), 15)

    val fullEpubLink = Link(FullEpub, new URI("http://media.blinkboxbooks.com/9780/141/909/837/8c9771c05e504f836e8118804e02f64c.epub"))
    val sampleEpubLink = Link(SampleEpub, new URI("http://media.blinkboxbooks.com/9780/141/909/837/8c9771c05e504f836e8118804e02f64c.sample.epub"))
    val epubKeyLink = Link(EpubKey, new URI("https://keys.mobcastdev.com/9780/141/909/837/e237e27468c6b37a5679fab718a893e6.epub.9780141909837.key"))
    val coverImageLink = Image(CoverImage, new URI("http://internal-media.mobcastdev.com/9780/141/909/837/1d067c7c7b1ef88ad580e99549e05ceb.png"))

    val images = List(coverImageLink)
    val links = List(sampleEpubLink, fullEpubLink, epubKeyLink)

    val TestLibraryItem = LibraryItem(ISBN, User, Full, Progress.cfi, Progress.percentage, clock.now(), clock.now())
    val TestBookDetails = BookDetails(ISBN, clock.now(), Full, Progress, images, links)

    val catalogueService = mock[CatalogueService]
    val libraryStore = mock[LibraryStore]

    val service = new DefaultLibraryService(libraryStore, catalogueService)
  }
}
