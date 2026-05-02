package coherence

import sjsonnew.BasicJsonProtocol._
import sjsonnew.JsonFormat

final case class Output(
  values: Map[String, Seq[Output.Position]],
  console: String,
) extends OutputCommon
    with ToJson[Output]

object Output extends FromJson[Output] {
  final case class Position(
    path: String,
    start: Int,
    end: Int
  ) extends PositionCommon

  object Position {
    implicit val instance: JsonFormat[Position] =
      caseClass3(apply, (_: Position).asTupleOption)("path", "start", "end")
  }

  implicit val instance: JsonFormat[Output] =
    caseClass2(apply, (_: Output).asTupleOption)("values", "console")
}
