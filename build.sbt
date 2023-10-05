lazy val baseConfig: Project => Project =
  _.settings(
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
    version := git.gitCurrentTags.value.headOption.getOrElse("0.0.0-SNAPSHOT"),
    scalaVersion := "2.13.11",
    scalacOptions ++= Seq(
      "-Ywarn-unused",
      "-Yrangepos"
    ),
    // scalafix settings
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    ThisBuild / scalafixScalaBinaryVersion := CrossVersion.binaryScalaVersion(scalaVersion.value),
    // scalafmt settings
    scalafmtOnCompile := true
  )

// JMX Settings

lazy val jmxExporterPort = 9090

// Docker用のJavaAgent設定
lazy val dockerJavaAgentSetting = JavaAgent(
  Dependencies.jmxExporterJavaAgent,
  arguments = s"$jmxExporterPort:/opt/docker/conf/jmx_exporter_config.yml"
)

// ローカル実行用のJavaAgent設定 (あんまり使わない)
lazy val defaultJavaAgentsSetting = JavaAgent(
  Dependencies.jmxExporterJavaAgent,
  scope = JavaAgent.AgentScope(run = true),
  arguments = s"$jmxExporterPort:${file("src/universal/conf/jmx_exporter_config.yml").getAbsolutePath}"
)

lazy val root = (project in file("."))
  .enablePlugins(
    DockerPlugin,
    JavaAgent,
    JavaAppPackaging
  )
  .configure(baseConfig)
  .settings(
    name := "berner",
    libraryDependencies ++= Dependencies.deps,
    Compile / resourceDirectory := baseDirectory.value / "src" / "main" / "resources",
    Universal / javaOptions ++= Seq("-Dpidfile.path=/dev/null"),
    dockerBaseImage := "azul/zulu-openjdk:11-latest",
    dockerUsername := Some("stoneream"),
    dockerExposedPorts := Seq(jmxExporterPort),
    javaAgents += dockerJavaAgentSetting
  )
