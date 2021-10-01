import Dependencies._

ThisBuild / scalaVersion := "2.13.5"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "dev.profunktor"
ThisBuild / organizationName := "ProfunKtor"
ThisBuild / homepage := Some(url("https://neutron.profunktor.dev"))
ThisBuild / licenses := List(
  "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
)
ThisBuild / crossScalaVersions := List("2.13.5", "3.0.2")
ThisBuild / startYear := Some(2021)

ThisBuild / developers := List(
  Developer(
    "gvolpe",
    "Gabriel Volpe",
    "volpegabriel@gmail.com",
    url("https://gvolpe.com")
  )
)

ThisBuild / scalafixDependencies += Libraries.organizeImports

resolvers += Resolver.sonatypeRepo("snapshots")

Compile / run / fork := true
Global / semanticdbEnabled := true

def avro4sDep(scalaVersion: String, scope: String = Provided.toString): List[ModuleID] =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, _)) =>
      List(Libraries.avro4s % scope)
    case Some((3, _)) =>
      List(Libraries.avro4s_scala3 % scope)
    case _ => Nil
  }

def kindProjectorDep(scalaVersion: String): List[ModuleID] =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, _)) =>
      List(
        CompilerPlugins.kindProjector
      )
    case _ => Nil
  }

val commonSettings = Seq(
  scalacOptions -= "-Wunused:params", // so many false-positives :(
  scalafmtOnCompile := true,
  autoAPIMappings := true,
  testFrameworks += new TestFramework("weaver.framework.CatsEffect")
)

lazy val noPublish = {
  publish / skip := true
}

lazy val `neutron-core` = (project in file("core"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= List(
          Libraries.cats,
          Libraries.catsEffect,
          Libraries.fs2,
          Libraries.pulsar,
          Libraries.weaverCats % Test
        ) ++ kindProjectorDep(scalaVersion.value)
  )

lazy val `neutron-circe` = (project in file("circe"))
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(`neutron-core`)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= List(
          Libraries.circeCore,
          Libraries.circeParser
        ) ++ avro4sDep(scalaVersion.value)
  )

lazy val `neutron-function` = (project in file("function"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= List(
          Libraries.pulsarFunctionsApi,
          Libraries.cats             % Test,
          Libraries.catsEffect       % Test,
          Libraries.cats             % Test,
          Libraries.weaverCats       % Test,
          Libraries.weaverScalaCheck % Test
        ) ++ avro4sDep(scalaVersion.value)
  )

lazy val tests = (project in file("tests"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .configs(IntegrationTest)
  .settings(
    noPublish,
    Defaults.itSettings,
    libraryDependencies ++= List(
          Libraries.circeCore    % "it,test",
          Libraries.circeGeneric % "it,test",
          Libraries.circeParser  % "it,test",
          Libraries.weaverCats   % "it,test"
        ) ++ avro4sDep(scalaVersion.value, "it,test") ++ kindProjectorDep(scalaVersion.value)
  )
  .dependsOn(`neutron-circe`)

lazy val docs = (project in file("docs"))
  .dependsOn(`neutron-circe`)
  .enablePlugins(ParadoxSitePlugin)
  .enablePlugins(ParadoxMaterialThemePlugin)
  .enablePlugins(GhpagesPlugin)
  .enablePlugins(MdocPlugin)
  .settings(
    noPublish,
    libraryDependencies ++= List(
          Libraries.circeGeneric
        ) ++ avro4sDep(scalaVersion.value),
    scalacOptions -= "-Xfatal-warnings",
    scmInfo := Some(
          ScmInfo(
            url("https://github.com/profunktor/neutron"),
            "scm:git:git@github.com:profunktor/neutron.git"
          )
        ),
    git.remoteRepo := scmInfo.value.get.connection.replace("scm:git:", ""),
    ghpagesNoJekyll := true,
    version := version.value.takeWhile(_ != '+'),
    paradoxProperties ++= Map(
          "scala-versions" -> (`neutron-core` / crossScalaVersions).value
                .map(CrossVersion.partialVersion)
                .flatten
                .map(_._2)
                .mkString("2.", "/", ""),
          "org" -> organization.value,
          "scala.binary.version" -> s"2.${CrossVersion.partialVersion(scalaVersion.value).get._2}",
          "neutron-core" -> s"${(`neutron-core` / name).value}_2.${CrossVersion.partialVersion(scalaVersion.value).get._2}",
          "neutron-circe" -> s"${(`neutron-circe` / name).value}_2.${CrossVersion.partialVersion(scalaVersion.value).get._2}",
          "version" -> version.value
        ),
    mdocIn := (Paradox / sourceDirectory).value,
    mdocExtraArguments ++= Seq("--no-link-hygiene"),
    makeSite := makeSite.dependsOn(mdoc.toTask("")).value,
    ParadoxMaterialThemePlugin.paradoxMaterialThemeSettings(Paradox),
    Paradox / paradoxMaterialTheme := {
      ParadoxMaterialTheme()
        .withColor("red", "orange")
        .withLogoIcon("flash_on")
        .withCopyright("Copyright © ProfunKtor")
        .withRepository(uri("https://github.com/profunktor/neutron"))
    }
  )

lazy val root = (project in file("."))
  .settings(name := "neutron")
  .settings(noPublish)
  .aggregate(
    `neutron-circe`,
    `neutron-core`,
    `neutron-function`,
    docs,
    tests
  )

addCommandAlias("runLinter", ";scalafixAll --rules OrganizeImports")
