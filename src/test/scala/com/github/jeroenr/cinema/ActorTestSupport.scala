package com.github.jeroenr.cinema

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.{ ImplicitSender, TestKit }
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.AfterAll

abstract class ActorTestBase extends TestKit(ActorSystem("test-actor-system")) with ImplicitSender with AfterAll with SpecificationLike {

  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
