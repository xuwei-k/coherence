package coherence

abstract class PositionCommon { self: Output.Position =>
  def asTupleOption = Option(Tuple.fromProductTyped(self))
}
