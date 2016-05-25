package eu.stratosphere.emma.compiler.lang.linalg

import eu.stratosphere.emma.api.DataBag
import eu.stratosphere.emma.compiler.BaseCompilerSpec
import eu.stratosphere.emma.compiler.ir._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/** A spec for the `LNF.cse` transformation. */
@RunWith(classOf[JUnitRunner])
class MatrixSpec extends BaseCompilerSpec {

  import compiler._
  import universe._

  def typeCheckAndNormalize[T]: Expr[T] => Tree = {
    (_: Expr[T]).tree
  } andThen {
    Type.check(_)
  } andThen {
    Core.destructPatternMatches
  } andThen {
    Core.resolveNameClashes
  } andThen {
    Core.anf
  } andThen {
    time(Core.cse(_), "cse")
  } andThen {
    Owner.at(Owner.enclosing)
  }

  "Matrix" - {
    "fill" in {
      val act = typeCheckAndNormalize(reify {
        val m = eu.stratosphere.emma.api.lara.Matrix(2,3, Array(1,2,3,4,5,6))
        scala.Predef.println(m)
      })

//      val exp = typeCheck(reify {
//        val x$1 = Seq(1, 2, 3)
//        val bag = eu.stratosphere.emma.api.DataBag(x$1)
//        val x$2 = bag.fetch()
//        val x$3 = scala.Predef.println(x$2)
//        x$3
//      })

      showCode(act)
//      act shouldEqual exp
    }

    "transform" in {
      val act = typeCheckAndNormalize(reify {
        val join = for {
          a <- MatrixSpec.A
          b <- MatrixSpec.B
          if a._1 == b._1
        } yield (a._2, a._3, b._2, b._3)

        val m = eu.stratosphere.emma.api.lara.Transformations.toMatrix[Int](join)
        println(m)
      })

      showCode(act)
    }

    "transform ir" in {
      val act = typeCheckAndNormalize(reify {
        val join = comprehension[(Int, Int, Int, Int), DataBag] {
          val a = generator(MatrixSpec.A)
          val b = generator(MatrixSpec.B)
          guard(a._1 == b._1)
          head(a._2, a._3, b._2, b._3)
        }

        val m = eu.stratosphere.emma.api.lara.Transformations.toMatrix[Double](join)
        println(m)
      })

      showCode(act)
    }
  }
}

object MatrixSpec {
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
}
