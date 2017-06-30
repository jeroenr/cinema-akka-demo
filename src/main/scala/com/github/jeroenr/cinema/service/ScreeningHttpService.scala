package com.github.jeroenr.cinema.service

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server._
import com.github.jeroenr.cinema.model._
import com.github.jeroenr.cinema.common._
import org.mongodb.scala.MongoDatabase

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

class ScreeningHttpService(screeningService: ScreeningService)(implicit ec: ExecutionContext, db: MongoDatabase) extends Directives with SprayJsonSupport with Logging {

  val screeningsRoute =
    pathPrefix("screenings") {
      pathEndOrSingleSlash {
        parameters('imdbId, 'screenId) { (imdbId, screenId) =>
          get {
            complete(screeningService.findByMovieAndScreen(imdbId, screenId))
          }
        } ~
          get {
            complete(screeningService.list().map(ScreeningList.apply))
          } ~
          post {
            entity(as[NewScreening]) { newScreening =>
              complete {
                screeningService.create(newScreening).map(toCreationResponse("screening"))
              }
            }
          }
      } ~
        pathPrefix(Segment) { id =>
          pathEndOrSingleSlash {
            get {
              complete(screeningService.findById(id))
            } ~
              delete {
                complete(screeningService.remove(id))
              }
          }
        }
    }
}

