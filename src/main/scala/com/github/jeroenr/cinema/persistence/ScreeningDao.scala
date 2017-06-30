package com.github.jeroenr.cinema.persistence

import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters._

import scala.concurrent.{ ExecutionContext, Future }

trait ScreeningDao extends MongoDao[Screening] {
  protected val collectionName = "Screening"

  override protected[this] implicit def jsonFormat = CinemaModel.screeningFormat

  def updateAvailableSeats(id: String, availableSeats: Int)(implicit ec: ExecutionContext, db: MongoDatabase) =
    updateDoc(id, BsonDocument("availableSeats" -> availableSeats))

  def allAvailableScreenings()(implicit ec: ExecutionContext, db: MongoDatabase): Future[List[Screening]] =
    findAll(gt("availableSeats", 0)).map(_.toList)

}

object ScreeningDao extends ScreeningDao