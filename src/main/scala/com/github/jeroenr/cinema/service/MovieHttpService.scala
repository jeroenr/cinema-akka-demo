package com.github.jeroenr.cinema.service

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server._
import com.github.jeroenr.cinema.common._
import com.github.jeroenr.cinema.model._
import org.mongodb.scala.MongoDatabase

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

trait MovieHttpService extends Directives with SprayJsonSupport with Logging {
  this: MovieService =>

  implicit val ec: ExecutionContext
  implicit val db: MongoDatabase

  val moviesRoute =
    pathPrefix("movies") {
      pathEndOrSingleSlash {
        get {
          complete(list().map(MovieList.apply))
        } ~
          post {
            entity(as[NewMovie]) { newMovie =>
              complete {
                create(newMovie).map(toCreationResponse("movie"))
              }
            }
          }
      } ~
        pathPrefix(Segment) { id =>
          pathEndOrSingleSlash {
            get {
              complete(findById(id))
            }
          }
        }
    }
}

