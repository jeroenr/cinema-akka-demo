package com.github.jeroenr.cinema.persistence

trait ReservationDao extends MongoDao[Reservation] {
  protected val collectionName = "Reservation"

  override protected[this] implicit def jsonFormat = CinemaModel.reservationFormat

}

object ReservationDao extends ReservationDao