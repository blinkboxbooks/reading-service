package com.blinkbox.books.reading

import java.net.URL

import akka.util.Timeout
import com.blinkbox.books.config.{ApiConfig, AuthClientConfig, DatabaseConfig, RichConfig}
import scala.concurrent.duration._
import com.typesafe.config.Config

case class AppConfig(
  api: ApiConfig,
  db: DatabaseConfig,
  auth: AuthClientConfig,
  catalogue: ClientConfig,
  startTimeout: Timeout
)

object AppConfig {
  def apply(config: Config): AppConfig = AppConfig(
    ApiConfig(config, "service.reading.api.public"),
    DatabaseConfig(config, "service.reading.db"),
    AuthClientConfig(config),
    ClientConfig(config),
    Timeout(config.getFiniteDuration("service.reading.startTimeout"))
  )
}

case class ClientConfig(url: URL, timeout: FiniteDuration)

object ClientConfig {
  def apply(config: Config): ClientConfig = ClientConfig(
    url = config.getHttpUrl("service.catalogue.api.public.internalUrl"),
    timeout = config.getFiniteDuration("service.catalogue.api.timeout")
  )
}
