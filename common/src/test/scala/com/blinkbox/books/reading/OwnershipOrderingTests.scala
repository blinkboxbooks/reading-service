package com.blinkbox.books.reading

import org.scalatest.FlatSpec

class OwnershipOrderingTests extends FlatSpec {

  "Ownership types" should " be ordered correctly" in {
    assert(List(Owned, Sample).sorted == List(Sample, Owned))
  }
}
