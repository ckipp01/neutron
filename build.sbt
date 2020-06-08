import Dependencies._

ThisBuild / crossScalaVersions := Seq("2.12.10", "2.13.2")
ThisBuild / homepage := Some(url("https://github.com/cr-org/neutron"))
ThisBuild / organization := "com.chatroulette"
ThisBuild / organizationName := "Chatroulette"
ThisBuild / startYear := Some(2020)
ThisBuild / licenses := List(
  "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
)
ThisBuild / headerLicenseStyle := HeaderLicenseStyle.SpdxSyntax
ThisBuild / developers := List(
  Developer(
    "agjacome",
    "Alberto G. Jácome",
    "alberto.jacome@chatroulette.com",
    url("https://github.com/agjacome")
  ),
  Developer(
    "AndreasKostler",
    "Andreas Kostler",
    "andreas.kostler@chatroulette.com",
    url("https://github.com/AndreasKostler")
  ),
  Developer(
    "gvolpe",
    "Gabriel Volpe",
    "gabriel.volpe@chatroulette.com",
    url("https://gvolpe.github.io")
  ),
  Developer(
    "psisoyev",
    "Pavels Sisojevs",
    "pavels.sisojevs@chatroulette.com",
    url("https://scala.monster/")
  ),
  Developer(
    "tabdulazim",
    "Tamer Abdulazim",
    "tamer.abdulazim@chatroulette.com",
    url("https://github.com/tabdulazim")
  )
)

def compilerFlags(v: String) =
  CrossVersion.partialVersion(v) match {
    case Some((2, 13)) => List("-Ymacro-annotations")
    case _             => List.empty
  }

def macroParadisePlugin(v: String) =
  CrossVersion.partialVersion(v) match {
    case Some((2, 13)) => List.empty
    case _             => List(CompilerPlugins.macroParadise)
  }

lazy val root = (project in file("."))
  .settings(
    name := "neutron",
    scalacOptions ++= compilerFlags(scalaVersion.value),
    scalafmtOnCompile := true,
    autoAPIMappings := true,
    testFrameworks += new TestFramework("munit.Framework"),
    libraryDependencies ++= List(
          CompilerPlugins.betterMonadicFor,
          CompilerPlugins.contextApplied,
          CompilerPlugins.kindProjector,
          Libraries.cats,
          Libraries.catsEffect,
          Libraries.fs2,
          Libraries.newtype,
          Libraries.pulsar,
          Libraries.munitCore       % Test,
          Libraries.munitScalacheck % Test
        ),
    libraryDependencies ++= macroParadisePlugin(scalaVersion.value)
  )
  .enablePlugins(AutomateHeaderPlugin)
