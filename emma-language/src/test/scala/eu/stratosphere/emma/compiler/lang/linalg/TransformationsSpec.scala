package eu.stratosphere.emma.compiler.lang.linalg

import eu.stratosphere.emma.api.DataBag
import eu.stratosphere.emma.api.lara.{Matrix, Vector}
import eu.stratosphere.emma.compiler.BaseCompilerSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TransformationsSpec extends BaseCompilerSpec {

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
    PushDown.rowAggregates
  } andThen {
    Owner.at(Owner.enclosing)
  }

  "Transformations" - {
    "toMatrix" in {
      val act = typeCheckAndANF(reify {
        val a = TransformationsSpec.A
        val m = eu.stratosphere.emma.api.lara.Transformations.toMatrix[Double](a)
        val agg: Vector[Double] = m.rows((row: Vector[Double]) => row.aggregate(_ + _))
        println(agg)
      })

      showCode(act)
    }

    "toIndexed" in {
      val act = typeCheckAndANF(reify {
        val a = TransformationsSpec.D
        val m = eu.stratosphere.emma.api.lara.Transformations.indexedToMatrix(a)
        val agg: Vector[Int] = m.rows((row: Vector[Int]) => row.aggregate(_ + _))
        println(agg)
      })

      showCode(act)
    }

    "toIndexed, explicit size" in {
      val act = typeCheckAndANF(reify {
        val a = TransformationsSpec.D
        val m = eu.stratosphere.emma.api.lara.Transformations.indexedToMatrix(2, 2)(a)
        val agg: Vector[Int] = m.rows((row: Vector[Int]) => row.aggregate(_ + _))
        println(agg)
      })

      showCode(act)
    }
  }

  "Synthetic" - {
    "fill" in {
      val act = typeCheckAndANF(reify {
        val m = Matrix.fill(2,2)((i, j) => 1)
        val agg: Vector[Int] = m.rows((row: Vector[Int]) => row.aggregate(_ + _))
        println(agg)
      })

      showCode(act)
    }
  }
}

object TransformationsSpec {
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

  val D = DataBag(Seq(
    (0,0,1),
    (0,1,2),
    (1,0,3),
    (1,1,4)
  ))
}
