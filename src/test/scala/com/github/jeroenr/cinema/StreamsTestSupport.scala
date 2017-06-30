package com.github.jeroenr.cinema

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

trait StreamsTestSupport {
  implicit val system = ActorSystem("test-actor-system")
  implicit val materializer = ActorMaterializer()
}
