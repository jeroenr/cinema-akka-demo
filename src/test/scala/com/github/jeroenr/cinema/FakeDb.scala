package com.github.jeroenr.cinema

import org.mongodb.scala.MongoDatabase

trait FakeDb {
  implicit val db = MongoDatabase(null)
}
