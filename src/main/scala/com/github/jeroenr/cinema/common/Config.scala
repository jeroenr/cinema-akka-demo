package com.github.jeroenr.cinema.common

import akka.http.scaladsl.model.Uri

import net.ceedubs.ficus.Ficus
import net.ceedubs.ficus.readers.{ ArbitraryTypeReader, ValueReader }

object Config {
  import ArbitraryTypeReader._
  import Ficus._
  import com.typesafe.config.{ Config, ConfigFactory }

  implicit val UriValueReader = new ValueReader[Uri] {
    override def read(config: Config, path: String) = {
      val value = config.getString(path)
      Uri(value)
    }
  }

  case class DatabaseConfig(mongoUri: String)
  case class HttpConfig(interface: String, port: Int)

  private val rootConfig = ConfigFactory.load()

  val config = rootConfig.getConfig("jeroenr.cinema")

  val databaseConfig = config.as[DatabaseConfig]("database")
  val httpConfig = config.as[HttpConfig]("http")
}
