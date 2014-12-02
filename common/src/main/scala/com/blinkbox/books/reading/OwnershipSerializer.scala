package com.blinkbox.books.reading

import org.json4s.JsonAST.JString
import org.json4s._

object OwnershipSerializer extends CustomSerializer[Ownership](_ => ({
  case JString("Owned") => Owned
  case JString("Sample") => Sample
  case JNull => null
}, {
  case Owned => JString("Owned")
  case Sample => JString("Sample")
}))