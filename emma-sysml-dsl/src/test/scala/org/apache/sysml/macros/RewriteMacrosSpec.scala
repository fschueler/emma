/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sysml.macros

import org.apache.spark.sql._
import org.apache.spark.sql.types.{DoubleType, StructField, StructType}
import org.apache.sysml.api.linalg.Matrix
import org.apache.sysml.api.linalg.api._
import org.apache.sysml.api.mlcontext.{Matrix => MLMatrix}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FreeSpec, Matchers}

import scala.util.Random

/** A spec for SystemML Algorithms. */
@RunWith(classOf[JUnitRunner])
class RewriteMacrosSpec extends FreeSpec with Matchers {

  "Matrix output" in {

    val alg = parallelize {
      val m = Matrix(Seq(11.0, 22.0, 33.0, 44.0), 2, 2)
      val n = sum(m)
      (m, n)
    }

    val (m: Matrix, n: Double) = alg.run()
  }

  "Matrix Multiplication" in {

    val alg = parallelize {
      val A = Matrix.rand(5, 3)
      val B = Matrix.rand(3, 7)
      val C = A %*% B
      C
    }

    val res = alg.run()
  }

  "For loop" in {

    val loop = parallelize {
      val A = Matrix.rand(3, 3)
      val B = Matrix.rand(3, 3)
      var C = Matrix.zeros(3, 3)

      val iter = 10
      var s = 0.0

      for (i <- 1 to iter) {
        C = A %*% B
        s = sum(C)
        println(s)
      }
    }

    val res = loop.run()
  }

  "NMF" in {

    val nmf: SystemMLAlgorithm[(Matrix, Matrix)] = parallelize {
      val tfidf = Array(1.0, 2.0, 3.0, 4.0) // tfidf feature matrix coming from somewhere
      val k = 40
      val m, n = 2 // dimensions of tfidf
      val maxIters = 200

      val V = Matrix(tfidf, m, n) // initialize matrices
      var W = Matrix.rand(m, k)
      var H = Matrix.rand(k, n)

      for (i <- 0 to maxIters) { //main loop
        H = H * (W.t %*% V) / (W.t %*% (W %*% H))
        W = W * (V %*% H.t) / (W %*% (H %*% H.t))
      }

      (W, H) // return values
    }

    val (w, h) = nmf.run()
  }


  "MinMaxMean from DataFrame" in {
    val numRows = 10000
    val numCols = 1000
    val data = sc.parallelize(0 to numRows-1).map { _ => Row.fromSeq(Seq.fill(numCols)(Random.nextDouble)) }
    val schema = StructType((0 to numCols-1).map { i => StructField("C" + i, DoubleType, true) } )
    val df = sqlContext.createDataFrame(data, schema)

    val alg = parallelize {
      /* this should take a dataframeand set it as input to the MLContext */
      val matrix: Matrix = Matrix.fromDataFrame(df) // can we find out the metadata?

      val minOut = min(matrix)
      val maxOut = max(matrix)
      val meanOut = mean(matrix)

      (minOut, maxOut, meanOut)
    }

    val  (minOut: Double, maxOut: Double, meanOut: Double) = alg.run()

    println(s"The minimum is $minOut, maximum: $maxOut, mean: $meanOut")

  }
}
