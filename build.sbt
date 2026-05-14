import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

def sbt2 = "2.0.0-RC12"
def Scala212 = "2.12.21"
val Scala3 = scala_version_from_sbt_version.ScalaVersionFromSbtVersion(sbt2)

val baseSettings = Def.settings(
  publishTo := (if (isSnapshot.value) None else localStaging.value),
  Compile / unmanagedResources += (LocalRootProject / baseDirectory).value / "LICENSE.txt",
  Compile / doc / scalacOptions ++= {
    val hash = sys.process.Process("git rev-parse HEAD").lineStream_!.head
    if (scalaBinaryVersion.value != "3") {
      Seq(
        "-sourcepath",
        (LocalRootProject / baseDirectory).value.getAbsolutePath,
        "-doc-source-url",
        s"https://github.com/xuwei-k/coherence/blob/${hash}€{FILE_PATH}.scala"
      )
    } else {
      Seq(
        "-source-links:github://xuwei-k/coherence",
        "-revision",
        hash
      )
    }
  },
  scalacOptions ++= {
    scalaBinaryVersion.value match {
      case "3" =>
        Seq(
          "-Wunused:all",
        )
      case "2.13" =>
        Seq(
          "-Xsource:3-cross",
        )
      case "2.12" =>
        Seq(
          "-Xsource:3",
        )
    }
  },
  scalacOptions ++= Seq(
    "-deprecation",
  ),
  pomExtra := (
    <developers>
      <developer>
        <id>xuwei-k</id>
        <name>Kenji Yoshida</name>
        <url>https://github.com/xuwei-k</url>
      </developer>
    </developers>
    <scm>
      <url>git@github.com:xuwei-k/coherence.git</url>
      <connection>scm:git:git@github.com:xuwei-k/coherence.git</connection>
    </scm>
  ),
  organization := "com.github.xuwei-k",
  homepage := Some(url("https://github.com/xuwei-k/coherence")),
  licenses := List(License.MIT),
)

val common = projectMatrix
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(Seq(Scala212, Scala3))
  .settings(
    baseSettings,
    name := "coherence-common",
    libraryDependencies += {
      scalaBinaryVersion.value match {
        case "3" =>
          "com.eed3si9n" %% "sjson-new-scalajson" % sjsonNewVersion(sbt2, "3")
        case _ =>
          "com.eed3si9n" %% "sjson-new-scalajson" % sjsonNewVersion(sbtVersion.value, "2.12")
      }
    },
    Compile / sourceGenerators += task {
      val dir = (Compile / sourceManaged).value
      val className = "CoherenceBuildInfo"
      val pkg = "coherence"
      val f = dir / pkg / s"${className}.scala"
      IO.write(
        f,
        Seq(
          s"package $pkg",
          "",
          s"object $className {",
          s"""  def version: String = "${version.value}" """,
          "}",
        ).mkString("", "\n", "\n")
      )
      Seq(f)
    },
  )

val core = project
  .settings(
    baseSettings,
    name := "coherence",
    scalaVersion := Scala3,
    libraryDependencies ++= Seq(
      "org.scala-lang" %% "scala3-tasty-inspector" % scalaVersion.value,
    )
  )
  .dependsOn(common.jvm(Scala3))

val plugin = projectMatrix
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(Seq(Scala212, Scala3))
  .enablePlugins(SbtPlugin)
  .settings(
    baseSettings,
    pluginCrossBuild / sbtVersion := (
      scalaBinaryVersion.value match {
        case "2.12" =>
          sbtVersion.value
        case _ =>
          sbt2
      }
    ),
    name := "coherence-plugin",
    scriptedLaunchOpts += s"-Dplugin.version=${version.value}",
    scriptedBufferLog := false,
  )
  .dependsOn(common)

val root = project
  .in(file("."))
  .settings(
    baseSettings,
    autoScalaLibrary := false,
    publish / skip := true,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommandAndRemaining("publishSigned"),
      releaseStepCommandAndRemaining("sonaRelease"),
      setNextVersion,
      commitNextVersion,
      pushChanges
    ),
  )
  .aggregate(core)
  .aggregate(plugin.projectRefs *)
  .aggregate(common.projectRefs *)

def sjsonNewVersion(sbtV: String, scalaBinaryV: String): String = {
  import lmcoursier.internal.shaded.coursier
  val dependency = coursier.Dependency(
    coursier.Module(
      coursier.Organization(
        "org.scala-sbt"
      ),
      coursier.ModuleName(
        "sbt"
      )
    ),
    sbtV
  )
  coursier.Fetch().addDependencies(dependency).runResult().detailedArtifacts.map(_._1).collect {
    case x
        if (x.module.organization.value == "com.eed3si9n") && (x.module.name.value == s"sjson-new-scalajson_${scalaBinaryV}") =>
      x.version
  } match {
    case Seq(x) =>
      x
    case Nil =>
      sys.error("not found")
    case xs =>
      sys.error(xs.toString)
  }
}

ThisBuild / scalafixDependencies += "com.github.xuwei-k" %% "scalafix-rules" % "0.6.25"
