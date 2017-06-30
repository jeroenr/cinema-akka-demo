package com.github.jeroenr.cinema.service

import com.github.jeroenr.cinema.common.Logging
import com.github.jeroenr.cinema.model.{ Movie, NewMovie }
import com.github.jeroenr.cinema.persistence.{ MovieDao, Movie => MovieEntity }
import org.mongodb.scala.MongoDatabase

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }
import scala.language.postfixOps

trait MovieService extends Logging {

  val movieDao: MovieDao

  def findById(id: String)(implicit ec: ExecutionContext, db: MongoDatabase): Future[Option[Movie]] =
    movieDao.findById(id).map(_.map(entity => Movie(entity.id, entity.title)))

  def create(newMovie: NewMovie)(implicit ec: ExecutionContext, db: MongoDatabase): Future[Try[String]] = {
    log.info(s"Creating movie $newMovie")
    val id = uuid
    movieDao.insertOne(MovieEntity(id, newMovie.movieTitle)).map {
      case true => Success(id)
      case _ => Failure(new IllegalStateException(s"Couldn't create movie $newMovie"))
    }.recover {
      case t =>
        log.error(s"Error while creating movie $newMovie", t)
        Failure(t)
    }
  }

  // TODO: pagination
  def list()(implicit ec: ExecutionContext, db: MongoDatabase): Future[List[Movie]] =
    movieDao.findAll().map(_.toList.map(entity => Movie(entity.id, entity.title)))
}