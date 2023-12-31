import sbt._

object Dependencies {

  object Versions {
    val logback = "1.4.7"
    val jmxExporterJavaAgent = "0.20.0"
    val typesafeConfig = "1.4.2"
    val cats = "2.9.0"
    val log4cats = "2.6.0"
    val catsEffect = "3.5.0"
    val catsMTL = "1.3.1"
    val circe = "0.14.1"
    val http4s = "0.23.21"
    val http4sJDKHttpClient = "0.9.1"
    val mariadbJavaClient = "3.1.4"
    val doobie = "1.0.0-RC4"
    val scalatest = "3.2.15"
  }

  val deps: Seq[ModuleID] = Seq(
    logback,
    Seq(jmxExporterJavaAgent),
    Seq(typesafeConfig),
    Seq(scopt),
    cats,
    log4cats,
    http4s,
    circe,
    db,
    test
  ).flatten

  // logging
  lazy val logback: Seq[ModuleID] = Seq(
    "ch.qos.logback" % "logback-classic" % Versions.logback
  )

  // metrics
  lazy val jmxExporterJavaAgent: ModuleID = "io.prometheus.jmx" % "jmx_prometheus_javaagent" % Versions.jmxExporterJavaAgent

  // config
  lazy val typesafeConfig: ModuleID = "com.typesafe" % "config" % Versions.typesafeConfig

  // cli option parser
  lazy val scopt: ModuleID = "com.github.scopt" %% "scopt" % "4.1.0"

  // cats
  lazy val cats: Seq[ModuleID] = catsCore ++ log4cats ++ catsEffect ++ catsMTL

  lazy val catsCore: Seq[ModuleID] = Seq(
    "org.typelevel" %% "cats-core" % Versions.cats,
    "org.typelevel" %% "cats-laws" % Versions.cats,
    "org.typelevel" %% "cats-free" % Versions.cats
  )
  lazy val log4cats: Seq[ModuleID] = Seq(
    "org.typelevel" %% "log4cats-core" % Versions.log4cats,
    "org.typelevel" %% "log4cats-slf4j" % Versions.log4cats
  )
  lazy val catsEffect: Seq[ModuleID] = Seq(
    "org.typelevel" %% "cats-effect" % Versions.catsEffect
  )
  lazy val catsMTL: Seq[ModuleID] = Seq(
    "org.typelevel" %% "cats-mtl" % Versions.catsMTL
  )

  // http4s
  lazy val http4s: Seq[ModuleID] = Seq(
    "org.http4s" %% "http4s-dsl" % Versions.http4s,
    "org.http4s" %% "http4s-circe" % Versions.http4s,
    "org.http4s" %% "http4s-jdk-http-client" % Versions.http4sJDKHttpClient
  )

  // circe
  lazy val circe: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-generic" % Versions.circe,
    "io.circe" %% "circe-literal" % Versions.circe,
    "io.circe" %% "circe-parser" % Versions.circe,
    "io.circe" %% "circe-optics" % Versions.circe,
    "io.circe" %% "circe-generic-extras" % Versions.circe
  )

  // database
  lazy val db: Seq[ModuleID] = mariadb ++ doobie

  lazy val mariadb: Seq[ModuleID] = Seq(
    "org.mariadb.jdbc" % "mariadb-java-client" % Versions.mariadbJavaClient
  )

  lazy val doobie: Seq[ModuleID] = Seq(
    "org.tpolecat" %% "doobie-core" % Versions.doobie,
    "org.tpolecat" %% "doobie-hikari" % Versions.doobie
  )

  // test
  lazy val test: Seq[sbt.ModuleID] = scalatest

  lazy val scalatest: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % Versions.scalatest % Test
  )

}
