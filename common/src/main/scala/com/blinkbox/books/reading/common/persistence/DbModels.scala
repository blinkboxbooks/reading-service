package com.blinkbox.books.reading.common.persistence

import com.blinkbox.books.reading.common.CFI

case class LibraryItem(
  isbn: String,
  userId: Int,
  sample: Boolean,
  cfi: Option[CFI] = None,
  readingPercentage: Option[Int] = None
)