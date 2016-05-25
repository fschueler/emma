package eu.stratosphere.emma.testschema

import eu.stratosphere.emma.api.lara.{Vector, Matrix}

object VecsAndMats {

  val X = Matrix.rand[Double](10,10)
  val y = Vector.rand[Double](10)

}
