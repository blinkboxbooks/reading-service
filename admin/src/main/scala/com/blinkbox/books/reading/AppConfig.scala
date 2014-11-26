package com.blinkbox.books.reading

import akka.util.Timeout
import com.blinkbox.books.config.{DatabaseConfig, ApiConfig, RichConfig}
import com.typesafe.config.Config

case class AppConfig(
  api: ApiConfig,
  db: DatabaseConfig,
  startTimeout: Timeout
)

object AppConfig {
  def apply(config: Config): AppConfig = AppConfig(
    ApiConfig(config, "service.reading.api.admin"),
    DatabaseConfig(config, "service.reading.db"),
    Timeout(config.getFiniteDuration("service.reading.startTimeout"))
  )
}
