package com.github.jeroenr.cinema.persistence

import org.mongodb.scala.MongoClient

/**
 * A standard wrapper around a Mongo connection to manage the state. Can be subclasses and should be used
 * to provide a singleton within an application.
 */
trait MongoConnection {
  protected[this] def mongoUri: String

  private val MONGO_URI_REGEX = "^(mongodb:(?:\\/{2})?)((\\w+?):(\\w+?)@|:?@?)(\\w+?):(\\d+)((,(\\w+?):(\\d+))*)\\/([\\w\\-]+?)(\\?.*)?$".r

  private val DEFAULT_DB = MONGO_URI_REGEX.findAllIn(mongoUri).matchData
    .toList.headOption
    .map(m => m.group(m.groupCount - 1))

  private val mongoClient = MongoClient(mongoUri)

  /**
   * Returns an instance of a database specified by the name.
   *
   * @param dbName the name of the database.
   *
   * @return an instance of the database.
   */
  def getDb(dbName: String) = {
    mongoClient.getDatabase(dbName)
  }

  def getDefaultDb =
    mongoClient.getDatabase(DEFAULT_DB.get)

  def shutdown() {
    mongoClient.close()
  }
}

class MongoCinemaDbConnection(val mongoUri: String) extends MongoConnection
