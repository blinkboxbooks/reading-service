package com.blinkbox.books.reading

import com.blinkbox.books.reading.common._
import org.joda.time.DateTime

case class BookDetails(
  isbn: String,
  addedDate: DateTime,
  isSample: Boolean,
  readingPosition: ReadingPosition,
  links: List[Link] = List.empty) {

  def readingStatus: ReadingStatus = readingPosition match {
    case ReadingPosition(_, 0) => NotStarted
    case ReadingPosition(_, 100) => Finished
    case _ => Reading
  }
}