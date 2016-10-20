/*
 * Copyright © 2014 TU Berlin (emma@dima.tu-berlin.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sysml.macros

import org.apache.sysml.compiler.DMLCompiler
org.apache.sysml.api.linalg.api._

import org.emmalanguage.compiler.{Common, MacroCompiler}

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class RewriteMacros(val c: blackbox.Context) extends MacroCompiler with Common {


  import Core.{Lang => core}
  import Source.{Lang => src}
  import universe._

  val idPipeline: c.Expr[Any] => u.Tree =
    dmlPipeline(typeCheck = false).compose(_.tree)

  val toDML: u.Tree => String =
    tree => DMLTransform.generateDML(tree)

  /** Ordering symbols by their name. */
  implicit private val byName: Ordering[u.TermSymbol] =
  Ordering.by(_.name.toString)

  // liftable for input parameters
  implicit val lift = Liftable[(String, u.TermSymbol)] { p =>
    q"(${p._1}, ${p._2})"
  }

  /**
    * The macro entry point to transform the tree and generate the DML Algorithm object
    * @param e the expression inside the parallelize macro
    * @tparam T type of the expression
    * @return an [[SystemMLAlgorithm]] of type T that can execute the DML script and return the result of type T
    */
  def impl[T: c.WeakTypeTag](e: c.Expr[T]) = {

    // TODO this needs to be more robust for possible and impossible return types
    /** extract the return type that has to be retained from mlcontext */
    val (outType: Type, outNames: List[Tree]) = e.tree match {
      case u.Block(_, expr) => expr match {
        case l: u.Literal => (l.tpe, List(l.value))
        case a: u.Apply if a.symbol.name == u.TermName("apply") => (a.tpe, a.args)
        case _ if expr.tpe =:= typeOf[Unit] =>
          (expr.tpe, List())
        case _ =>
          (expr.tpe, List(expr))
      }
      case _ =>
        (e.tree.tpe, e.tree)
    }

    // generate the actual DML code
    val dmlString = toDML(idPipeline(e))

    // assemble the input and output parameters to MLContext
    val inParams  = DMLTransform.sources.toList
    val outParams = outNames.map(_.symbol.name.toString)

    // assemble the type of the return expression we want from MLContext
    val outTypes  = outType.typeArgs match {
      case Nil => List(outType)
      case ls => ls
    }

    // if the return type is Unit, we don't want to call getTuple
    val result = if (outParams.isEmpty) q"()" else q"res.getTuple[..${outTypes}](..${outParams})"

    // this is a workaround for the fact that MLContext only returns tuples
    val out = if (outTypes.length == 1 && outParams.nonEmpty) q"out._1" else q"out"

    // Construct algorithm object
    val alg = q"""
      import eu.stratosphere.emma.api.SystemMLAlgorithm
      import eu.stratosphere.emma.sysml.api._

      import org.apache.sysml.api.mlcontext.{Matrix => _, _}
      import org.apache.sysml.api.mlcontext.ScriptFactory._

      new SystemMLAlgorithm[${u.weakTypeOf[T]}]  {
      import _root_.scala.reflect._

      def run(): ${u.weakTypeOf[T]} = {
        println("Running script:\n" + ${dmlString})
        val ml = implicitly[MLContext]
        val script = dml($dmlString).in(Seq(..${inParams})).out(..${outParams})
        val res = ml.execute(script)
        val out = $result

        $out
      }
    }"""

    identity(typeCheck = true)(alg)
  }
}


// TODO matrix should be abstract and other matrix types should be clearly defines (MLContextMatrix, BreezeMatrix, ...)

// TODO implicit conversion from MLContextMatrix to Matrix for the return type
// --> solve java.lang.ClassCastException: org.apache.sysml.api.mlcontext.Matrix cannot be cast to org.apache.sysml.api.linalg.Matrix

// TODO single bindingrefs as return expressions in the src langauge need to be removed because DML doesn't support them

// TODO Unit as return type should be removed