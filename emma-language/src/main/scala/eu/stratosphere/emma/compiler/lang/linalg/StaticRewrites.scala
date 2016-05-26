package eu.stratosphere.emma.compiler.lang.linalg

import eu.stratosphere.emma.compiler.lang.core.Core
import eu.stratosphere.emma.compiler.{Common, Rewrite}

private[linalg] trait StaticRewrites extends Common with Rewrite {
  self: Core with LinAlg =>

  import universe._
  import LinAlg.{Syntax}
  import Tree._

  private[linalg] object StaticRewrites {
    var cmeta: Core.Meta = _

    def rewrite(matrix: Symbol)(tree: Tree): Tree = {
      cmeta = new Core.Meta(tree)

      val ms = new Syntax() with StaticRewriteRules

      ({
        Engine.postWalk(List(ms.Mtv))
      }) (tree)
    }
  }

  protected[linalg] trait StaticRewriteRules {
    self: Syntax =>

    /**
      * Rewrites an expression of the form X.t %*% v into (v.t %*% X).t to avoid matrix transpose
      */
    object Mtv extends Rule {

      case class RuleMatch(enclosing: Block, parent: Tree,  x: Symbol, y: Symbol) {
        lazy val bs1 = enclosing.children.takeWhile(_ != parent)
        lazy val bs2 = enclosing.children.reverse.takeWhile(_ != parent).reverse
      }

      override def bind(root: Tree): Traversable[RuleMatch] = root match {
        case enclosing: Block => new Traversable[RuleMatch] {
          override def foreach[U](f: (RuleMatch) => U): Unit = {
            val parents = (enclosing.stats collect {
              // a ValDef statement with a matrix multiplications of the type M x V -> V as rhs
              case val_(_, orig@matmultvec(x, y, tpe), _) => orig
            }) ++ (enclosing.expr match {
              // an expr with a matrix multiplications of the type M x V -> V
              case orig@matmultvec(x, y, tpe) => List(orig)
              case _ => Nil
            })

            for (m <- parents) m match {
              case matmultvec(x, y, _) => f(RuleMatch(enclosing, m, x.symbol, y.symbol))
            }
          }
        }
        case _ => Traversable.empty[RuleMatch]
      }

      override def guard(rm: RuleMatch): Boolean = {
        // check if matrix is transposed
        val xt = StaticRewrites.cmeta.valdef(rm.x) match {
          case Some(val_(_, mtranspose(mat), _)) => true
          case _ => false
        }

        // check if vector is not transposed
        val y = StaticRewrites.cmeta.valdef(rm.y) match {
          case Some(val_(_, vtranspose(mat), _)) => false
          case _ => true
        }

        xt && y
      }

      override def fire(rm: RuleMatch): Tree = {
        val x  = StaticRewrites.cmeta.valdef(rm.x) match {
          case Some(val_(_, mtranspose(mat), _)) => mat
        }
        // create rewritten statements for expression
        val ytsy = Term.name.freshTermAndSymbol("yt", Type.of(rm.y))._2
        val yt   = val_(ytsy, Method.call(resolve(rm.y), LA.v_transpose.asTerm)(Seq()))

        val ytXsy = Term.name.freshTermAndSymbol("ytX", Type.of(yt.symbol))._2
        val vmult = Term.app(Term.sel(resolve(yt.symbol), LA.v_mult.asTerm, Type.of(yt.symbol)))(Seq(x))

        val ytX  = val_(ytXsy, vmult)

        val ytXtsy = Term.name.freshTermAndSymbol("ytXt", Type.of(ytX.symbol))._2
        val ytXt = Method.call(resolve(ytX.symbol), LA.v_transpose.asTerm)(Seq())

        block(rm.bs1 ++ List(yt,  ytX, ytXt) ++ rm.bs2)
      }

    }
  }


}
