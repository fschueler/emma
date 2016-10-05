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

package eu.stratosphere.emma.sysml.macros

import eu.stratosphere.emma.api.SystemMLAlgorithm
import org.junit.runner.RunWith
import org.scalatest.{FreeSpec, Matchers}
import org.scalatest.junit.JUnitRunner
import eu.stratosphere.emma.sysml.api._
import org.apache.spark.SparkContext
import org.apache.spark.sql._
import org.apache.spark.sql.types.{DoubleType, StructField, StructType}

import scala.util.Random

/** A spec for SystemML Algorithms. */
@RunWith(classOf[JUnitRunner])
class RewriteMacrosSpec extends FreeSpec with Matchers {


//  "Matrix Multiplication" in {
//
//    val alg = parallelize {
//      val A = Matrix.rand(5, 3)
//      val B = Matrix.rand(3, 7)
//      val C = A %*% B
//      C
//    }
//
//    val res = alg.run()
//  }

//  "MinMaxMean from DataFrame" in {
//    val numRows = 10000
//    val numCols = 1000
//    val data = sc.parallelize(0 to numRows-1).map { _ => Row.fromSeq(Seq.fill(numCols)(Random.nextDouble)) }
//    val schema = StructType((0 to numCols-1).map { i => StructField("C" + i, DoubleType, true) } )
//    val df = sqlContext.createDataFrame(data, schema)
//
//    val alg = parallelize {
//      /* this should take a dataframeand set it as input to the MLContext */
//      val matrix: Matrix = Matrix.fromDataFrame(df) // can we find out the metadata?
//
//      val minOut = min(matrix)
//      val maxOut = max(matrix)
//      val meanOut = mean(matrix)
//
//      (minOut, maxOut, meanOut)
//    }
//
//    val  (minOut: Double, maxOut: Double, meanOut: Double) = alg.run()
//
//    println(s"The minimum is $minOut, maximum: $maxOut, mean: $meanOut")
//
//  }

//  "Matrix output" in {
//
//    val alg = parallelize {
//      val m = Matrix(Seq(11.0, 22.0, 33.0, 44.0), 2, 2)
//      val n = sum(m)
//      (m, n)
//    }
//
//    val (m: Matrix, n: Double) = alg.run()
//  }
}
