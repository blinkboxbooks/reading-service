package com.blinkbox.books.reading.common.persistence

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