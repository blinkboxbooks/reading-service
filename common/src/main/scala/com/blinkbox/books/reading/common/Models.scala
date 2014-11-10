package com.blinkbox.books.reading.common

import java.net.URI

sealed trait ReadingStatus
object NotStarted extends ReadingStatus
object Reading extends ReadingStatus
object Finished extends ReadingStatus

case class ReadingPosition(cfi: CFI, percentage: Int) {
  require(percentage >= 0 && percentage <= 100)
}

case class CFI(value: String)

case class Book(
  isbn: String,
  isSample: Boolean,
  readingPosition: ReadingPosition,
  links: Map[String, URI] = Map.empty[String, URI]) {

  def readingStatus: ReadingStatus = readingPosition match {
    case ReadingPosition(_, 0) => NotStarted
    case ReadingPosition(_, 100) => Finished
    case _ => Reading
  }
}

case class Link(name: String, link: URI)