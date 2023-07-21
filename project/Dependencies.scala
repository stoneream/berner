import sbt._

object Dependencies {

  object Versions {
    val logback = "1.4.7"
    val typesafeConfig = "1.4.2"
    val scalatest = "3.2.15"
  }

  
  lazy val deps: Seq[ModuleID] = Seq(
    logback,
    typesafeConfig,
    scalatest
  ).flatten

  // logging
  lazy val logback: Seq[ModuleID] = Seq(
    "ch.qos.logback" % "logback-classic" % Versions.logback
  )

  // config
  lazy val typesafeConfig: Seq[ModuleID] = Seq(
    "com.typesafe" % "config" % Versions.typesafeConfig
  )

  lazy val scalatest: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % Versions.scalatest % Test
  )

}
