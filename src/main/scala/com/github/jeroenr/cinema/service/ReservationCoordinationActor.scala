package com.github.jeroenr.cinema.service

import akka.actor.{ Actor, ActorRef, PoisonPill, Props }
import com.github.jeroenr.cinema.common.Logging
import com.github.jeroenr.cinema.persistence.Screening
import com.github.jeroenr.cinema.service.ReservationCoordinationActor._
import org.mongodb.scala.MongoDatabase
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class ReservationCoordinationActor(availableScreenings: List[Screening], updateFunc: (MovieAndScreen, Int) => Future[Long])(implicit db: MongoDatabase) extends Actor with Logging {

  implicit val ec = context.dispatcher
  implicit val timeout = Timeout(10 seconds)

  // movieId_screenId -> ReserveSeatActor
  var reserveSeatActors: Map[MovieAndScreen, ActorRef] = _

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    log.info(s"Starting reservation coordinator")
    reserveSeatActors = availableScreenings.map {
      case Screening(_, screenId, movieId, _, _, availableSeats) =>
        log.debug(s"Creating reserve seat actor for movie $movieId, screen $screenId and seats $availableSeats")
        reserveSeatActorMappingFor(movieId, screenId, availableSeats)
    }.toMap
  }

  override def receive: Receive = {
    case ReserveSeat(movieId, screenId) =>
      log.debug(s"Finding reserve seat actor to handle reservation for movie $movieId and screen $screenId")
      log.debug(s"Actors $reserveSeatActors")
      reserveSeatActors.get((movieId, screenId)).map { reserveSeatActor =>
        log.info(s"Trying to reserve seat for movie $movieId and screen $screenId")
        reserveSeatActor ? ReserveSeatActor.ReserveSeat
      }.getOrElse(Future.successful(ReserveSeatActor.SoldOut)).pipeTo(sender())

    case AddScreening(movieId, screenId, availableSeats) =>
      log.info(s"Add new screening for movie $movieId and screen $screenId for $availableSeats seats")
      reserveSeatActors += reserveSeatActorMappingFor(movieId, screenId, availableSeats)
      sender() ! ScreeningAdded

    case RemoveScreening(movieId, screenId) =>
      log.info(s"Remove screening for movie $movieId and screen $screenId")
      val id = movieId -> screenId
      reserveSeatActors.get(id).foreach(_ ! PoisonPill)
      reserveSeatActors -= id
      sender() ! ScreeningRemoved
  }

  private def reserveSeatActorMappingFor(movieId: String, screenId: String, availableSeats: Int) =
    (movieId, screenId) -> createChildActor(movieId, screenId, availableSeats)

  protected def createChildActor(movieId: String, screenId: String, availableSeats: Int) =
    context.system.actorOf(Props(new ReserveSeatActor((movieId, screenId), availableSeats, updateFunc)))
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
