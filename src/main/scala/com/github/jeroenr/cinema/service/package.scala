package com.github.jeroenr.cinema

import com.github.jeroenr.cinema.common.Logging
import spray.json.DefaultJsonProtocol

import scala.util.{ Failure, Success, Try }

package object service extends Logging {
  def uuid = java.util.UUID.randomUUID.toString

  type MovieAndScreen = (String, String)

  sealed trait ResponseModel

  case class EntityCreated(
    id: Option[String],
    error: Option[String]
  ) extends ResponseModel

  object ResponseModel extends DefaultJsonProtocol {
    implicit val entityCreatedFormat = jsonFormat2(EntityCreated)
  }

  def toResponse(entityType: String): PartialFunction[Try[String], EntityCreated] = {
    case Success(id) => EntityCreated(id = Some(id), error = None)
    case Failure(t) =>
      log.error(s"Couldn't add $entityType", t)
      EntityCreated(id = None, error = Some(s"Couldn't add $entityType. ${t.getMessage}"))
  }
}
