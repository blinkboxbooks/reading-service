package com.blinkbox.books.reading.common

import com.blinkbox.books.spray.v2.Relation

sealed trait ReadingStatus
object NotStarted extends ReadingStatus
object Reading extends ReadingStatus
object Finished extends ReadingStatus

case class ReadingPosition(cfi: CFI, percentage: Int) {
  require(percentage >= 0 && percentage <= 100)
}

case class CFI(value: String)

sealed trait BookType
object Full extends BookType
object Sample extends BookType

sealed trait LinkType extends Relation
case object CoverImage extends LinkType
case object SampleEpub extends LinkType

sealed trait LibraryMediaLinkType extends LinkType
case object EpubKey extends LibraryMediaLinkType
case object FullEpub extends LibraryMediaLinkType