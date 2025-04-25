package lib

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.slf4j.LoggerFactory

trait TestBase extends AnyFunSuite with BeforeAndAfterEach with BeforeAndAfterAll {
  protected val logger = LoggerFactory.getLogger(getClass)
}
