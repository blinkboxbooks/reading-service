package com.blinkbox.books.reading

import com.blinkbox.books.config.Configuration
import com.blinkbox.books.logging.Loggers
import com.typesafe.scalalogging.slf4j.StrictLogging

object Main extends App with Configuration with Loggers with StrictLogging {
  logger.info(s"Starting reading service version ")
}
