package com.github.jeroenr.cinema.persistence

trait MovieDao extends MongoDao[Movie] {
  protected val collectionName = "Movie"

  override protected[this] implicit def jsonFormat = CinemaModel.movieFormat

}

object MovieDao extends MovieDao