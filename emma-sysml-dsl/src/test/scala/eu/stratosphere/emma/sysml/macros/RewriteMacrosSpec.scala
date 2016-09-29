package eu.stratosphere.emma.sysml.macros

import eu.stratosphere.emma.api.SystemMLAlgorithm
import org.junit.runner.RunWith
import org.scalatest.{FreeSpec, Matchers}
import org.scalatest.junit.JUnitRunner
import eu.stratosphere.emma.sysml.api._

/** A spec for SystemML Algorithms. */
@RunWith(classOf[JUnitRunner])
class RewriteMacrosSpec extends FreeSpec with Matchers {


  "Matrix Multiplication" in {
    val alg = parallelize {
      val A = Matrix.rand(5, 3)
      val B = Matrix.rand(3, 7)
      val C = A %*% B
      write(C, "/home/felix/matrix.csv", Format.CSV)
    }

    val a = alg
    val m = a.run()
    m
  }
}
