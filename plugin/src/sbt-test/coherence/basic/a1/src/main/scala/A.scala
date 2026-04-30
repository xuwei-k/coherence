package example

trait A[X]

object B {
  given A[Int] = ???
  given a1: A[Int] = ???
}
