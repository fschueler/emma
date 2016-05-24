package eu.stratosphere.emma.compiler.lang.linalg

import eu.stratosphere.emma.api.DataBag
import eu.stratosphere.emma.api.lara.{Matrix, Vector}
import eu.stratosphere.emma.compiler.BaseCompilerSpec
import eu.stratosphere.emma.compiler.ir._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PropertiesSpec extends BaseCompilerSpec {

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
    LinAlg.buildChains
  } andThen {
    Owner.at(Owner.enclosing)
  }

  "Properties" - {
    "simple" in {
      val act = typeCheckAndANF(reify {
        val a = PropertiesSpec.A
        val m = eu.stratosphere.emma.api.lara.Transformations.toMatrix[Double](a)

        val add: Matrix[Double] = m.rows((row: Vector[Double]) => row.transpose())

        println(add)
      })

      showCode(act)
    }

  }
}

object PropertiesSpec {
  val A = DataBag(Seq(
    (0,1,1),
    (1,2,2),
    (2,3,3),
    (3,4,4)
  ))

  val B = DataBag(Seq(
    (0,5,5),
    (1,6,6),
    (2,7,7),
    (3,8,8)
  ))

  val C = DataBag(Seq(
    (0,9,9),
    (1,10,10),
    (2,11,11),
    (3,12,12)
  ))
}
