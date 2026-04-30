package coherence

import sbt.Def
import sbt.Keys.Classpath

private[coherence] trait CoherencePluginCompat { self: CoherencePlugin.type =>
  implicit class DefOps(self: Def.type) {
    def uncached[A](value: A): A = value
  }

  def convertClasspath(classpath: Classpath): Seq[String] =
    classpath.map(_.data.getAbsolutePath)
}
