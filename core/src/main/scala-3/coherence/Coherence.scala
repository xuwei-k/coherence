package coherence

import dotty.tools.dotc.reporting.ConsoleReporter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import scala.annotation.tailrec
import scala.quoted.Quotes
import scala.reflect.TypeTest
import scala.tasty.inspector.Inspector
import scala.tasty.inspector.Tasty
import scala.tasty.inspector.TastyInspector

object Coherence {
  private def inspector(input: Input): Inspector = new Inspector {
    override def inspect(using q: Quotes)(tastys: List[Tasty[q.type]]): Unit = {
      import q.reflect.*
      val buffer = new ByteArrayOutputStream()
      val myWriter = new PrintWriter(buffer)
      val context =
        q.asInstanceOf[scala.quoted.runtime.impl.QuotesImpl]
          .ctx
          .fresh
          .setReporter(
            new ConsoleReporter(
              writer = myWriter,
              echoer = myWriter,
            )
          )
      case class Result(typeString: String, pos: Position)
      val accumulator = new TreeAccumulator[List[Result]] {
        @tailrec
        override def foldTree(result: List[Result], tree: Tree)(owner: Symbol): List[Result] = {
          // TODO DefDef
          tree match {
            case x: ValDef
                if !x.symbol.flags
                  .is(Flags.Param) && (x.symbol.flags.is(Flags.Given) || x.symbol.flags.is(Flags.Implicit)) && summon[
                  TypeTest[Tree, Applied]
                ].unapply(x.tpt).isDefined =>
              val typeStr = x.tpt.tpe.dealias.show
              x.rhs match {
                case Some(body) =>
                  val p = if (x.pos.start < body.pos.start) {
                    Position(x.pos.sourceFile, x.pos.start, body.pos.start)
                  } else {
                    x.pos
                  }
                  val r = Result(typeStr, p) :: result
                  foldTree(r, body)(owner)
                case None =>
                  Result(typeStr, x.pos) :: result
              }
            case _ =>
              foldOverTree(result, tree)(owner)
          }
        }

        override def foldOverTree(result: List[Result], tree: Tree)(owner: Symbol): List[Result] = {
          tree match {
            case dotty.tools.dotc.ast.tpd.EmptyTree =>
              result
            case _ =>
              super.foldOverTree(result, tree)(owner)
          }
        }
      }
      val result: List[Result] = tastys.flatMap { t =>
        accumulator.foldOverTree(Nil, t.ast)(Symbol.spliceOwner)
      }
      val duplicate = result.groupBy(_.typeString).filter(_._2.sizeIs > 1).toList.sortBy(_._2.size)
      duplicate.foreach { (k, v) =>
        v.foreach { x =>
          val message = s"Duplicate ${k} instance"
          dotty.tools.dotc.report.error(message, x.pos.asInstanceOf)(using context)
          if (input.console) {
            report.error(message, x.pos)
          }
        }
      }
      myWriter.flush()
      val str = new String(buffer.toByteArray, StandardCharsets.UTF_8)
      val out = Output(
        duplicate.toMap.view
          .mapValues(
            _.map { x =>
              Output.Position(
                path = x.pos.sourceFile.path,
                start = x.pos.start,
                end = x.pos.end,
              )
            }
          )
          .toMap,
        if (input.color) {
          str
        } else {
          str.replaceAll("\u001B\\[[;\\d]*m", "")
        }
      )
      Files.writeString(
        new File("output.json").toPath,
        out.toJsonString
      )
    }
  }

  def main(args: Array[String]): Unit = {
    val input = Input.fromJson(Files.readString(new File("input.json").toPath))
    import scala.jdk.StreamConverters.*
    val tastyFiles = input.tastyDirectories
      .filter(dir => new File(dir).exists())
      .flatMap(dir =>
        Files
          .walk(new File(dir).toPath)
          .filter(_.toFile.isFile)
          .filter(_.toFile.getName.endsWith(".tasty"))
          .toScala(List)
      )
      .map(_.toFile.getAbsolutePath)
      .toList

    TastyInspector.inspectAllTastyFiles(
      tastyFiles = tastyFiles,
      jars = Nil,
      dependenciesClasspath = input.classpath.toList
    )(inspector(input))
  }
}
