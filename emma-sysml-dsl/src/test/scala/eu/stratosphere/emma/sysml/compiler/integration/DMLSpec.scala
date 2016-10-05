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
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import resource._

/** A spec for SystemML Algorithms. */
@RunWith(classOf[JUnitRunner])
class DMLSpec extends BaseCompilerSpec {

  import compiler._
  import Core.{Lang => core}

  // ---------------------------------------------------------------------------
  // Transformation pipelines
  // ---------------------------------------------------------------------------

  val anfPipeline: u.Expr[Any] => u.Tree =
    compiler.pipeline(typeCheck = true)(
      Core.anf
    ).compose(_.tree)

  val liftPipeline: u.Expr[Any] => u.Tree =
    compiler.pipeline(typeCheck = true)(
      Core.anf
    ).compose(_.tree)

  val prettyPrint: u.Tree => String =
    tree => time(Core.prettyPrint(tree), "pretty print")

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

  "Matrix constructors" in {

    val t = idPipeline(u.reify {
      val x$01 = Matrix.rand(3, 3)
      val x$02 = Matrix.zeros(3, 3)
      x$01 + x$02
    })

    val acts = (t collect {
      case u.Block(stats, _) => stats.collect {
        case vd@u.ValDef(_, _, _, _) => toDML(vd)
      }
    }).flatten

    val exps =
      """
        |x$01 = rand(rows=3, cols=3)
        |x$02 = matrix(0, rows=3, cols=3)
      """.stripMargin.trim.split('\n')

    acts.length shouldEqual exps.length

    (acts zip exps) foreach { case (act, exp) =>
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
        |C = A %*% B
      """.stripMargin.trim

    act shouldEqual exp
  }

  "Reading a matrix" in {

    val act = toDML(idPipeline(u.reify {
      val A = read("path/to/matrix.csv")
      val B = Matrix.zeros(3, 3)
      val C = A %*% B
    }))

    val exp =
      """
        |A = read("path/to/matrix.csv")
        |B = matrix(0, rows=3, cols=3)
        |C = A %*% B
      """.stripMargin.trim

    act shouldEqual exp
  }

  "Writing a matrix" in {
    val act = toDML(idPipeline(u.reify {
      val A = read("path/to/matrix.csv")
      val B = Matrix.zeros(3, 3)
      val C = A %*% B
      write(C, "path/to/matrix.csv", Format.CSV)
    }))

    val exp =
      """
        |A = read("path/to/matrix.csv")
        |B = matrix(0, rows=3, cols=3)
        |C = A %*% B
        |write(C, "path/to/matrix.csv", format="csv")
      """.stripMargin.trim

    act shouldEqual exp
  }
}
