val common = Def.settings(
  scalaVersion := "3.3.7"
)

val a1 = project.settings(common)

val a2 = project.settings(common).dependsOn(a1)

val expect = coherence.Output(
  Map(
    "example.A[scala.Int]" -> Seq(
      coherence.Output.Position(
        "a1/src/main/scala/A.scala",
        63,
        82,
      ),
      coherence.Output.Position(
        "a1/src/main/scala/A.scala",
        42,
        57,
      ),
      coherence.Output.Position(
        "a2/src/main/scala/C.scala",
        30,
        45,
      ),
    )
  )
)

val root = project
  .aggregate(a1, a2)
  .settings(
    scalaVersion := "2.13.18",
    InputKey[Unit]("check") := {
      val result = (LocalRootProject / coherenceCheck).value
      assert(result == expect, result)
    },
  )
