package com.github.jeroenr.cinema

import org.bson._
import org.mongodb.scala.bson.ObjectId
import scala.collection.Iterable
import scala.collection.JavaConversions._
import spray.json._

/**
 * A set of utility methods and implicits that convert spray json objects into Mongo objects.
 */
package object persistence {

  implicit class RichMap[T: JsonFormat](map: Map[String, T]) extends DefaultJsonProtocol {
    def toBsonDoc: BsonDocument =
      map.toJson.toBsonDoc
  }

  implicit class PimpedJsValue(jsValue: JsValue) {
    def toBsonDoc: BsonDocument = jsValue match {
      case jsObject: JsObject => jsObjectToDbObject0(jsObject, root = true)
      case otherValue => throw new IllegalArgumentException("Cannot convert simple value '" + otherValue + "' to object")
    }

    private def jsObjectToDbObject0(jsObject: JsObject, root: Boolean): BsonDocument = jsObject.fields.map {
      case ("id", value) if root => "_id" -> jsValueToDbObject0(value)
      case (name, value) => name -> jsValueToDbObject0(value)
    }.foldLeft(new BsonDocument) {
      case (acc, (key, v)) => acc.append(key, v)
    }

    private def jsValueToDbObject0(jsValue: JsValue): BsonValue = jsValue match {
      case JsNumber(decimal) => new BsonDouble(decimal.toDouble)
      case JsString(str) => new BsonString(str)
      case JsBoolean(bool) => new BsonBoolean(bool)
      case JsArray(array) => new BsonArray(array.map(e => jsValueToDbObject0(e)))
      case JsNull => new BsonNull
      case jsObj: JsObject => jsObjectToDbObject0(jsObj, root = false)
      case o => throw new IllegalArgumentException("Could not parse type " + o.getClass)
    }
  }

  implicit class PimpedDbObject(doc: BsonDocument) {
    private def toJsObject(doc: BsonDocument, root: Boolean): JsObject = new JsObject(doc.toMap.mapValues {
      case bstr: BsonString => JsString(bstr.getValue)
      case nr: BsonNumber => JsNumber(nr.doubleValue())
      case bool: BsonBoolean => JsBoolean(bool.getValue)
      case arr: BsonArray => JsArray(arr.toArray.map {
        case bstr: BsonString => JsString(bstr.getValue)
        case nr: BsonNumber => JsNumber(nr.doubleValue())
        case bool: BsonBoolean => JsBoolean(bool.getValue)
        case obj: BsonDocument => toJsObject(obj, root = false)
        case o => throw new IllegalArgumentException("Cannot parse type " + o.getClass)
      }: _*)
      case obj: BsonDocument => toJsObject(obj, root = false)
      case _: BsonNull => JsNull
      case null => JsNull
      case o => throw new IllegalArgumentException("Cannot parse type " + o.getClass)
    }.map {
      case (key, value) => key match {
        case "_id" if root => "id" -> value
        case _ => key -> value
      }
    }.toMap)

    def toJsObject: JsObject = toJsObject(doc, root = true)
  }

}
