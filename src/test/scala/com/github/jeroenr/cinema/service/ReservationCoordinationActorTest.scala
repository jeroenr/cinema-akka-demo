package com.github.jeroenr.cinema.service

import akka.actor.Props
import akka.testkit.{ TestActorRef, TestProbe }
import com.github.jeroenr.cinema.ActorTestBase
import com.github.jeroenr.cinema.persistence.Screening
import com.github.jeroenr.cinema.service.ReservationCoordinationActor._
import org.mongodb.scala.MongoDatabase

import scala.concurrent.Future
import scala.util.control.NoStackTrace

class ReservationCoordinationActorTest extends ActorTestBase {
  sequential

  implicit val db = MongoDatabase(null)

  def fakeUpdateFunc(id: (String, String), numSeats: Int) =
    Future.successful(1L)

  "ReservationCoordinationActor" should {
    "add reserve seat actor when adding screening" in {
      val reservationCoordinationActor = TestActorRef(new ReservationCoordinationActor(Nil, fakeUpdateFunc))
      reservationCoordinationActor.underlyingActor.reserveSeatActors must beEmpty
      reservationCoordinationActor ! ReservationCoordinationActor.AddScreening("foo", "bar", 2)
      reservationCoordinationActor.underlyingActor.reserveSeatActors.get(("foo", "bar")) must beSome
      expectMsg(ScreeningAdded)
      success
    }

    "remove reserve seat actor when removing screening" in {
      val reservationCoordinationActor = TestActorRef(new ReservationCoordinationActor(List(Screening("id", "foo", "bar", "movieTitle", 5, 5)), fakeUpdateFunc))
      reservationCoordinationActor.underlyingActor.reserveSeatActors.get(("bar", "foo")) must beSome
      reservationCoordinationActor ! RemoveScreening("bar", "foo")
      reservationCoordinationActor.underlyingActor.reserveSeatActors must beEmpty
      expectMsg(ScreeningRemoved)
      success
    }

    "try to reserve seat through child actor" in {
      val probe = TestProbe()
      val reservationCoordinationActor = TestActorRef(new ReservationCoordinationActor(List(Screening("id", "foo", "bar", "movieTitle", 5, 5)), fakeUpdateFunc) {
        override def createChildActor(movieId: String, screenId: String, availableSeats: Int) = {
          log.debug(s"Creating test child actor")
          probe.ref
        }
      })
      reservationCoordinationActor ! ReserveSeat("bar", "foo")
      probe.expectMsg(ReserveSeatActor.ReserveSeat)
      success
    }

    "pipe sold out message if no seat actor available" in {
      val reservationCoordinationActor = TestActorRef(new ReservationCoordinationActor(Nil, fakeUpdateFunc))
      reservationCoordinationActor ! ReserveSeat("bar", "foo")
      expectMsg(ReserveSeatActor.SoldOut)
      success
    }

  }
}
