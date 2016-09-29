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
    val dmlString = s"""
        |print('Starting SystemML execution')
        |${toDML(idPipeline(e))}
        |print('finished execution...')
      """.stripMargin

    val testString = "print('hello world')"
    // Construct algorithm object
    val alg = q"""
      import eu.stratosphere.emma.api.SystemMLAlgorithm
      import eu.stratosphere.emma.sysml.api._

      import org.apache.sysml.api.MLContext

      new SystemMLAlgorithm[${u.weakTypeOf[T]}]  {
      import _root_.scala.reflect._

      def run(): ${u.weakTypeOf[T]} = {
        val ml = implicitly[MLContext]
        val script: String = $dmlString
        ml.executeScript(script)
      }
    }"""

    identity(typeCheck = true)(alg)
  }

}
