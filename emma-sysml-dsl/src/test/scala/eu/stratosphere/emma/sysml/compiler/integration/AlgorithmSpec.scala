package eu.stratosphere.emma.sysml.compiler.integration

import eu.stratosphere.emma.compiler.BaseCompilerSpec
import eu.stratosphere.emma.sysml.api._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/** A spec for SystemML Algorithms. */
@RunWith(classOf[JUnitRunner])
class AlgorithmSpec extends BaseCompilerSpec {
  import compiler._

  // ---------------------------------------------------------------------------
  // Transformation pipelines
  // ---------------------------------------------------------------------------

  val anfPipeline: u.Expr[Any] => u.Tree =
    compiler.pipeline(typeCheck = true)(
      Core.anf
    ).compose(_.tree)

  val liftPipeline: u.Expr[Any] => u.Tree =
    compiler.pipeline(typeCheck = true)(
      Core.lift
    ).compose(_.tree)

  val prettyPrint: u.Tree => String =
    tree => time(Core.prettyPrint(tree), "pretty print")

  // ---------------------------------------------------------------------------
  // Program closure
  // ---------------------------------------------------------------------------

  val expr = anfPipeline(u.reify {
    val X = Matrix.rand(100, 2)
    var w = Vector.rand(2)

    var s = 0.0

    while (s > 1.0) {
      w = w.t %*% X
      s = sum(w)
    }

    w
  })

  // ---------------------------------------------------------------------------
  // Specs
  // ---------------------------------------------------------------------------

  "lifting" in {
    expr
    1.0
  }
}
