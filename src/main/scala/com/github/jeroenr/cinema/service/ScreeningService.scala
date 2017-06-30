package com.github.jeroenr.cinema.service

import akka.actor.ActorRef
import com.github.jeroenr.cinema.common._
import com.github.jeroenr.cinema.model.{ NewScreening, Screening }
import com.github.jeroenr.cinema.persistence.{ MovieDao, ScreeningDao, Screening => ScreeningEntity }
import org.mongodb.scala.MongoDatabase
import akka.pattern._
import akka.util.Timeout
import scala.language.postfixOps

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

class ScreeningService(screeningDao: ScreeningDao, movieDao: MovieDao, reservationCoordinator: ActorRef) extends Logging {

  implicit val timeout = Timeout(10 seconds)

  def findById(id: String)(implicit ec: ExecutionContext, db: MongoDatabase): Future[Option[Screening]] =
    screeningDao.findById(id).map(_.map(entityToResponseModel))

  def create(newScreening: NewScreening)(implicit ec: ExecutionContext, db: MongoDatabase): Future[Try[String]] = {
    log.info(s"Creating screening $newScreening")
    movieDao.findById(newScreening.imdbId).flatMap {
      case Some(movie) =>
        screeningDao.add(
          movieId = movie.id,
          screenId = newScreening.screenId,
          title = movie.title,
          availableSeats = newScreening.availableSeats
        ).flatMap {
          case true =>
            log.info(s"Created new screening $newScreening")
            (reservationCoordinator ? ReservationCoordinationActor.AddScreening(movie.id, newScreening.screenId, newScreening.availableSeats))
              .map(_ => Success(screeningDao.uniqueId(movie.id, newScreening.screenId)))
          case false =>
            Future.failed(new IllegalStateException(s"Failed to open reservation for screening $newScreening"))
        }
      case None =>
        Future.failed(new IllegalArgumentException(s"Couldn't find movie with id ${newScreening.imdbId}"))
    }.recover {
      case t =>
        log.error(s"Error while creating screening $newScreening", t)
        Failure(t)
    }
  }

  // TODO: pagination
  def list()(implicit ec: ExecutionContext, db: MongoDatabase): Future[List[Screening]] =
    screeningDao.findAll().map(_.toList.map(entityToResponseModel))

  private def entityToResponseModel(entity: ScreeningEntity): Screening =
    Screening(
      screenId = entity.screenId,
      imdbId = entity.movieId,
      availableSeats = entity.totalSeats,
      reservedSeats = entity.totalSeats - entity.availableSeats,
      movieTitle = entity.movieTitle
    )
}