package com.blinkbox.books.reading.persistence

import java.net.URI

import com.blinkbox.books.reading.{ReadingStatus, BookType, LibraryMediaLinkType, Cfi}
import org.joda.time.DateTime

case class LibraryItem(
  isbn: String,
  userId: Int,
  bookType: BookType,
  readingStatus: ReadingStatus,
  progressCfi: Cfi,
  progressPercentage: Int,
  createdAt: DateTime,
  updatedAt: DateTime
)

case class LibraryItemLink(
  isbn: String, mediaType: LibraryMediaLinkType,
  uri: URI, 
  createdAt: DateTime, 
  updatedAt: DateTime
)
