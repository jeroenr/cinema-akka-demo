package com.github.jeroenr.cinema.model

import spray.json._

sealed trait ApiModel

case class MovieList(
  movies: List[Movie]
) extends ApiModel

case class NewMovie(
  movieTitle: String
) extends ApiModel

case class Movie(
  imdbId: String,
  movieTitle: String
) extends ApiModel

case class NewScreening(
  screenId: String, // externally managed
  imdbId: String,
  availableSeats: Int
) extends ApiModel

case class Screening(
  screenId: String,
  imdbId: String,
  availableSeats: Int,
  reservedSeats: Int,
  movieTitle: String
) extends ApiModel

case class ScreeningList(
  screenings: List[Screening]
) extends ApiModel

case class Reservation(
  id: String,
  screenId: String,
  movieId: String
) extends ApiModel

case class NewReservation(
  screenId: String,
  imdbId: String
) extends ApiModel

case class ReservationList(
  reservations: List[Reservation]
) extends ApiModel

object ApiModel extends DefaultJsonProtocol {
  implicit val movieFormat = jsonFormat2(Movie)
  implicit val newMovieFormat = jsonFormat1(NewMovie)
  implicit val movieListFormat = jsonFormat1(MovieList)
  implicit val newScreeningFormat = jsonFormat3(NewScreening)
  implicit val screeningFormat = jsonFormat5(Screening)
  implicit val screeningListFormat = jsonFormat1(ScreeningList)
  implicit val reservationFormat = jsonFormat3(Reservation)
  implicit val newReservationFormat = jsonFormat2(NewReservation)
  implicit val reservationListFormat = jsonFormat1(ReservationList)
}