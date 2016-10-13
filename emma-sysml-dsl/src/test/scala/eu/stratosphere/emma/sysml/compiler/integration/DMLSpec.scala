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

package eu.stratosphere.emma.sysml.compiler.integration

import org.emmalanguage.compiler.BaseCompilerSpec

import eu.stratosphere.emma.sysml.api._
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{DoubleType, StructField, StructType}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import resource._

import scala.util.Random

/** A spec for SystemML Algorithms. */
@RunWith(classOf[JUnitRunner])
class DMLSpec extends BaseCompilerSpec {

  import compiler._
  import Core.{Lang => core}

  // ---------------------------------------------------------------------------
  // Transformation pipelines
  // ---------------------------------------------------------------------------
  override val idPipeline: u.Expr[Any] => u.Tree = {
    (_: u.Expr[Any]).tree
  } andThen {
    compiler.dmlPipeline(typeCheck = true)
  } andThen {
    checkCompile
  }

  val toDML: u.Tree => String =
    tree => time(Core.generateDML(tree), "generate dml")

  "Atomics:" - {

    "Literals" in {
      val acts = idPipeline(u.reify(
        42, 42L, 3.14, 3.14F, .1e6, 'c', "string", ()
      )) collect {
        case act@core.Lit(_) => toDML(act)
      }

      val exps = Seq(
        "42", "42", "3.14", "3.14", "100000.0", "\"c\"", "\"string\""
      )

      (acts zip exps) foreach { case (act, exp) =>
        act shouldEqual exp
      }
    }

    "References" in {
      val acts = idPipeline(u.reify {
        val x = 1
        val y = 2
        val * = 3
        val `p$^s` = 4
        val ⋈ = 5
        val `foo and bar` = 6
        x * y * `*` * `p$^s` * ⋈ * `foo and bar`
      }) collect {
        case act@core.Ref(_) => toDML(act)
      }

      val exps = Seq(
        "x", "y", "*", "p$^s", "⋈", "foo and bar"
      )

      (acts zip exps) foreach { case (act, exp) =>
        act shouldEqual exp
      }
    }

    "This" is pending
  }

  "Matrix construction" - {

    "from rand" in {
      val act = toDML(idPipeline(u.reify {
        val x$01 = Matrix.rand(3, 3)
      }))

      val exp =
        """
          |x$01 = rand(rows=3, cols=3)
        """.stripMargin.trim

      act shouldEqual exp
    }

    "from zeros" in {
      val act = toDML(idPipeline(u.reify {
        val x$01 = Matrix.zeros(3, 3)
      }))

      val exp =
        """
          |x$01 = matrix(0, rows=3, cols=3)
        """.stripMargin.trim

      act shouldEqual exp
    }

    "from sequence" in {
      val act = toDML(idPipeline(u.reify {
        val x$01 = Matrix(Seq(1.0, 2.0, 3.0, 4.0), 2, 2)
      }))

      val exp =
        """
          |x$01 = matrix("1.0 2.0 3.0 4.0", rows=2, cols=2)
        """.stripMargin.trim

      act shouldEqual exp
    }

    "from DataFrame" in {
      val numRows = 10000
      val numCols = 1000
      val data = sc.parallelize(0 to numRows-1).map { _ => Row.fromSeq(Seq.fill(numCols)(Random.nextDouble)) }
      val schema = StructType((0 to numCols-1).map { i => StructField("C" + i, DoubleType, true) } )
      val df = sqlContext.createDataFrame(data, schema)

      val act = toDML(idPipeline(u.reify {
        val A = Matrix.fromDataFrame(df)
      }))

      val exp = "" // the transformation code should be removed and the dataframe passed as input in MLContext

      act shouldEqual exp
    }
  }

  "Definitions" - {

    "Values" in {
      val act = toDML(idPipeline(u.reify {
        val a = 5
      }))

      val exp =
        """
          |a = 5
        """.stripMargin.trim

      act shouldEqual exp
    }

    "Variables" in {
      val act = toDML(idPipeline(u.reify {
        var a = 5
      }))

      val exp =
        """
          |a = 5
        """.stripMargin.trim

      act shouldEqual exp
    }
  }

  "Matrix Multiplication" in {

    val act = toDML(idPipeline(u.reify {
      val A = Matrix.rand(5, 3)
      val B = Matrix.rand(3, 7)
      val C = A %*% B
    }))

    val exp =
      """
        |A = rand(rows=5, cols=3)
        |B = rand(rows=3, cols=7)
        |C = (A %*% B)
      """.stripMargin.trim

    act shouldEqual exp
  }

  "Matrix Multiply Chain" in {
    val act = toDML(idPipeline(u.reify {
      val A = Matrix.rand(5, 3)
      val B = Matrix.rand(3, 7)
      val C = Matrix.rand(7, 7)
      val D = (A %*% B) %*% C
    }))

    val exp =
      """
        |A = rand(rows=5, cols=3)
        |B = rand(rows=3, cols=7)
        |C = rand(rows=7, cols=7)
        |D = ((A %*% B) %*% C)
      """.stripMargin.trim

    act shouldEqual exp
  }

  "Reading a matrix" in {

    val act = toDML(idPipeline(u.reify {
      val A = read("path/to/matrix.csv")
    }))

    val exp =
      """
        |A = read("path/to/matrix.csv")
      """.stripMargin.trim

    act shouldEqual exp
  }

  "Writing a matrix" in {
    val act = toDML(idPipeline(u.reify {
      val B = Matrix.zeros(3, 3)
      write(B, "path/to/matrix.csv", Format.CSV)
    }))

    val exp =
      """
        |B = matrix(0, rows=3, cols=3)
        |write(B, "path/to/matrix.csv", format="csv")
      """.stripMargin.trim

    act shouldEqual exp
  }

  "Control flow" - {

    "For loop" - {

      "without closure modification" in {
        val act = toDML(idPipeline(u.reify {
          for (i <- 1 to 20) {
            println(i)
          }
        }))

        val exp =
          """
            |for (i in 1:20) {
            |  print(i)
            |}
          """.
            stripMargin.trim

        act shouldEqual exp
      }

      "with closure modificiation" in {

        val act = toDML(idPipeline(u.reify {
          var A = 5
          for (i <- 1 to 20) {
            A = A + 1
          }
        }))

        val exp =
          """
            |A = 5
            |for (i in 1:20) {
            |  A = A + 1
            |}
          """.
            stripMargin.trim

        act shouldEqual exp
      }
    }
  }
}
