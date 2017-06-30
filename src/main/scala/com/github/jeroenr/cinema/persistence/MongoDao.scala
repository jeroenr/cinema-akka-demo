package com.github.jeroenr.cinema.persistence

import org.mongodb.scala.bson._
import org.mongodb.scala.model.Filters._

import scala.concurrent.{ ExecutionContext, Future }
import org.mongodb.scala.{ Document, MongoCollection, MongoDatabase }
import org.slf4s.Logging
import com.mongodb.client.model.{ InsertOneModel, UpdateOptions }
import org.mongodb.scala.bson.conversions.Bson
import spray.json._

trait MongoDao[T] extends DefaultJsonProtocol with Logging {
  protected def collectionName: String

  protected[this] implicit def jsonFormat: JsonFormat[T]

  protected def col(db: MongoDatabase): MongoCollection[Document] =
    db.getCollection(collectionName)

  def findById(id: String)(implicit ec: ExecutionContext, db: MongoDatabase): Future[Option[T]] =
    findOne(equal("_id", id))

  def findOne(filter: conversions.Bson)(implicit ec: ExecutionContext, db: MongoDatabase): Future[Option[T]] = {
    findAllStream(filter).first().head().map {
      _.toBsonDocument.toJsObject.convertTo[T]
    }.map(Option.apply).recover {
      case t =>
        log.debug(s"Couldn't find entity based on $filter")
        None
    }
  }

  def findAll()(implicit ec: ExecutionContext, db: MongoDatabase): Future[Seq[T]] =
    findAllStream(BsonDocument()).toFuture().map(_.map(_.toBsonDocument.toJsObject.convertTo[T]))

  def findAll(filter: conversions.Bson)(implicit ec: ExecutionContext, db: MongoDatabase): Future[Seq[T]] =
    findAllStream(filter).toFuture().map(_.map(_.toBsonDocument.toJsObject.convertTo[T]))

  protected def findAllStream(filter: Bson)(implicit ec: ExecutionContext, db: MongoDatabase) =
    col(db).find(filter)

  def insertDoc(doc: Document)(implicit ec: ExecutionContext, db: MongoDatabase): Future[Boolean] =
    insertDocs(List(doc))

  def insertDocs(docs: List[Document])(implicit ec: ExecutionContext, db: MongoDatabase): Future[Boolean] =
    col(db).insertMany(docs).head().recover {
      case t =>
        log.error(s"Couldn't insert ${docs}", t)
        false
    }.map(_ => true)

  def insertOne(t: T)(implicit ec: ExecutionContext, db: MongoDatabase): Future[Boolean] =
    insert(List(t))

  def insert(ts: List[T])(implicit ec: ExecutionContext, db: MongoDatabase): Future[Boolean] =
    insertDocs(ts.map(_.toJson.toBsonDoc).map(Document.apply))

  def insertBulk(ts: List[T])(implicit ec: ExecutionContext, db: MongoDatabase) =
    if (ts.isEmpty) {
      Future.successful(0)
    } else insertBulkDocs(ts.map(_.toJson.toBsonDoc).map(Document.apply))

  def insertBulkDocs(docs: List[Document])(implicit ec: ExecutionContext, db: MongoDatabase) =
    col(db).bulkWrite(docs.map(doc => new InsertOneModel(doc))).head().map(_.getInsertedCount)

  def save(t: T)(implicit ec: ExecutionContext, db: MongoDatabase): Future[Long] =
    saveDoc(t.toJson.toBsonDoc)

  def saveDoc(doc: Document)(implicit ec: ExecutionContext, db: MongoDatabase): Future[Long] = {
    val idOrEmpty = doc.getOrElse("_id", doc.getOrElse("id", ""))
    updateDocInternal(idOrEmpty, doc, upsert = true)
  }

  def updateDoc(id: String, doc: Document)(implicit ec: ExecutionContext, db: MongoDatabase): Future[Long] = {
    updateDocInternal(BsonString(id), doc, upsert = false)
  }

  def removeById(id: String)(implicit ec: ExecutionContext, db: MongoDatabase): Future[Long] =
    col(db).deleteMany(equal("_id", id)).toFuture().map(_.getDeletedCount)

  private def updateDocInternal(id: BsonValue, doc: Document, upsert: Boolean)(implicit ec: ExecutionContext, db: MongoDatabase): Future[Long] = {
    col(db).updateOne(equal("_id", id), BsonDocument("$set" -> doc), new UpdateOptions().upsert(upsert)).head().map(_.getModifiedCount)
  }
}
