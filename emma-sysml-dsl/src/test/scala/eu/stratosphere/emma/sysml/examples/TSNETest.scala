package eu.stratosphere.emma.sysml.examples

import eu.stratosphere.emma.sysml.api.Matrix
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FreeSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class TSNETest extends FreeSpec with Matchers {

  "TSNE should run" in {
    val X           = Matrix.rand(100, 100)
    val reducedDims = 2
    val perplexity  = 30
    val lr          = 300.0
    val momentum    = 0.9
    val iter        = 1000

    TSNE.tsne(X, reducedDims, perplexity, lr, momentum, iter)
  }
}
