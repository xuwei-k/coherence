package coherence

import sjsonnew.Builder
import sjsonnew.JsonWriter
import sjsonnew.support.scalajson.unsafe.PrettyPrinter

trait ToJson[A] { self: A =>
  def toJsonString(implicit instance: JsonWriter[A]): String =
    ToJson.toJsonString[A](self)
}

object ToJson {
  def toJsonString[A](a: A)(implicit instance: JsonWriter[A]): String = {
    val builder = new Builder(sjsonnew.support.scalajson.unsafe.Converter.facade)
    instance.write(a, builder)
    PrettyPrinter.apply(
      builder.result.getOrElse(sys.error("invalid json"))
    )
  }
}
