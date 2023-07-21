// === scala settings ===

inThisBuild(
  List(
    scalaVersion := "3.3.0",
    scalacOptions ++= List(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-language:postfixOps"
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

sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

lazy val publishSettings = Seq(
  publish / skip := false,
  Test / publishArtifact := false,
  versionScheme := Some("early-semver")
)

// === project setting ===

lazy val root = (project in file(".")).settings(
  name := "scala-template",
  libraryDependencies ++= Dependencies.deps,
  publish / skip := true
)
//  .aggregate(subProject)

//lazy val subProject = (project in file("subProject")).settings(
//  name := "subProject",
//  publishSettings
//)
