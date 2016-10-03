package eu.stratosphere.emma.macros.program

import eu.stratosphere.emma.api.SystemMLAlgorithm
import eu.stratosphere.emma.compiler.{Common, MacroCompiler}

import cats.std.all._

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

  /** Ordering symbols by their name. */
  implicit private val byName: Ordering[u.TermSymbol] =
  Ordering.by(_.name.toString)

  implicit val lift = Liftable[(String, u.TermSymbol)] { p =>
    q"(${p._1}, ${p._2})"
  }

  def impl[T: c.WeakTypeTag](e: c.Expr[T]) = {

    // TODO this needs to be more robust for possible and impossible return types
    /** construct the return type that has to be retained from mlcontext */
    val (outType: Type, outNames: List[Tree]) = e.tree match {
      case u.Block(_, expr) => expr match {
        case l: u.Literal => (l.tpe, List(l.value))
        case a: u.Apply if a.symbol.name == u.TermName("apply") => (a.tpe, a.args)
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

    val inParams  = DMLTransform.sources.toList
    val outParams = outNames.map(_.symbol.name.toString)

    // Construct algorithm object
    val alg = q"""
      import eu.stratosphere.emma.api.SystemMLAlgorithm
      import eu.stratosphere.emma.sysml.api._

      import org.apache.sysml.api.mlcontext.{Matrix => _, _}
      import org.apache.sysml.api.mlcontext.ScriptFactory._

      new SystemMLAlgorithm[${u.weakTypeOf[T]}]  {
      import _root_.scala.reflect._

      def run(): ${u.weakTypeOf[T]} = {
        println("Running script:" + ${dmlString})
        val ml = implicitly[MLContext]
        val script = dml($dmlString).in(Seq(..${inParams})).out(..${outParams})
        val out = ml.execute(script).getTuple[..${outType.typeArgs}](..${outParams})
        out
      }
    }"""

    identity(typeCheck = true)(alg)
  }
}

// (1.0, 1.0, 1.0)
