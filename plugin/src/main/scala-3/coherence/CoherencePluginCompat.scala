package coherence

import sbt.Keys.Classpath
import sbt.Keys.fileConverter

private[coherence] trait CoherencePluginCompat { self: CoherencePlugin.type =>
  inline def convertClasspath(classpath: Classpath): Seq[String] =
    classpath.map(_.data).map(fileConverter.value.toPath).map(_.toFile.getAbsolutePath)
}
