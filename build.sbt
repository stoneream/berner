lazy val baseSettings = Seq(
  // project info
  name := "berner",
  version := git.gitCurrentTags.value.headOption.getOrElse("0.0.0-SNAPSHOT"),
  organization := "io.github.stoneream",
  homepage := Some(url("https://github.com/stoneream/berner")),
  licenses := List("MIT License" -> url("https://github.com/stoneream/berner/blob/main/LICENSE")),
  developers := List(
    Developer(
      "stoneream",
      "Ishikawa Ryuto",
      "ishikawa-r@protonmail.com",
      url("https://github.com/stoneream")
    )
  ),
  // scala settings
  scalaVersion := "2.13.14",
  scalacOptions ++= Seq(
    "-Ywarn-unused",
    "-Yrangepos"
  ),
  // scalafix settings
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision,
  // scalafmt settings
  scalafmtOnCompile := true,
  // sbt-assembly settings
  assembly / assemblyMergeStrategy := {
    case PathList("META-INF", xs @ _*) =>
      (xs map { _.toLowerCase }) match {
        case "services" :: xs =>
          MergeStrategy.filterDistinctLines
        case _ => MergeStrategy.discard
      }
    case x if x.endsWith("module-info.class") => MergeStrategy.discard
    case "reference.conf" => MergeStrategy.concat
    case x =>
      val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
      oldStrategy(x)
  },
  assembly / assemblyJarName := s"${name.value}.jar"
)

lazy val root = (project in file("."))
  .settings(baseSettings)
  .aggregate(bot, logging)

lazy val logging = (project in file("logging"))
  .settings(baseSettings)
  .settings(
    name := "berner-logging",
    libraryDependencies ++= Dependencies.logging
  )

lazy val database = (project in file("database"))
  .settings(baseSettings)
  .settings(
    name := "berner-database",
    libraryDependencies ++= Dependencies.database
  )
  .enablePlugins(ScalikejdbcPlugin)

lazy val bot = (project in file("bot"))
  .settings(baseSettings)
  .settings(
    name := "berner-bot",
    libraryDependencies ++= Dependencies.bot,
    fork := true
  )
  .dependsOn(logging % "compile->compile;test->test")
  .dependsOn(database % "compile->compile;test->test")
