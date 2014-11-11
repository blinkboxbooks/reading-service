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

trait Relation

case class Link(rel: Relation, url: URI)

sealed trait LinkType extends Relation
case object CoverImage extends LinkType
case object SampleEpub extends LinkType

sealed trait LibraryMediaLinkType extends LinkType
case object EpubKey extends LibraryMediaLinkType
case object FullEpub extends LibraryMediaLinkType