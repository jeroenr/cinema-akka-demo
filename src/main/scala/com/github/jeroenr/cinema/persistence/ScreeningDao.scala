package com.github.jeroenr.cinema.persistence

import com.github.jeroenr.cinema.service.MovieAndScreen
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters._

import scala.concurrent.{ ExecutionContext, Future }

trait ScreeningDao extends MongoDao[Screening] {
  protected val collectionName = "Screening"

  override protected[this] implicit def jsonFormat = CinemaModel.screeningFormat

  def uniqueId(movieId: String, screenId: String): String = {
    s"${movieId}_${screenId}"
  }

  def add(movieId: String, screenId: String, title: String, availableSeats: Int)(implicit ec: ExecutionContext, db: MongoDatabase) =
    insertOne(Screening(uniqueId(movieId, screenId), screenId, movieId, title, availableSeats, availableSeats))

  def findByMovieAndScreen(movieId: String, screenId: String)(implicit ec: ExecutionContext, db: MongoDatabase) =
    findById(uniqueId(movieId, screenId))

  def updateAvailableSeats(id: MovieAndScreen, availableSeats: Int)(implicit ec: ExecutionContext, db: MongoDatabase) = id match {
    case (movieId, screenId) => updateDoc(uniqueId(movieId, screenId), BsonDocument("availableSeats" -> availableSeats))
  }

  def allAvailableScreenings()(implicit ec: ExecutionContext, db: MongoDatabase): Future[List[Screening]] =
    findAll(gt("availableSeats", 0)).map(_.toList)

}

object ScreeningDao extends ScreeningDao