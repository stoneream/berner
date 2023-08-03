// === scala settings ===

inThisBuild(
  List(
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
    homepage := Some(url("https://PROJECT-URL-HERE")),
    licenses := List("LICENSE NAME HERE" -> url("https://LICENSE-URL-HERE")),
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
  .settings(
    name := "berner",
    libraryDependencies ++= Dependencies.deps,
    publish / skip := true,
    dockerBaseImage := "azul/zulu-openjdk:11-latest",
    Compile / resourceDirectory := baseDirectory.value / "src" / "main" / "resources",
    Universal / javaOptions ++= List(
      "-Dpidfile.path=/dev/null"
    )
  )
  .enablePlugins(
    DockerPlugin,
    JavaAgent, // todo https://github.com/prometheus/jmx_exporter
    JavaAppPackaging,
    UniversalPlugin
  )
//  .aggregate(subProject)

//lazy val subProject = (project in file("subProject")).settings(
//  name := "subProject",
//  publishSettings
//)
