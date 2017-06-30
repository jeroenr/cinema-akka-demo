package com.github.jeroenr.cinema.service

import akka.actor.Props
import akka.testkit.TestProbe
import com.github.jeroenr.cinema.ActorTestBase
import org.mongodb.scala.MongoDatabase

import scala.concurrent.Future
import scala.util.control.NoStackTrace

class ReserveSeatActorTest extends ActorTestBase {
  sequential

  implicit val db = MongoDatabase(null)

  def fakeUpdateFunc(id: (String, String), numSeats: Int) =
    Future.successful(1L)

  def failingUpdateFunc(id: (String, String), numSeats: Int) =
    Future.failed(new RuntimeException("Booom!") with NoStackTrace)

  "ReserveSeatActor" should {
    "register seat until not available" in {
      val reserveSeatActor = system.actorOf(Props(new ReserveSeatActor(("foo", "bar"), 2, fakeUpdateFunc)))
      reserveSeatActor ! ReserveSeatActor.ReserveSeat
      expectMsg(ReserveSeatActor.SeatRegistered(1))
      reserveSeatActor ! ReserveSeatActor.ReserveSeat
      expectMsg(ReserveSeatActor.SeatRegistered(0))
      reserveSeatActor ! ReserveSeatActor.ReserveSeat
      expectMsg(ReserveSeatActor.SoldOut)
      success
    }

    "handle failure of update func" in {
      val reserveSeatActor = system.actorOf(Props(new ReserveSeatActor(("foo", "bar"), 2, failingUpdateFunc)))
      reserveSeatActor ! ReserveSeatActor.ReserveSeat
      expectMsg(ReserveSeatActor.Error("Couldn't persist seat reservation"))
      success
    }

    "terminate after no availability" in {
      val reserveSeatActor = system.actorOf(Props(new ReserveSeatActor(("foo", "bar"), 0, fakeUpdateFunc)))
      val probe = TestProbe()
      probe watch reserveSeatActor
      reserveSeatActor ! ReserveSeatActor.ReserveSeat
      probe.expectTerminated(reserveSeatActor)
      success
    }

  }
}
