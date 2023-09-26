// === scala settings ===

inThisBuild(
  List(
    version := git.gitCurrentTags.value.headOption.getOrElse("0.0.0-SNAPSHOT"),
    scalaVersion := "2.13.11",
    scalacOptions ++= List(
      "-Ywarn-unused",
      "-Yrangepos"
    ),
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalafixScalaBinaryVersion := CrossVersion.binaryScalaVersion(scalaVersion.value),
    scalafmtOnCompile := true
  )
)

// === project info ===

inThisBuild(
  List(
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
    )
  )
)

// === publish settings ===

// todo

// === project setting ===

lazy val root = (project in file("."))
  .enablePlugins(
    DockerPlugin,
    JavaAgent, // todo https://github.com/prometheus/jmx_exporter
    JavaAppPackaging,
    UniversalPlugin
  )
  .settings(
    name := "berner",
    libraryDependencies ++= Dependencies.deps,
    publish / skip := true,
    Compile / resourceDirectory := baseDirectory.value / "src" / "main" / "resources",
    Universal / javaOptions ++= List(
      "-Dpidfile.path=/dev/null"
    ),
    dockerBaseImage := "azul/zulu-openjdk:11-latest"
  )
//  .aggregate(subProject)

//lazy val subProject = (project in file("subProject")).settings(
//  name := "subProject",
//  publishSettings
//)
