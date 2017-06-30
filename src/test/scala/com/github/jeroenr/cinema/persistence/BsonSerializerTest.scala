package com.github.jeroenr.cinema.persistence

import org.bson.BsonArray
import org.mongodb.scala.bson.BsonDocument
import org.specs2.mutable.Specification
import spray.json._

class BsonSerializerTest extends Specification {
  "JS to Mongo" should {
    "extend JSObject with implicits" in {
      JsObject().toBsonDoc must beAnInstanceOf[org.bson.BsonDocument]
    }

    "convert simple objects" in {
      """{
        |  "string": "test",
        |  "number": 1.3,
        |  "intNumber": 5,
        |  "bool": true}"""
        .stripMargin.parseJson.toBsonDoc mustEqual BsonDocument(
          "string" -> "test",
          "number" -> 1.3,
          "intNumber" -> 5.0,
          "bool" -> true
        )
    }

    "throw an exception on non JS object" in {
      JsString("hello").toBsonDoc must throwAn[IllegalArgumentException]
    }

    "deal with ID conversion to db Object" in {
      """{"id": "123"}"""
        .parseJson.toBsonDoc mustEqual BsonDocument(
          "_id" -> "123"
        )
    }

    "only convert ID of top level object, not nested ones" in {
      """{
        |  "id": "123",
        |  "nested": {
        |    "id": "456",
        |    "nested": {"id": "789"}
        |  }
        |}
      """.stripMargin.parseJson.toBsonDoc mustEqual BsonDocument(
        "_id" -> "123",
        "nested" -> BsonDocument("id" -> "456", "nested" -> BsonDocument("id" -> "789"))
      )
    }

    "convert nested objects" in {
      """{
        |  "rootString": "test",
        |  "nested": {"nestedString": "testNested"}
        |}"""
        .stripMargin.parseJson.toBsonDoc mustEqual BsonDocument(
          "rootString" -> "test",
          "nested" -> BsonDocument("nestedString" -> "testNested")
        )
    }
  }

  "Mongo to JS" should {
    "convert simple documents" in {
      BsonDocument(
        "string" -> "test",
        "number" -> 1.3,
        "intNumber" -> 5.0,
        "bool" -> true
      ).toJsObject mustEqual
        """{
          |  "string": "test",
          |  "number": 1.3,
          |  "intNumber": 5,
          |  "bool": true}"""
        .stripMargin.parseJson
    }

    "deal with lists" in {
      BsonDocument("list" -> new BsonArray()).toJsObject mustEqual
        """{"list": []}"""
        .stripMargin.parseJson
    }

    "translate ID" in {
      BsonDocument(
        "_id" -> "123",
        "name" -> "test"
      ).toJsObject mustEqual
        """{
          |  "id": "123",
          |  "name": "test"
          |}
        """.stripMargin.parseJson
    }

    "only translate top level id" in {
      BsonDocument(
        "_id" -> "123",
        "nested" -> BsonDocument(
          "_id" -> "456",
          "nested" -> BsonDocument("_id" -> "789")
        )
      ).toJsObject mustEqual
        """{
          |  "id": "123",
          |  "nested": {
          |    "_id": "456",
          |    "nested": {"_id": "789"}
          |  }
          |}
        """.stripMargin.parseJson
    }
  }
}

