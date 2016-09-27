package eu.stratosphere.emma.macros.program

import eu.stratosphere.emma.api.SystemMLAlgorithm
import eu.stratosphere.emma.compiler.MacroCompiler

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class RewriteMacros(val c: blackbox.Context) extends MacroCompiler {

  import Core.{Lang => core}
  import universe._

  val idPipeline: c.Expr[Any] => u.Tree =
    identity(typeCheck = false).compose(_.tree)

  val toDML: u.Tree => String =
    tree => Core.generateDML(tree)

  def impl[T: c.WeakTypeTag](e: c.Expr[T]) = {
    val dmlstring = core.Lit(toDML(idPipeline(e)))

    // Construct algorithm object
    val alg = q"""
      import eu.stratosphere.emma.api.SystemMLAlgorithm
      import eu.stratosphere.emma.sysml.api._

      new SystemMLAlgorithm[${u.weakTypeOf[T]}]  {
      import _root_.scala.reflect._

      def run(): ${u.weakTypeOf[T]} = {
        $dmlstring
        Matrix.rand(3, 3)
      }
    }"""

    identity(typeCheck = true)(alg)
  }

}
