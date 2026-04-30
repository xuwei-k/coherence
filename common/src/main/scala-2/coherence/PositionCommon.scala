package coherence

abstract class PositionCommon { self: Output.Position =>
  def asTupleOption = Output.Position.unapply(self)
}
