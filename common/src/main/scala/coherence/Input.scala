package coherence

import sjsonnew.BasicJsonProtocol._
import sjsonnew.JsonFormat

final case class Input(
  tastyDirectories: Seq[String],
  classpath: Seq[String],
  excludeTypes: Seq[String],
  console: Boolean,
) extends InputCommon
    with ToJson[Input] {
  override def toString: String = toJsonString
}

object Input extends FromJson[Input] {
  implicit val instance: JsonFormat[Input] =
    caseClass4(apply, (_: Input).asTupleOption)("directories", "classpath", "exclude", "console")
}
