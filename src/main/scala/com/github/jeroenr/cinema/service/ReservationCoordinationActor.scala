package com.github.jeroenr.cinema.service

import akka.actor.{ Actor, ActorRef, Props }
import akka.actor.Actor.Receive
import com.github.jeroenr.cinema.common.Logging
import com.github.jeroenr.cinema.persistence.{ Screening, ScreeningDao }
import com.github.jeroenr.cinema.service.ReservationCoordinationActor._
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.model.Filters._
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class ReservationCoordinationActor(availableScreenings: List[Screening], updateFunc: (String, Int) => Future[Long])(implicit db: MongoDatabase) extends Actor with Logging {

  implicit val ec = context.dispatcher
  implicit val timeout = Timeout(10 seconds)

  // movieId_screenId -> ReserveSeatActor
  var reserveSeatActors: Map[String, ActorRef] = _

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    log.info(s"Starting reservation coordinator")
    reserveSeatActors = availableScreenings.map {
      case Screening(screenId, movieId, _, _, availableSeats) =>
        reserveSeatActorMappingFor(movieId, screenId, availableSeats)
    }.toMap
  }

  override def receive: Receive = {
    case ReserveSeat(movieId, screenId) =>
      reserveSeatActors.get(uniqueId(movieId, screenId)).map { reserveSeatActor =>
        log.info(s"Trying to reserve seat for movie $movieId and screen $screenId")
        reserveSeatActor ? ReserveSeatActor.ReserveSeat
      }.getOrElse(Future(ReserveSeatActor.SoldOut)).pipeTo(sender())

    case AddScreening(movieId, screenId, availableSeats) =>
      log.info(s"Add new screening for movie $movieId and screen $screenId for $availableSeats seats")
      reserveSeatActors += reserveSeatActorMappingFor(movieId, screenId, availableSeats)
      sender() ! ScreeningAdded

    case RemoveScreening(movieId, screenId) =>
      log.info(s"Remove screening for movie $movieId and screen $screenId")
      reserveSeatActors -= uniqueId(movieId, screenId)
      sender() ! ScreeningRemoved
  }

  private def uniqueId(movieId: String, screenId: String) =
    s"movieId_screenId"

  private def reserveSeatActorMappingFor(movieId: String, screenId: String, availableSeats: Int) =
    uniqueId(movieId, screenId) -> context.system.actorOf(Props(new ReserveSeatActor(movieId, screenId, availableSeats, updateFunc)))
}

object ReservationCoordinationActor {
  // request
  case class ReserveSeat(movieId: String, screenId: String)
  case class AddScreening(movieId: String, screenId: String, availableSeats: Int)
  case class RemoveScreening(movieId: String, screenId: String)

  //response
  case object ScreeningAdded
  case object ScreeningRemoved
}
