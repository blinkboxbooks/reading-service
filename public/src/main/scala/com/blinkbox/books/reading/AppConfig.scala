package com.blinkbox.books.reading

import akka.util.Timeout
import com.blinkbox.books.config.{ApiConfig, AuthClientConfig, DatabaseConfig, RichConfig}
import com.typesafe.config.Config

case class AppConfig(api: ApiConfig, db: DatabaseConfig, auth: AuthClientConfig, startTimeout: Timeout)

object AppConfig {
  def apply(config: Config): AppConfig = AppConfig(
    ApiConfig(config, "service.reading.api.public"),
    DatabaseConfig(config, "service.reading.db"),
    AuthClientConfig(config),
    Timeout(config.getFiniteDuration("service.reading.startTimeout"))
  )
}