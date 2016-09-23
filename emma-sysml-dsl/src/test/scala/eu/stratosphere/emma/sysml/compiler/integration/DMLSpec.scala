package eu.stratosphere.emma.sysml.compiler.integration

import eu.stratosphere.emma.compiler.BaseCompilerSpec
import eu.stratosphere.emma.compiler.lang.core.DML
import eu.stratosphere.emma.sysml.api._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/** A spec for SystemML Algorithms. */
@RunWith(classOf[JUnitRunner])
class DMLSpec extends BaseCompilerSpec {

  import compiler._
  import Core.{Lang => core}

  // ---------------------------------------------------------------------------
  // Transformation pipelines
  // ---------------------------------------------------------------------------

  val anfPipeline: u.Expr[Any] => u.Tree =
    compiler.pipeline(typeCheck = true)(
      Core.anf
    ).compose(_.tree)

  val liftPipeline: u.Expr[Any] => u.Tree =
    compiler.pipeline(typeCheck = true)(
      Core.anf
    ).compose(_.tree)

  val prettyPrint: u.Tree => String =
    tree => time(Core.prettyPrint(tree), "pretty print")

  val toDML: u.Tree => String =
    tree => time(Core.generateDML(tree), "generate dml")

  "Atomics:" - {

    "Literals" in {
      val acts = idPipeline(u.reify(
        42, 42L, 3.14, 3.14F, .1e6, 'c', "string"
      )) collect {
        case act@core.Lit(_) => toDML(act)
      }

      val exps = Seq(
        "42", "42", "3.14", "3.14", "100000.0", "\"c\"", "\"string\""
      )

      (acts zip exps) foreach { case (act, exp) =>
        act shouldEqual exp
      }
    }

    "References" in {
      val acts = idPipeline(u.reify {
        val x = 1
        val y = 2
        val * = 3
        val `p$^s` = 4
        val ⋈ = 5
        val `foo and bar` = 6
        x * y * `*` * `p$^s` * ⋈ * `foo and bar`
      }) collect {
        case act@core.Ref(_) => toDML(act)
      }

      val exps = Seq(
        "x", "y", "*", "p$^s", "⋈", "foo and bar"
      )

      (acts zip exps) foreach { case (act, exp) =>
        act shouldEqual exp
      }
    }

    "This" is pending
  }

  "Matrix constructors" in {

    val t = idPipeline(u.reify {
      val x$01 = Matrix.rand(3, 3)
      val x$02 = Matrix.zeros(3, 3)
      x$01 + x$02
    })

    val acts = (t collect {
      case u.Block(stats, _) => stats.collect {
        case vd@u.ValDef(_, _, _, _) => toDML(vd)
      }
    }).flatten

    val exps =
      """
        |x$01 = rand(rows=3, cols=3)
        |x$02 = matrix(0, rows=3, cols=3)
      """.stripMargin.trim.split('\n')

    acts.length shouldEqual exps.length

    (acts zip exps) foreach { case (act, exp) =>
      act shouldEqual exp
    }
  }

  "Matrix Multiplication" in {

    val act = toDML(idPipeline(u.reify {
      val A = Matrix.rand(5, 3)
      val B = Matrix.rand(3, 7)
      A %*% B
    }))

    val exp =
      """
        |A = rand(rows=5, cols=3)
        |B = rand(rows=3, cols=7)
        |A %*% B
      """.stripMargin.trim

    act shouldEqual exp
  }
}
