//package eu.stratosphere.emma.examples.lara
//
//import eu.stratosphere.emma.api.TextInputFormat
//import eu.stratosphere.emma.api.lara.{Transformations, Matrix, Vector}
//import eu.stratosphere.emma.api._
//
//object KMeans {
//
//  //@formatter:off
//  val MaxItr     = 10
//  val Centers    = 3
//  val Dimensions = 3
//  //@formatter:on
//  def cluster() = {
//    val input = "inputPath"
//
//    val points = for (line <- read(input, new TextInputFormat[String]('\n'))) yield {
//      val record = line.split("\t")
//      Vector(record.map(_.toDouble))
//    }
//
//    val CP = Matrix.rand[Double](Centers, Dimensions)
//
//    // assign points to cluster centers
//    val labeled = for (p <- points) yield {
//      val distances: Vector[Double] = CP.rows(r => p dot r)
//      val cID = distances.indexedAggregate((a, b) => Math.min(a.id, b.id))
//      (cID, p)
//    }
//
//    // calc new cluster centers
//    for (p <- labeled.groupBy(t => t._1)) yield {
//      val sum = p.values.
//      val count = p.values.size
//      (p.key, sum / count)
//    }
//
//
//
//    val TS = Transformations.vecToMatrix(points)
//
//    val CP = Matrix.rand[Double](3, TS.numCols)
//
//    for (_ <- 0 until MaxItr) {
//      val c = (TS %*% CP.transpose()).map(Math.sqrt(_))
//      // get row min == nearest cluster point
//      val minC: Vector[Int] = c.rows(r => r.indexedAggregate((a, b) => Math.min(a.id, b.id)))
//      // for each cluster: sum points & count
//
//    }
//  }
//}
