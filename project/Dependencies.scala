import sbt._

object Dependencies {

  lazy val bot: Seq[sbt.ModuleID] = Seq(
    catsIO,
    jda,
    scalikejdbc,
    mariadb,
    typesafeConfig,
    circe,
    zip4j
  ).flatten

  // logging
  lazy val logging: Seq[ModuleID] = Seq(
    "ch.qos.logback" % "logback-classic" % "1.5.6",
    "net.logstash.logback" % "logstash-logback-encoder" % "8.0"
  )

  // config
  lazy val typesafeConfig: Seq[ModuleID] = Seq(
    "com.typesafe" % "config" % "1.4.3"
  )

  // cli option parser
  lazy val scopt: Seq[ModuleID] = Seq(
    "com.github.scopt" %% "scopt" % "4.1.0"
  )

  // circe
  lazy val circeVersion = "0.14.1"
  lazy val circe: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-literal" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-optics" % circeVersion,
    "io.circe" %% "circe-generic-extras" % circeVersion
  )

  // database
  lazy val mariadb: Seq[ModuleID] = Seq(
    "org.mariadb.jdbc" % "mariadb-java-client" % "3.4.1" excludeAll (
      ExclusionRule("org.slf4j", "jcl-over-slf4j") // 依存がぶつかるので除外
    )
  )

  val scalikejdbcVersion = "4.3.0"
  lazy val scalikejdbc: Seq[ModuleID] = Seq(
    "org.scalikejdbc" %% "scalikejdbc" % scalikejdbcVersion,
    "org.scalikejdbc" %% "scalikejdbc-config" % scalikejdbcVersion,
    "org.scalikejdbc" %% "scalikejdbc-syntax-support-macro" % scalikejdbcVersion
  )

  // discord
  lazy val jda: Seq[ModuleID] = Seq(
    "net.dv8tion" % "JDA" % "5.4.0",
    "club.minnced" % "discord-webhooks" % "0.8.4"
  )

  lazy val zip4j: Seq[ModuleID] = Seq(
    "net.lingala.zip4j" % "zip4j" % "2.11.5"
  )

  lazy val catsIO: Seq[ModuleID] = Seq(
    "org.typelevel" %% "cats-effect" % "3.5.7"
  )

  // test
  lazy val test: Seq[sbt.ModuleID] = scalatest

  lazy val scalatest: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % "3.2.19" % Test
  )

}
