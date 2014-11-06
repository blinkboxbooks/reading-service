package com.blinkbox.books.reading

import com.blinkbox.books.clients.catalogue.CatalogueService
import com.blinkbox.books.reading.common.persistence.LibraryStore
import com.typesafe.scalalogging.slf4j.StrictLogging

trait LibraryService {

}

class DefaultLibraryService(
  libraryStore: LibraryStore,
  catalogueService: CatalogueService) extends LibraryService with StrictLogging {

}
