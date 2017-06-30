package com.github.jeroenr.cinema.persistence

import spray.json._

sealed trait CinemaModel

case class Screening(
  id: String,
  movieId: String,
  movieTitle: String,
  totalSeats: Int,
  availableSeats: Int
) extends CinemaModel

case class Movie(
  id: String,
  title: String
) extends CinemaModel

case class Reservation(
  id: String,
  screenId: String
) extends CinemaModel

object CinemaModel extends DefaultJsonProtocol {
  implicit val movieFormat = jsonFormat2(Movie)
  implicit val reservationFormat = jsonFormat2(Reservation)
  implicit val screeningFormat = jsonFormat5(Screening)
}