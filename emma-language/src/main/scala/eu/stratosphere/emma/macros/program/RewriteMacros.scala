package eu.stratosphere.emma.macros.program

import eu.stratosphere.emma.api.SystemMLAlgorithm
import eu.stratosphere.emma.compiler.{Common, MacroCompiler}
import eu.stratosphere.emma.macros.utility.DMLEnvironment

import scala.collection.SortedSet
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class RewriteMacros(val c: blackbox.Context) extends MacroCompiler with Common {

  import Core.{Lang => core}
  import Source.{Lang => src}

  import universe._

  val idPipeline: c.Expr[Any] => u.Tree =
    identity(typeCheck = false).compose(_.tree)

  val toDML: u.Tree => String =
    tree => Core.generateDML(tree)

  def impl[T: c.WeakTypeTag](e: c.Expr[T]) = {

    // TODO this needs to be more robust for possible and impossible return types
    /** construct the return type that has to be retained from mlcontext */
    val (outType: Type, outNames: List[Tree]) = e.tree match {
      case u.Block(_, expr) => expr match {
        case l: u.Literal => (l.tpe, List(l.value))
        case a: u.Apply if a.symbol.name == u.TermName("apply")=> (a.tpe, a.args)
        case _ => (expr.tpe, List(expr))
      }
      case _ => (e.tree.tpe, e.tree)
    }

    val res = toDML(idPipeline(e))

    val dmlString = s"""
        |print('Starting SystemML execution')
        |${res}
        |print('finished execution...')
      """.stripMargin

    val outParams = outNames.map(x => s""""$x"""").mkString(", ")

    // Construct algorithm object
    val alg = q"""
      import eu.stratosphere.emma.api.SystemMLAlgorithm
      import eu.stratosphere.emma.sysml.api._

      import org.apache.sysml.api.mlcontext.{Matrix => _, _}
      import org.apache.sysml.api.mlcontext.ScriptFactory._

      new SystemMLAlgorithm[${u.weakTypeOf[T]}]  {
      import _root_.scala.reflect._

      def run(): ${u.weakTypeOf[T]} = {
        val ml = implicitly[MLContext]
        val script = dml($dmlString).out()
        val out = ml.execute(script).getTuple[$outType]($outParams)

        out
      }
    }"""

    identity(typeCheck = true)(alg)
  }

}
