package berner.logging

import net.logstash.logback.argument.{StructuredArgument, StructuredArguments}
import org.slf4j
import org.slf4j.LoggerFactory

trait Logger {
  protected val logger: slf4j.Logger = LoggerFactory.getLogger(getClass)

  protected val kv: (String, Any) => StructuredArgument = StructuredArguments.kv(_: String, _: Any)
}
