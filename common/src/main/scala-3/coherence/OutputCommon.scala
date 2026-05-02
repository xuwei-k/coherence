package coherence

abstract class OutputCommon { self: Output =>
  def asTupleOption = Option(Tuple.fromProductTyped(self))
}
