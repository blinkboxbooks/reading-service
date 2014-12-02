package com.blinkbox.books.reading

import java.net.URI

import com.blinkbox.books.auth.User
import com.blinkbox.books.clients.catalogue.{CatalogueInfo, CatalogueInfoMissingException, CatalogueService}
import com.blinkbox.books.reading._
import com.blinkbox.books.reading.persistence.{LibraryItem, LibraryMediaMissingException, LibraryStore}
import com.blinkbox.books.spray.v2.Link
import com.blinkbox.books.test.{FailHelper, MockitoSyrup}
import com.blinkbox.books.time.{StoppedClock, TimeSupport}
import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.FlatSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class LibraryServiceTests extends FlatSpec with MockitoSyrup with ScalaFutures with FailHelper {

  "Library service" should "return book details" in new TestFixture {
    when(catalogueService.getInfoFor(ISBN)).thenReturn(Future.successful(TestCatalogueInfo))
    when(libraryStore.getBook(any[String], any[Int])).thenReturn(Future.successful(Some(TestLibraryItem)))
    when(libraryStore.getBookMedia(ISBN)).thenReturn(Future.successful(List(fullEpubLink, epubKeyLink)))

    whenReady(service.getBook(ISBN)) { res =>
      assert(res == Some(TestBookDetails))
    }

    verify(catalogueService).getInfoFor(ISBN)
    verify(libraryStore).getBook(ISBN, UserId)
    verify(libraryStore).getBookMedia(ISBN)

    verifyNoMoreInteractions(catalogueService)
    verifyNoMoreInteractions(libraryStore)
  }

  it should "return None if user does not have it in his library" in new TestFixture {
    when(catalogueService.getInfoFor(ISBN)).thenReturn(Future.successful(TestCatalogueInfo))
    when(libraryStore.getBook(any[String], any[Int])).thenReturn(Future.successful(None))
    when(libraryStore.getBookMedia(ISBN)).thenReturn(Future.successful(List(fullEpubLink, epubKeyLink)))

    whenReady(service.getBook(ISBN)) { res =>
      assert(res == None)
    }

    verify(catalogueService).getInfoFor(ISBN)
    verify(libraryStore).getBook(ISBN, UserId)
    verify(libraryStore).getBookMedia(ISBN)

    verifyNoMoreInteractions(catalogueService)
    verifyNoMoreInteractions(libraryStore)
  }

  it should "return LibraryMediaMissing exception if library does not have media for a book in the library" in new TestFixture {
    when(catalogueService.getInfoFor(ISBN)).thenReturn(Future.successful(TestCatalogueInfo))
    when(libraryStore.getBook(any[String], any[Int])).thenReturn(Future.successful(Some(TestLibraryItem)))
    when(libraryStore.getBookMedia(ISBN)).thenReturn(Future.failed(new LibraryMediaMissingException("expected exception")))

    failingWith[LibraryMediaMissingException](service.getBook(ISBN))

    verify(catalogueService).getInfoFor(ISBN)
    verify(libraryStore).getBook(ISBN, UserId)
    verify(libraryStore).getBookMedia(ISBN)

    verifyNoMoreInteractions(catalogueService)
    verifyNoMoreInteractions(libraryStore)
  }

  it should "return CatalogueInfoMissing exception if catalogue does not have book info for a book in the library" in new TestFixture {
    when(catalogueService.getInfoFor(ISBN)).thenReturn(Future.failed(new CatalogueInfoMissingException("expected exception")))
    when(libraryStore.getBook(any[String], any[Int])).thenReturn(Future.successful(Some(TestLibraryItem)))
    when(libraryStore.getBookMedia(ISBN)).thenReturn(Future.successful(List(fullEpubLink, epubKeyLink)))

    failingWith[CatalogueInfoMissingException](service.getBook(ISBN))

    verify(catalogueService).getInfoFor(ISBN)
    verify(libraryStore).getBook(ISBN, UserId)
    verify(libraryStore).getBookMedia(ISBN)

    verifyNoMoreInteractions(catalogueService)
    verifyNoMoreInteractions(libraryStore)
  }

  it should "return the entire library of a user" in new TestFixture {
    // The links have two sample items as one comes from the libraryStore and the other comes from the Catalogue service.
    // Doing that so that I don't have to mess with ordering of lists
    val expectedBookDetail1 =
      BookDetails(isbn1, catalogueInfo1.title, catalogueInfo1.author, catalogueInfo1.sortableAuthor, clock.now(), libItem1.bookType, libItem1.readingStatus, ReadingPosition(libItem1.progressCfi, libItem1.progressPercentage), List(Image(CoverImage, catalogueInfo1.coverImageUrl)), List(Link(SampleEpub, catalogueInfo1.sampleEpubUrl), Link(SampleEpub, catalogueInfo1.sampleEpubUrl)))
    val expectedBookDetail2 =
      BookDetails(isbn2, catalogueInfo2.title, catalogueInfo2.author, catalogueInfo2.sortableAuthor, clock.now(), libItem2.bookType, libItem2.readingStatus, ReadingPosition(libItem2.progressCfi, libItem2.progressPercentage), List(Image(CoverImage, catalogueInfo2.coverImageUrl)), List(Link(SampleEpub, catalogueInfo2.sampleEpubUrl), Link(SampleEpub, catalogueInfo2.sampleEpubUrl)))
    val expectedBookDetail3 =
      BookDetails(isbn3, catalogueInfo3.title, catalogueInfo3.author, catalogueInfo3.sortableAuthor, clock.now(), libItem3.bookType, libItem3.readingStatus, ReadingPosition(libItem3.progressCfi, libItem3.progressPercentage), List(Image(CoverImage, catalogueInfo3.coverImageUrl)), List(Link(SampleEpub, catalogueInfo3.sampleEpubUrl), Link(SampleEpub, catalogueInfo3.sampleEpubUrl)))
    val expectedBookDetail4 =
      BookDetails(isbn4, catalogueInfo4.title, catalogueInfo4.author, catalogueInfo4.sortableAuthor, clock.now(), libItem4.bookType, libItem4.readingStatus, ReadingPosition(libItem4.progressCfi, libItem4.progressPercentage), List(Image(CoverImage, catalogueInfo4.coverImageUrl)), List(Link(SampleEpub, catalogueInfo4.sampleEpubUrl), Link(SampleEpub, catalogueInfo4.sampleEpubUrl)))

    when(libraryStore.getLibrary(count, offset, userId)).thenReturn(Future.successful(List(libItem1, libItem2, libItem3, libItem4)))
    when(libraryStore.getSamples(count, offset, userId)).thenReturn(Future.successful(List(libItem3, libItem4)))
    when(libraryStore.getBooksMedia(List(isbn3, isbn4), user.id)).
      thenReturn(Future.successful(Map(
      (isbn3 -> List(Link(SampleEpub, catalogueInfo3.sampleEpubUrl))),
      (isbn4 -> List(Link(SampleEpub, catalogueInfo4.sampleEpubUrl)))
    )))
    when(catalogueService.getBulkInfoFor(List(isbn3, isbn4), user.id)).thenReturn(Future.successful(List(catalogueInfo3, catalogueInfo4)))

    whenReady(service.getSamples(count, offset)) { res =>
      assert(res.size == 2)
      assert(res.contains(expectedBookDetail3))
      assert(res.contains(expectedBookDetail4))
    }
  }

  it should "successfully add a sample book if a book exists in the catalogue service and not in the data store" in new TestFixture {
    when(catalogueService.getInfoFor(ISBN)).thenReturn(Future.successful(TestCatalogueInfo))
    when(libraryStore.addSample(ISBN, userId)).thenReturn(Future.successful(()))
    when(libraryStore.getBook(ISBN, userId)).thenReturn(Future.successful(None))

    whenReady(service.addSample(ISBN)) { res =>
      assert(res == Created)
    }
  }

  it should "successfully add a sample book if a book exists in the catalogue service and exists as a sample in the data store" in new TestFixture {
    when(catalogueService.getInfoFor(ISBN)).thenReturn(Future.successful(TestCatalogueInfo))
    when(libraryStore.addSample(ISBN, userId)).thenReturn(Future.successful(()))
    when(libraryStore.getBook(ISBN, userId)).thenReturn(Future.successful(Some(TestLibrarySampleItem)))

    whenReady(service.addSample(ISBN)) { res =>
      assert(res == Exists)
    }
  }

  it should "should throw LibraryConflictException when adding a sample and the full version of the book exists" in new TestFixture {
    when(catalogueService.getInfoFor(ISBN)).thenReturn(Future.successful(TestCatalogueInfo))
    when(libraryStore.addSample(ISBN, userId)).thenReturn(Future.successful(()))
    when(libraryStore.getBook(ISBN, userId)).thenReturn(Future.successful(Some(TestLibraryItem)))

    failingWith[LibraryConflictException](service.addSample(ISBN))
  }

  it should "return the samples within a user's library" in new TestFixture {
    val expectedBookDetail1 =
      BookDetails(isbn1, catalogueInfo1.title, catalogueInfo1.author, catalogueInfo1.sortableAuthor, clock.now(), libItem1.bookType, libItem1.readingStatus, ReadingPosition(libItem1.progressCfi, libItem1.progressPercentage), List(Image(CoverImage, catalogueInfo1.coverImageUrl)), List(Link(SampleEpub, catalogueInfo1.sampleEpubUrl), Link(SampleEpub, catalogueInfo1.sampleEpubUrl)))
    val expectedBookDetail2 =
      BookDetails(isbn2, catalogueInfo2.title, catalogueInfo2.author, catalogueInfo2.sortableAuthor, clock.now(), libItem2.bookType, libItem2.readingStatus, ReadingPosition(libItem2.progressCfi, libItem2.progressPercentage), List(Image(CoverImage, catalogueInfo2.coverImageUrl)), List(Link(SampleEpub, catalogueInfo2.sampleEpubUrl), Link(SampleEpub, catalogueInfo2.sampleEpubUrl)))
  }

  it should "return successfully when a user has no items in his library" in new TestFixture {
    when(libraryStore.getLibrary(count, offset, UserId)).thenReturn(Future.successful(List.empty[LibraryItem]))
    when(catalogueService.getBulkInfoFor(List.empty[String], user.id)).thenReturn(Future.successful(List.empty[CatalogueInfo]))
    when(libraryStore.getBooksMedia(List.empty[String], user.id)).thenReturn(Future.successful(Map.empty[String, List[Link]])) // compiler needs type hint here

    whenReady(service.getLibrary(count, offset)) { res =>
      assert(res == List.empty[BookDetails])
    }
  }

  it should "return a LibraryMediaMissingException in a bulk request for library medias if there are missing media for a book" in new TestFixture {

    when(libraryStore.getLibrary(count, offset, userId)).thenReturn(Future.successful(List(libItem1, libItem2)))
    when(libraryStore.getBooksMedia(List(isbn1, isbn2), user.id)).
      thenReturn(Future.failed(new LibraryMediaMissingException("failed test")))
    when(catalogueService.getBulkInfoFor(List(isbn1, isbn2), user.id)).thenReturn(Future.successful(List(catalogueInfo1, catalogueInfo2)))

    failingWith[LibraryMediaMissingException](service.getLibrary(count, offset))
  }

  it should "return a CatalogueInfoMissingException in a bulk request for book infos if there is missing information for a book" in new TestFixture {
    when(libraryStore.getLibrary(count, offset, userId)).thenReturn(Future.successful(List(libItem1, libItem2)))
    when(libraryStore.getBooksMedia(List(isbn1, isbn2), user.id)).
      thenReturn(Future.successful(Map(
      (isbn1 -> List(Link(SampleEpub, catalogueInfo1.sampleEpubUrl))),
      (isbn2 -> List(Link(SampleEpub, catalogueInfo2.sampleEpubUrl)))
    )))
    when(catalogueService.getBulkInfoFor(List(isbn1, isbn2), user.id)).thenReturn(Future.failed(new CatalogueInfoMissingException("failed test")))
    failingWith[CatalogueInfoMissingException](service.getLibrary(count, offset))
  }

  class TestFixture extends TimeSupport {
    val clock = StoppedClock()

    val UserId = 1
    implicit val user =  User("", Map("sub" -> s"urn:blinkbox:zuul:user:$UserId", "sso/at" -> "ssoToken"))
    val ISBN = "9780141909837"
    val ReadingStatus = Reading
    val Progress = ReadingPosition(Cfi("someCfi"), 15)

    val fullEpubLink = Link(FullEpub, new URI("http://media.blinkboxbooks.com/9780/141/909/837/8c9771c05e504f836e8118804e02f64c.epub"))
    val sampleEpubLink = Link(SampleEpub, new URI("http://media.blinkboxbooks.com/9780/141/909/837/8c9771c05e504f836e8118804e02f64c.sample.epub"))
    val epubKeyLink = Link(EpubKey, new URI("https://keys.mobcastdev.com/9780/141/909/837/e237e27468c6b37a5679fab718a893e6.epub.9780141909837.key"))
    val coverImageLink = Image(CoverImage, new URI("http://internal-media.mobcastdev.com/9780/141/909/837/1d067c7c7b1ef88ad580e99549e05ceb.png"))

    val images = List(coverImageLink)
    val links = List(sampleEpubLink, fullEpubLink, epubKeyLink)

    val TestLibraryItem = LibraryItem(ISBN, UserId, Full, ReadingStatus, Progress.cfi, Progress.percentage, clock.now(), clock.now())
    val TestLibrarySampleItem = LibraryItem(ISBN, UserId, Sample, ReadingStatus, Progress.cfi, Progress.percentage, clock.now(), clock.now())
    val TestCatalogueInfo = CatalogueInfo(ISBN, "Title", "Name Surname", "Surname, Name", coverImageLink.url, sampleEpubLink.url)
    val TestBookDetails = BookDetails(ISBN, TestCatalogueInfo.title, TestCatalogueInfo.author, TestCatalogueInfo.sortableAuthor, clock.now(), Full, ReadingStatus, Progress, images, links)

    val catalogueService = mock[CatalogueService]
    val libraryStore = mock[LibraryStore]

    /* variables for the persistence layer and catalogue service mocks */
    val userId = 1
    val isbn1 = "9870123456789"
    val isbn2 = "9879876543210"
    val isbn3 = "9879876543000"
    val isbn4 = "9879876543999"
    val libItem1 = LibraryItem(isbn1, userId, Full, Finished, Cfi("/6/4"), 100, clock.now(), clock.now)
    val libItem2 = LibraryItem(isbn2, userId, Full, Reading, Cfi("/6/4"), 50, clock.now(), clock.now)
    val libItem3 = LibraryItem(isbn3, userId, Sample, Reading, Cfi("/6/4"), 50, clock.now(), clock.now)
    val libItem4 = LibraryItem(isbn4, userId, Sample, Reading, Cfi("/6/4"), 50, clock.now(), clock.now)
    val catalogueInfo1 = CatalogueInfo(isbn1, "Book Name", "Author Name", "Name, Author", new URI("http://cover/location"), new URI("http://sample/location"))
    val catalogueInfo2 = CatalogueInfo(isbn2, "Book Other", "Author Other", "Other, Author", new URI("http://cover/location2"), new URI("http://sample/location2"))
    val catalogueInfo3 = CatalogueInfo(isbn3, "Sample Name", "Author Name", "Name, Author", new URI("http://cover/location3"), new URI("http://sample/location3"))
    val catalogueInfo4 = CatalogueInfo(isbn4, "Sample Other", "Author Other", "Other, Author", new URI("http://cover/location4"), new URI("http://sample/location4"))
    /*******************************************************************/

    val count = 25
    val offset = 0

    val service = new DefaultLibraryService(libraryStore, catalogueService)
  }
}
