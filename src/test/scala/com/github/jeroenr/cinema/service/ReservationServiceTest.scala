package com.github.jeroenr.cinema.service

import akka.testkit.{ TestActorRef, TestProbe }
import com.github.jeroenr.cinema.{ ActorTestBase, FakeDb }
import com.github.jeroenr.cinema.model.NewReservation
import com.github.jeroenr.cinema.persistence.ReservationDao

class ReservationServiceTest extends ActorTestBase with FakeDb {

  "ReservationServiceTest" should {
    "tell to remove screening if no seats available" in {
      val probe = TestProbe()
      val reservationService = new ReservationService(new ReservationDao {}, probe.ref)
      reservationService.create(NewReservation("bar", "foo"))
      probe.expectMsg(ReservationCoordinationActor.ReserveSeat("foo", "bar"))
      probe.reply(ReserveSeatActor.SeatRegistered(0))
      probe.expectMsg(ReservationCoordinationActor.RemoveScreening("foo", "bar"))
      success
    }

  }
}
