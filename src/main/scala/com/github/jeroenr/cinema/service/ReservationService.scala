package com.github.jeroenr.cinema.service

import akka.actor.ActorRef
import com.github.jeroenr.cinema.model._
import com.github.jeroenr.cinema.persistence.{ MovieDao, ReservationDao, ScreeningDao, Reservation => ReservationEntity }
import org.mongodb.scala.MongoDatabase

import scala.concurrent.{ ExecutionContext, Future }
import akka.pattern._
import akka.util.Timeout
import com.github.jeroenr.cinema.common.Logging

import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }
import scala.language.postfixOps

class ReservationService(reservationDao: ReservationDao, reservationCoordinationActor: ActorRef) extends Logging {

  implicit val timeout = Timeout(10 seconds)

  def findById(id: String)(implicit ec: ExecutionContext, db: MongoDatabase): Future[Option[Reservation]] =
    reservationDao.findById(id).map(_.map(entityToResponseModel))

  def create(newReservation: NewReservation)(implicit ec: ExecutionContext, db: MongoDatabase): Future[Try[String]] = {
    log.info(s"Making reservation $newReservation")
    (reservationCoordinationActor ? ReservationCoordinationActor.ReserveSeat(
      movieId = newReservation.imdbId,
      screenId = newReservation.screenId
    )).flatMap {
      case ReserveSeatActor.SeatRegistered(left) =>
        log.info(s"Seat reserved for movie ${newReservation.imdbId} and screen ${newReservation.screenId}. $left seats left.")
        if (left <= 0) reservationCoordinationActor ! ReservationCoordinationActor.RemoveScreening
        val id = uuid
        reservationDao.insertOne(ReservationEntity(
          id = id,
          screenId = newReservation.screenId,
          movieId = newReservation.imdbId
        )).map {
          case true => Success(id)
          case _ => Failure(new IllegalStateException(s"Couldn't add reservation $newReservation"))
        }
      case ReserveSeatActor.SoldOut =>
        Future.failed(new IllegalArgumentException(s"Can't make reservation because the screening ${newReservation.screenId} is sold out!"))
      case ReserveSeatActor.Error(msg) =>
        Future.failed(new IllegalStateException(msg))
    }.recover {
      case t =>
        log.error(s"Error while adding reservation")
        Failure(t)
    }
  }

  // TODO: pagination
  def list()(implicit ec: ExecutionContext, db: MongoDatabase): Future[List[Reservation]] =
    reservationDao.findAll().map(_.toList.map(entityToResponseModel))

  private def entityToResponseModel(entity: ReservationEntity) =
    Reservation(
      id = entity.id,
      screenId = entity.screenId
    )
}
