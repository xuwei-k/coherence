package coherence

import sjsonnew.BasicJsonProtocol._
import sjsonnew.Builder
import sjsonnew.JsonFormat
import sjsonnew.Unbuilder

final case class Output(
  values: Map[String, Seq[Output.Position]],
) extends OutputCommon
    with ToJson[Output] {
  override def toString: String = toJsonString
}

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

  private def bimap[A, B](instance: JsonFormat[A], f1: A => B, f2: B => A): JsonFormat[B] =
    new JsonFormat[B] {
      override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): B =
        f1(instance.read(jsOpt, unbuilder))
      override def write[J](obj: B, builder: Builder[J]): Unit =
        instance.write(f2(obj), builder)
    }

  implicit val instance: JsonFormat[Output] =
    bimap(
      implicitly[JsonFormat[Map[String, Seq[Output.Position]]]],
      apply,
      _.values
    )

}
