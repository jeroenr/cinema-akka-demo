package com.github.jeroenr.cinema

import scala.concurrent._
import scala.concurrent.duration._

/**
 * Easier version for future tests from specs2 that just waits until a
 * timeout is reached for futures.
 */
trait FutureTestSupport {
  import scala.language.implicitConversions

  protected[this] val awaitTimeout = FutureTestSupport.DEFAULT_TIMEOUT

  implicit class PimpedFuture[T](future: Future[T]) {
    def await = Await.ready(future, awaitTimeout)
    def awaitResult = Await.result(future, awaitTimeout)
  }
}

object FutureTestSupport {
  val DEFAULT_TIMEOUT = 10.seconds
}