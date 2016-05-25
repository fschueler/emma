package eu.stratosphere.emma.compiler.lang.linalg

import eu.stratosphere.emma.compiler.lang.core.Core
import eu.stratosphere.emma.compiler.{Common, Rewrite}

private[linalg] trait StaticRewrites extends Common with Rewrite {
  self: Core with LinAlg =>

  import universe._
  import LinAlg.{Syntax}
  import Tree._

  private[linalg] object StaticRewrites {

    def rewrite(matrix: Symbol)(tree: Tree): Tree = {
      val ms = new Syntax(matrix: Symbol) with StaticRewriteRules

      ({
        Engine.postWalk(List(ms.Mtv))
      }) (tree)
    }
  }

  protected[linalg] trait StaticRewriteRules {
    self: Syntax =>

    /**
      * Rewrites an expression of the form X.t %*% v into (X %*% v.t).t
      */
    object Mtv extends Rule {

      case class RuleMatch(tree: Tree)

      override def bind(root: Tree): Traversable[RuleMatch] = {

        root match {
          /* find all matrix multiplications of the type M x V -> V */
          case orig@matmult(x@xx, y@yy) =>
            val x = xx
            val y = yy
            val mtch = RuleMatch(orig)
            // In case the multiplication is of the form M.t x V -> V we want to rewrite it as (M x v.t).t -> V
            Seq(mtch)

          case _ =>
            Traversable.empty[RuleMatch]
        }
      }

      override def guard(rm: RuleMatch): Boolean =
        true

      override def fire(rm: RuleMatch): Tree = {
        block()
      }

    }
  }


}
