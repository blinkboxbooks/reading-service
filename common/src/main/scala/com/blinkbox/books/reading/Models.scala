package com.blinkbox.books.reading

import com.blinkbox.books.spray.v2.Relation

sealed trait ReadingStatus
object NotStarted extends ReadingStatus
object Reading extends ReadingStatus
object Finished extends ReadingStatus

case class ReadingPosition(cfi: Option[Cfi], percentage: Int) {
  require(percentage >= 0 && percentage <= 100)
}

case class Cfi(value: String)

sealed trait Ownership extends Ordered[Ownership] {
  val order: Int
}
object Owned extends Ownership {
  override val order: Int = 1
  override def compare(that: Ownership): Int = this.order - that.order
}
object Sample extends Ownership {
  override val order: Int = 0
  override def compare(that: Ownership): Int = this.order - that.order
}

sealed trait LinkType extends Relation
case object CoverImage extends LinkType
case object SampleEpub extends LinkType

sealed trait LibraryMediaLinkType extends LinkType
case object EpubKey extends LibraryMediaLinkType
case object FullEpub extends LibraryMediaLinkType

class LibraryItemConflict(msg: String, cause: Throwable = null) extends Exception(msg, cause)