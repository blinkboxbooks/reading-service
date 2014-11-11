package com.blinkbox.books.reading.common.persistence

import java.net.URI

import com.blinkbox.books.reading.common.{LibraryMediaLinkType, CFI}
import org.joda.time.DateTime

case class LibraryItem(
  isbn: String,
  userId: Int,
  sample: Boolean,
  progressCfi: CFI,
  progressPercentage: Int,
  createdAt: DateTime,
  updatedAt: DateTime
)

case class LibraryItemLink(isbn: String, `type`: LibraryMediaLinkType, uri: URI)