package com.blinkbox.books.clients

import java.net.URL

import com.blinkbox.books.config.RichConfig
import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration

case class ClientConfig(url: URL, timeout: FiniteDuration)

object ClientConfig {
  def apply(config: Config): ClientConfig = ClientConfig(
    url = config.getHttpUrl("service.catalogue.api.public.internalUrl"),
    timeout = config.getFiniteDuration("service.catalogue.api.timeout")
  )
}
