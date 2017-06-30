package com.github.jeroenr.cinema.common

import org.slf4s.{ Logging => SLF4SLogging }

trait Logging extends SLF4SLogging {
  @inline
  protected[this] lazy val logger = log
}
