package coherence

abstract class InputCommon { self: Input =>
  def asTupleOption = Option(Tuple.fromProductTyped(self))
}
