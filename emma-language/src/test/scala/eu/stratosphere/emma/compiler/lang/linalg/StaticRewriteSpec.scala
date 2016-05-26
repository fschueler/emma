package eu.stratosphere.emma.compiler.lang.linalg

import eu.stratosphere.emma.compiler.BaseCompilerSpec
import eu.stratosphere.emma.testschema.VecsAndMats
import eu.stratosphere.emma.testschema.VecsAndMats._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/** A spec for static rewrites of linear algebra expressions. */
@RunWith(classOf[JUnitRunner])
class StaticRewriteSpec extends BaseCompilerSpec {

  import compiler._
  import universe._

  // ---------------------------------------------------------------------------
  // Helper methods
  // ---------------------------------------------------------------------------

  /** Pipeline for this spec. */
  def typeCheckAndANF[T]: Expr[T] => Tree = {
    (_: Expr[T]).tree
  } andThen {
    Type.check(_: Tree)
  } andThen {
    Core.destructPatternMatches
  } andThen {
    Core.resolveNameClashes
  } andThen {
    Core.anf
  } andThen {
    Core.simplify
  } andThen {
    Comprehension.resugar(API.bagSymbol)
  } andThen {
    Owner.at(Owner.enclosing)
  }

  def rewrite: Tree => Tree = {
    LinAlg.rewrite(LA.matrixSymbol)(_)
  } andThen {
    Core.dce
  } andThen {
    Core.simplify
  }

  val (inp1, exp1) = {

    val inp = (typeCheckAndANF) (reify {
      val matrix$1 = X
      val vector$1 = VecsAndMats.y
      val transpose$1 = matrix$1.transpose()
      val matmult$1 = transpose$1 %*% vector$1
      matmult$1
    })

    val exp = (typeCheckAndANF) (reify {
      val matrix$1 = X
      val vector$1 = VecsAndMats.y
      val transpose$1 = vector$1.transpose()
      val matmult$1 = transpose$1 %*% matrix$1
      val transpose$2 = matmult$1.transpose()
      transpose$2
    })

    (inp, exp)
  }

  // ---------------------------------------------------------------------------
  // Spec tests
  // ---------------------------------------------------------------------------

  "static rewrite" - {
    "of M.t %*% v" in {
      rewrite(inp1) shouldBe alphaEqTo(exp1)
      rewrite(exp1) shouldBe alphaEqTo(exp1) // this block should not be rewritten
    }
  }
}
