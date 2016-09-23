package eu.stratosphere.emma.macros.program

import eu.stratosphere.emma.compiler.MacroCompiler

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class RewriteMacros(val c: blackbox.Context) extends MacroCompiler {

  import Core.{Lang => core}

  val idPipeline: c.Expr[Any] => u.Tree =
    identity(typeCheck = false).compose(_.tree)

  val toDML: u.Tree => String =
    tree => Core.generateDML(tree)

  def impl[T](e: c.Expr[T]): c.Expr[String] = c.Expr(
    core.Lit(
      toDML(idPipeline(e))
    )
  )
}
