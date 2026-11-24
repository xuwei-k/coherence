package coherence

import sbt.*
import sbt.Keys.*

private[coherence] trait CoherencePluginCompat { self: CoherencePlugin.type =>
  inline def convertClasspath(classpath: Classpath): Seq[String] =
    classpath.map(_.data).map(fileConverter.value.toPath).map(_.toFile.getAbsolutePath)

  def coherenceInputTask: Def.Initialize[Task[Input]] = Def.taskDyn {
    val projects: List[String] = coherenceProjects.value
    val directories = projects.map(getClassDirectories)
    val allClasspath = getAllClasspath(projects).join

    Def.taskDyn {
      val x = allClasspath.value
      Def.task {
        Input(
          tastyDirectories = directories.join.value.flatten.map(_.getAbsolutePath),
          classpath = x.flatten.sorted.distinct,
          excludeTypes = (LocalRootProject / autoImport.coherenceExcludeTypes).?.value.toSeq.flatten,
          console = true,
          color = true,
        )
      }
    }
  }
}
