package coherence

import sbt.*
import sbt.Keys.*

private[coherence] trait CoherencePluginCompat { self: CoherencePlugin.type =>
  implicit class DefOps(self: Def.type) {
    def uncached[A](value: A): A = value
  }

  def convertClasspath(classpath: Classpath): Seq[String] =
    classpath.map(_.data.getAbsolutePath)

  def coherenceInputTask: Def.Initialize[Task[Input]] = Def.taskDyn {
    val projects: List[String] = coherenceProjects.value
    val directories = projects.map(getClassDirectories)
    val allClasspath = getAllClasspath(projects).join

    Def.taskDyn {
      val x = allClasspath.value
      Def.task {
        Input(
          tastyDirectories = directories.join.value.flatten.map(_.getAbsolutePath),
          classpath = x.join.value.flatten.sorted.distinct,
          excludeTypes = (LocalRootProject / autoImport.coherenceExcludeTypes).?.value.toSeq.flatten,
          console = true,
          color = true,
        )
      }
    }
  }
}
