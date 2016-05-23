package eu.stratosphere.emma.compiler.lang.linalg

import eu.stratosphere.emma.api.DataBag
import eu.stratosphere.emma.api.lara.{Transformations, Matrix, Vector}
import eu.stratosphere.emma.compiler.BaseCompilerSpec
import eu.stratosphere.emma.compiler.ir._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/** A spec for the `LNF.cse` transformation. */
@RunWith(classOf[JUnitRunner])
class RowAggPushDownSpec extends BaseCompilerSpec {

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

  /** Pipeline for this spec. */
  def validate[T]: Expr[T] => Tree = {
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

  /*
  Apply optimization when aggregation is used, then two cases:
  1. Data is read and only used to get the aggregated values (matrix is never used afterwards)
  2. The matrix is used for further processing after the aggregation

  In the first case we can just apply the aggregation on the bag and do not create a matrix at all.
  In the second case we have to either
  a) append the aggregated values to the tuple of the bag.
  b) calculate the values as accumulators and then broadcast the values (this should be prefered IMO)
   */

  // We can not use the version with the fill operator, as we do not allow direct
  // access via get() to the vector (in the fill op.)
  // This seems fine for me.

  "Pushdown row operations" - {
    "single input, no aggregation" in {
      val act = typeCheckAndANF(reify {
        val a = RowAggPushDownSpec.A
        val m = eu.stratosphere.emma.api.lara.Transformations.toMatrix[Double](a)

        val add: Matrix[Double] = m.rows((row: Vector[Double]) => row.transpose())

        println(add)
      })

      showCode(act)
    }

    "single input, no reuse, complex aggregate" in {
      val act = typeCheckAndANF(reify {
        val a = RowAggPushDownSpec.A
        val m = Transformations.toMatrix[Int](a)
        val agg: Vector[Int] = m.rows((row: Vector[Int]) => row.aggregate(_ + _))
        println(agg)
      })

      val exp = validate(reify {
        val a = RowAggPushDownSpec.A
        val agg = {
          val map = a.map[Int]{ p =>
            p.productIterator.map(_.asInstanceOf[Int]).reduce(_ * _)
          }
          Transformations.toVector(map)
        }
        println(agg)
      })

//      act shouldBe alphaEqTo(exp)

      showCode(act)
    }

    "single input, no reuse" in {
      val act = typeCheckAndANF(reify {
        val a = RowAggPushDownSpec.A
        val m = Transformations.toMatrix[Int](a)
        val agg: Vector[Double] = m.rows((row: Vector[Int]) => {
          val x = 3
          val u = row.aggregate(_ + _ - x)
          val res = u * 10
          res * 3.0
        })
        println(agg)
      })

      showCode(act)
    }

    "single input, reuse (self)" in {
      val act = typeCheckAndANF(reify {
        val a = RowAggPushDownSpec.A
        val m = eu.stratosphere.emma.api.lara.Transformations.toMatrix[Double](a)

        val agg: Vector[Double] = m.rows((row: Vector[Double]) => row.aggregate(_ + _))
        val out = m - agg

        println(out)
      })

      showCode(act)
    }

    "single input, reuse (self)(fill)" in {
      val act = typeCheckAndANF(reify {
//        val a = RowAggPushDownSpec.A
//        val m = eu.stratosphere.emma.api.lara.Transformations.toMatrix[Double](a)
//
//        val agg: Vector[Double] = m.rows((row: Vector[Double]) => row.aggregate(_ + _))
//        val out = m - Matrix.fill(m.numRows, m.numCols)((i, j) => agg.get(j))
//
//        println(out)
      })

      showCode(act)
    }

    "single input, reuse" in {
      val act = typeCheckAndANF(reify {
        val a = RowAggPushDownSpec.A
        val m = eu.stratosphere.emma.api.lara.Transformations.toMatrix[Double](a)
        val b = RowAggPushDownSpec.B
        val n = eu.stratosphere.emma.api.lara.Transformations.toMatrix[Double](b)

        val agg: Vector[Double] = m.rows((row: Vector[Double]) => row.aggregate(_ + _))
        val out = m %*% n
        val minus = out - agg
        // or
//        val minus = n - agg
//        val out = m %*% minus

        println(minus)
      })

      showCode(act)
    }

    "join, no reuse"  in {
      val act = typeCheckAndANF(reify {
        val join = comprehension[(Int, Int, Int, Int), DataBag] {
          val a = generator(RowAggPushDownSpec.A)
          val b = generator(RowAggPushDownSpec.B)
          guard(a._1 == b._1)
          head(a._2, a._3, b._2, b._3)
        }

        val m = eu.stratosphere.emma.api.lara.Transformations.toMatrix[Double](join)
        val agg: Vector[Double] = m.rows((row: Vector[Double]) => row.aggregate(_ + _))

        println(agg)
      })

      showCode(act)
    }

    "join, reuse (self)" in {
      val act = typeCheckAndANF(reify {
        val join = comprehension[(Int, Int, Int, Int), DataBag] {
          val a = generator(RowAggPushDownSpec.A)
          val b = generator(RowAggPushDownSpec.B)
          guard(a._1 == b._1)
          head(a._2, a._3, b._2, b._3)
        }

        val m = eu.stratosphere.emma.api.lara.Transformations.toMatrix[Double](join)
        val agg: Vector[Double] = m.rows((row: Vector[Double]) => row.aggregate(_ + _))
        val out = m - agg

        println(out)
      })

      showCode(act)
    }

    "join, reuse (self)(fill)" in {
      val act = typeCheckAndANF(reify {
//        val join = comprehension[(Int, Int, Int, Int), DataBag] {
//          val a = generator(RowAggPushDownSpec.A)
//          val b = generator(RowAggPushDownSpec.B)
//          guard(a._1 == b._1)
//          head(a._2, a._3, b._2, b._3)
//        }
//
//        val m = eu.stratosphere.emma.api.lara.Transformations.toMatrix[Double](join)
//        val agg: Vector[Double] = m.rows((row: Vector[Double]) => row.aggregate(_ + _))
//        val out = m - Matrix.fill(m.numRows, m.numCols)((i, j) => agg.get(j))
//
//        println(out)
      })

      showCode(act)
    }

    "join, reuse (self on join operand)" in {
      val act = typeCheckAndANF(reify {
        val a = RowAggPushDownSpec.A
        val m = eu.stratosphere.emma.api.lara.Transformations.toMatrix[Double](a)

        val join = comprehension[(Int, Int, Int, Int), DataBag] {
          val a = generator(RowAggPushDownSpec.A)
          val b = generator(RowAggPushDownSpec.B)
          guard(a._1 == b._1)
          head(a._2, a._3, b._2, b._3)
        }

        val n = eu.stratosphere.emma.api.lara.Transformations.toMatrix[Double](join)
        val agg: Vector[Double] = m.rows((row: Vector[Double]) => row.aggregate(_ + _))
        val out = m %*% n
        val minus = out - agg
        // or
        //        val minus = n - agg
        //        val out = m %*% minus

        println(minus)
      })

      showCode(act)
    }

    "join, reuse" in {
      val act = typeCheckAndANF(reify {
        val a = RowAggPushDownSpec.C
        val m = eu.stratosphere.emma.api.lara.Transformations.toMatrix[Double](a)

        val join = comprehension[(Int, Int, Int, Int), DataBag] {
          val a = generator(RowAggPushDownSpec.A)
          val b = generator(RowAggPushDownSpec.B)
          guard(a._1 == b._1)
          head(a._2, a._3, b._2, b._3)
        }

        val n = eu.stratosphere.emma.api.lara.Transformations.toMatrix[Double](join)
        val agg: Vector[Double] = m.rows((row: Vector[Double]) => row.aggregate(_ + _))
        val out = m %*% n
        val minus = out - agg
        // or
        //        val minus = n - agg
        //        val out = m %*% minus

        println(minus)
      })

      showCode(act)
    }
  }

  "Pushdown cols operations" - {

  }
}

object RowAggPushDownSpec {
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
