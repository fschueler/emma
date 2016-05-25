package eu.stratosphere.emma.compiler.lang.linalg

import eu.stratosphere.emma.api.DataBag
import eu.stratosphere.emma.api.lara.{Matrix, Vector}
import eu.stratosphere.emma.compiler.BaseCompilerSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AlgebraicTransformationsSpec extends BaseCompilerSpec {

  import compiler._
  import universe._

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


  "Matrix Vector Multiplication" - {

    /* We can rewrite t(X) %*% y to t((t(y) %*% X)) in order to prevent the transpose of X altogether. */
    "Matrix transpose should be avoided" in {
      val act = typeCheckAndANF(reify {
        val X = AlgebraicTransformationsSpec.X
        val y = AlgebraicTransformationsSpec.y
        val r = X.transpose() %*% y

        println(r)
      })

      println("showcode:")
      println(showCode(act))
    }
  }

  object AlgebraicTransformationsSpec {
    val X = Matrix.rand[Double](10, 10)
    val y = Vector.rand[Double](10)
  }
}
