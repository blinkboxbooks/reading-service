package com.blinkbox.books.reading.common.persistence

import java.net.URI

import com.blinkbox.books.reading.common.CFI
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

sealed trait MediaType
object EpubKey extends MediaType
object FullEpub extends MediaType


case class LibraryItemLink(isbn: String, `type`: MediaType, uri: URI)