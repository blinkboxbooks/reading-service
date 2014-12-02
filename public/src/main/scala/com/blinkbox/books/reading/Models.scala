package com.blinkbox.books.reading

import java.net.URI

import com.blinkbox.books.reading._
import com.blinkbox.books.spray.v2.{Link, Relation}
import org.joda.time.DateTime

// TODO: add to common-spray
case class Image(rel: Relation, url: URI)

case class BookDetails(
  isbn: String,
  title: String,
  author: String,
  sortableAuthor: String,
  addedDate: DateTime,
  bookType: BookType,
  readingStatus: ReadingStatus,
  readingPosition: ReadingPosition,
  images: List[Image] = List.empty,
  links: List[Link] = List.empty) {
}

trait PostRequestStatus
case object Created extends PostRequestStatus
case object Exists extends PostRequestStatus

case class LibraryConflictException(message: String) extends Exception(message)