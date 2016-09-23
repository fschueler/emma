package eu.stratosphere.emma.sysml.macros

import org.junit.runner.RunWith
import org.scalatest.{FreeSpec, Matchers}
import org.scalatest.junit.JUnitRunner

import eu.stratosphere.emma.sysml.api._

/** A spec for SystemML Algorithms. */
@RunWith(classOf[JUnitRunner])
class RewriteMacrosSpec extends FreeSpec with Matchers {


  "Produce DML script" in {
    val dml = parallelize { val x = Matrix.zeros(3, 3) }
    dml
  }

}
