package com.github.jeroenr.cinema.service

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server._
import com.github.jeroenr.cinema.common._
import com.github.jeroenr.cinema.model._
import org.mongodb.scala.MongoDatabase

import scala.concurrent.ExecutionContext

class ReservationHttpService(reservationService: ReservationService)(implicit ec: ExecutionContext, db: MongoDatabase) extends Directives with SprayJsonSupport with Logging {

  val reservationsRoute =
    pathPrefix("reservations") {
      pathEndOrSingleSlash {
        get {
          complete(reservationService.list().map(ReservationList.apply))
        } ~
          post {
            entity(as[NewReservation]) { newReservation =>
              complete {
                reservationService.create(newReservation).map(toResponse("reservation"))
              }
            }
          }
      } ~
        pathPrefix(Segment) { id =>
          pathEndOrSingleSlash {
            get {
              complete(reservationService.findById(id))
            }
          }
        }
    }
}

