package com.github.jeroenr.cinema

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.github.jeroenr.cinema.common._
import com.github.jeroenr.cinema.service._
import com.github.jeroenr.cinema.persistence._
import org.mongodb.scala.model.Filters._

import scala.language.postfixOps

object Boot extends App with Logging
    with CorsRoute
    with CorsDirectives
    with MovieService
    with MovieHttpService {

  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val movieDao = MovieDao
  val screeningDao = ScreeningDao
  val reservationDao = ReservationDao

  log.info(s"Starting Cinema Service")

  val mongoConnection = new MongoCinemaDbConnection(Config.databaseConfig.mongoUri)
  implicit val db = mongoConnection.getDefaultDb

  // FIXME: reservation system is only available after this future
  screeningDao.allAvailableScreenings().flatMap { screenings =>
    log.info(s"Setting up reservation coordination actor for ${screenings.size} available screenings")
    val reservationCoordinator = system.actorOf(Props(new ReservationCoordinationActor(screenings, screeningDao.updateAvailableSeats)))

    val reservationService = new ReservationService(reservationDao, reservationCoordinator)
    val reservationHttpService = new ReservationHttpService(reservationService)

    val screeningService = new ScreeningService(screeningDao, movieDao, reservationCoordinator)
    val screeningHttpService = new ScreeningHttpService(screeningService)

    val rootRoute =
      defaultCORSHeaders {
        rejectEmptyResponse {
          corsRoute ~ moviesRoute ~ screeningHttpService.screeningsRoute ~ reservationHttpService.reservationsRoute
        }
      }
    Http().bindAndHandle(rootRoute, Config.httpConfig.interface, Config.httpConfig.port).transform(
      binding => log.info(s"REST interface bound to ${binding.localAddress} "), { t => log.error(s"Couldn't start Cinema Service", t); sys.exit(1) }
    )
  }.onFailure {
    case t =>
      log.error(s"Unexpected error when bootstrapping reservation system", t)
  }

}
