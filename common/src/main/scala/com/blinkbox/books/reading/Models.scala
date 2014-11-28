package com.blinkbox.books.reading

import com.blinkbox.books.spray.v2.Relation

sealed trait ReadingStatus
object NotStarted extends ReadingStatus
object Reading extends ReadingStatus
object Finished extends ReadingStatus

case class ReadingPosition(cfi: Cfi, percentage: Int) {
  require(percentage >= 0 && percentage <= 100)
}

case class Cfi(value: String)

sealed trait Ownership
object Owned extends Ownership
object Sample extends Ownership

sealed trait LinkType extends Relation
case object CoverImage extends LinkType
case object SampleEpub extends LinkType

sealed trait LibraryMediaLinkType extends LinkType
case object EpubKey extends LibraryMediaLinkType
case object FullEpub extends LibraryMediaLinkType