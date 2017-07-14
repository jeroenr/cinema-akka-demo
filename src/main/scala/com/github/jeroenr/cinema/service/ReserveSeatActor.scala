package com.github.jeroenr.cinema.service

import akka.actor.{ Actor, PoisonPill }
import com.github.jeroenr.cinema.common.{ Logging }

import scala.concurrent.duration._
import akka.util.Timeout
import scala.util.{Success, Failure}
import org.mongodb.scala.MongoDatabase

import scala.concurrent.Future
import akka.pattern._

class ReserveSeatActor(screeningId: MovieAndScreen, initialSeatsAvailable: Int, updateFunc: (MovieAndScreen, Int) => Future[Long])(implicit db: MongoDatabase) extends Actor with Logging {

  implicit val ec = context.system.dispatcher

  var availableSeats = initialSeatsAvailable

  import ReserveSeatActor._
  import scala.language.postfixOps

  implicit val timeout = Timeout(10 seconds)

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    log.info(s"Starting reserve seat actor for screening $screeningId")
  }

  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    log.info(s"Stopped reserve seat actor for screening $screeningId")
  }

  override def receive: Receive = {
    case ReserveSeat =>
      log.info(s"Checking availability")
      if (availableSeats > 0) {
        log.info(s"Looks like there's still $availableSeats seats left!")
        availableSeats -= 1
        updateFunc(screeningId, availableSeats).onComplete {
          case Success(1L) =>
            log.info(s"Seat reserved for screening $screeningId. $availableSeats seats left."
          case Success(amount) =>
            log.error(s"Couldn't reserve seat. Updated ${amount} reservations")
          case Failure(t) =>
            log.error(s"Exception while trying to persist reservation", t)
        }
        sender() ! SeatRegistered(availableSeats)

      } else {
        log.warn(s"Sorry no more available seats for screening $screeningId")
        sender() ! SoldOut
        self ! PoisonPill
      }
  }
}

object ReserveSeatActor {
  // request
  case object ReserveSeat

  // response
  case class SeatRegistered(left: Int)
  case class Error(msg: String)
  case object SoldOut
}
