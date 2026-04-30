package coherence

import java.nio.file.Files
import sbt.*
import sbt.Keys._
import sbt.plugins.JvmPlugin

object CoherencePlugin extends AutoPlugin with CoherencePluginCompat {
  object autoImport {
    @transient
    val coherenceCheck = taskKey[Output]("")
    @transient
    val coherenceWrite = taskKey[File]("")
    val coherenceWriteFile = settingKey[File]("")
    val coherenceScalaVersion = settingKey[Option[String]]("")
    @transient
    val coherenceInput = taskKey[Input]("")
    val coherenceExcludeTypes = settingKey[Seq[String]]("")
  }

  override def trigger: PluginTrigger = allRequirements

  override def requires: Plugins = JvmPlugin

  import autoImport.*

  private def getClassDirectories(id: String) = Def.setting {
    Seq(
      (LocalProject(id) / Compile / classDirectory).value,
      (LocalProject(id) / Test / classDirectory).value
    )
  }

  private val coherenceProjects = Def.task {
    val extracted = Project.extract(state.value)
    val currentBuildUri = extracted.currentRef.build
    extracted.structure.units
      .apply(currentBuildUri)
      .defined
      .values
      .filter(_.autoPlugins.contains(CoherencePlugin))
      .map(_.id)
      .filter(p => extracted.get(LocalProject(p) / scalaBinaryVersion) == "3")
      .toList
      .sorted
  }

  override def buildSettings: Seq[Def.Setting[?]] = Def.settings(
    LocalRootProject / coherenceWriteFile := file("target/coherence.json"),
    LocalRootProject / coherenceWrite := {
      val f = (LocalRootProject / coherenceWriteFile).value
      IO.write(
        f,
        (LocalRootProject / coherenceCheck).value.toJsonString
      )
      f
    },
    LocalRootProject / coherenceScalaVersion := None,
    LocalRootProject / coherenceCheck / forkOptions := Def.uncached(
      ForkOptions()
    ),
    LocalRootProject / coherenceCheck := Def.taskDyn {
      val projects = coherenceProjects.value
      val log = (LocalRootProject / streams).value.log
      if (projects.isEmpty) {
        log.warn("There are no Scala 3 projects")
      }
      val forkOpt = (LocalRootProject / coherenceCheck / forkOptions).value
      val launcher = sbtLauncher(coherenceCheck).value
      val input = (LocalRootProject / coherenceInput).value
      val scalaVersionSetting = (LocalRootProject / coherenceScalaVersion).value match {
        case Some(v) =>
          assert(VersionNumber(v)._1.contains(3L), s"Invalid ${coherenceScalaVersion.key.label} ${v}")
          s"""|scalaVersion := "${v}"
              |""".stripMargin
        case None =>
          """|autoScalaLibrary := false
             |
             |scalaVersion := scalaVersion.value""".stripMargin
      }
      val buildSbt =
        s"""|${scalaVersionSetting}
            |
            |run / fork := true
            |
            |libraryDependencies ++= Seq(
            |  "com.github.xuwei-k" % "coherence_3" % "${CoherenceBuildInfo.version}"
            |)
            |""".stripMargin

      val base = (LocalRootProject / baseDirectory).value

      Def.task {
        IO.withTemporaryDirectory { dir =>
          val allSources = projects
            .flatMap(id =>
              Seq(
                LocalProject(id) / Compile / sources,
                LocalProject(id) / Test / sources
              )
            )
            .join
            .value
            .flatten
          Files.writeString((dir / "build.sbt").toPath, buildSbt)
          Files.writeString((dir / "input.json").toPath, input.toJsonString)
          IO.copy(
            allSources.flatMap { f =>
              IO.relativize(base, f).map { x =>
                f -> (dir / x)
              }
            }
          )
          val res = Fork.java.apply(
            forkOpt.withWorkingDirectory(dir),
            Seq(
              "-jar",
              launcher.getCanonicalPath,
              Seq(
                "runMain",
                "coherence.Coherence",
              ).mkString(" ")
            )
          )
          log.info(s"res = $res")
          Output.fromJson(
            IO.read(dir / "output.json")
          )
        }
      }
    }.value,
    LocalRootProject / coherenceExcludeTypes := Nil,
    LocalRootProject / coherenceInput := Def.taskDyn {
      val projects: List[String] = coherenceProjects.value
      val directories = projects.map(getClassDirectories)
      val allClasspath = projects.map { p =>
        Def.task {
          Seq(
            convertClasspath((LocalProject(p) / Compile / fullClasspath).value),
            convertClasspath((LocalProject(p) / Test / fullClasspath).value),
          ).flatten
        }
      }.join

      Def.taskDyn {
        val x = allClasspath.value
        Def.task {
          Input(
            tastyDirectories = directories.join.value.flatten.map(_.getAbsolutePath),
            classpath = x.join.value.flatten.sorted.distinct,
            excludeTypes = (LocalRootProject / coherenceExcludeTypes).?.value.toSeq.flatten
          )
        }
      }
    }.value
  )

  private def sbtLauncher[A](k: TaskKey[A]): Def.Initialize[Task[File]] = Def.taskDyn {
    val v = (k / sbtVersion).value
    Def.task {
      val Seq(launcher) = getJarFiles("org.scala-sbt" % "sbt-launch" % v).value
      launcher
    }
  }

  private def getJarFiles(module: ModuleID): Def.Initialize[Task[Seq[File]]] = Def.task {
    (LocalRootProject / dependencyResolution).value
      .retrieve(
        dependencyId = module,
        scalaModuleInfo = (LocalRootProject / scalaModuleInfo).value,
        retrieveDirectory = (LocalRootProject / csrCacheDirectory).value,
        log = (LocalRootProject / streams).value.log
      )
      .left
      .map(e => throw e.resolveException)
      .merge
      .distinct
  }
}
