package coherence

abstract class InputCommon { self: Input =>
  def asTupleOption = Input.unapply(self)
}
