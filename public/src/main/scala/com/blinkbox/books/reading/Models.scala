package com.blinkbox.books.reading

import java.net.URI

import com.blinkbox.books.reading.common._
import com.blinkbox.books.spray.v2.{Relation, Link}
import org.joda.time.DateTime

// TODO: add to common-spray
case class Image(rel: Relation, url: URI)

case class BookDetails(
  isbn: String,
  addedDate: DateTime,
  isSample: Boolean,
  readingPosition: ReadingPosition,
  images: List[Image] = List.empty,
  links: List[Link] = List.empty) {

  def readingStatus: ReadingStatus = readingPosition match {
    case ReadingPosition(_, 0) => NotStarted
    case ReadingPosition(_, 100) => Finished
    case _ => Reading
  }
}