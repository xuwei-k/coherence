package coherence

import sjsonnew.JsonReader

trait FromJson[A] {
  final def fromJson(string: String)(implicit instance: JsonReader[A]): A = {
    val json = sjsonnew.support.scalajson.unsafe.Parser.parseUnsafe(string)
    val unbuilder = new sjsonnew.Unbuilder(sjsonnew.support.scalajson.unsafe.Converter.facade)
    instance.read(Some(json), unbuilder)
  }
}
