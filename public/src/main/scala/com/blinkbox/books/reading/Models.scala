package com.blinkbox.books.reading

import com.blinkbox.books.spray.v2.{Image, Link}
import org.joda.time.DateTime

case class BookDetails(
  isbn: String,
  title: String,
  author: String,
  sortableAuthor: String,
  addedDate: DateTime,
  ownership: Ownership,
  readingStatus: ReadingStatus,
  readingPosition: ReadingPosition,
  images: List[Image] = List.empty,
  links: List[Link] = List.empty) {
}