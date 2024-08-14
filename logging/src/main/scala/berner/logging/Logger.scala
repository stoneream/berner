package berner.logging

import org.slf4j.LoggerFactory

trait Logger {
  private val logger = LoggerFactory.getLogger(getClass)

  def debug(message: String): Unit = logger.debug(message)

  def info(message: String): Unit = logger.info(message)

  def warn(message: String): Unit = logger.warn(message)

  def warn(message: String, t: Throwable): Unit = logger.warn(message, t)

  def error(message: String, t: Throwable): Unit = logger.error(message, t)
}
