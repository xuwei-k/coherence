package coherence

abstract class OutputCommon { self: Output =>
  def asTupleOption = Output.unapply(self)
}
