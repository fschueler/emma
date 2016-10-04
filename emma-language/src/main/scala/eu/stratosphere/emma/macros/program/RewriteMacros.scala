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
        case _ => (expr.tpe, List(expr))
      }
      case _ => (e.tree.tpe, e.tree)
    }

    // generate the actual DML code
    val res = toDML(idPipeline(e))

    val dmlString = s"""
                       |print('Starting SystemML execution')
                       |${res}
                       |print('finished execution...')
      """.stripMargin

    // assemble the input and output parameters to MLContext
    val inParams  = DMLTransform.sources.toList
    val outParams = outNames.map(_.symbol.name.toString)

    // assemble the type of the return expression we want from MLContext
    val outTypes  = outType.typeArgs match {
      case Nil => List(outType)
      case ls => ls
    }

    // this is a workaround for the fact that MLContext only returns tuples
    val out = if (outTypes.length == 1) q"out._1" else q"out"

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
        val out = ml.execute(script).getTuple[..${outTypes}](..${outParams})

        $out
      }
    }"""

    identity(typeCheck = true)(alg)
  }
}

// TODO

// matrix should be abstract and other matrix types should be clearly defines (MLContextMatrix, BreezeMatrix, ...)

// implicit conversion from MLContextMatrix to Matrix for the return type
// --> solve java.lang.ClassCastException: org.apache.sysml.api.mlcontext.Matrix cannot be cast to eu.stratosphere.emma.sysml.api.Matrix

// single bindingrefs as return expressions in the src langauge need to be removed because DML doesn't support them
