package eu.stratosphere.emma.compiler.lang.linalg

import eu.stratosphere.emma.api.lara.{Matrix, Vector}
import eu.stratosphere.emma.compiler.Common
import eu.stratosphere.emma.compiler.lang.comprehension.Comprehension
import eu.stratosphere.emma.compiler.lang.core.Core

trait LinAlg extends Common with Comprehension {
  self: Core =>

  import Term._
  import Tree._
  import universe._

  private[emma] object LinAlg {

    def buildChains(tree: Tree): Tree = {

      val transform: Tree => Any = bottomUp {
        case a =>
          println(a)
          a
      }

      val x = transform (tree)

      tree
    }

    // -------------------------------------------------------------------------
    // Mock comprehension syntax language
    // -------------------------------------------------------------------------

    trait MatrixOps {
      val symbol: TermSymbol

      def apply(xs: Tree)(fn: Tree): Tree

      def unapply(tree: Tree): Option[(Tree, Tree)]

    }

    object Syntax {

      val monadTpe /*  */ = LA.MATRIX//monad.asType.toType.typeConstructor
      val moduleSel /* */ = resolve(LA.moduleSymbol)

      val cs = new Comprehension.Syntax(API.bagSymbol)

      val VectorTpe = Type.weak[Vector[_]]
      val MatrixTpe = Type.weak[Matrix[_]]

      // -----------------------------------------------------------------------
      // Matrix Ops
      // -----------------------------------------------------------------------

      object rowAggregate {
        val symbol = LA.m_rows.asTerm

        def apply(lhs: TermSymbol, matrix: Tree, tpe: Type, func: Tree, impl: Seq[Tree]): Tree = {
          val mCall = Method.call(matrix, symbol, tpe)(Seq(func))
          val appl = Term.app(mCall)(impl)
          val_(lhs, appl)
        }

        def unapply(apply: Tree): Option[(TermSymbol, Tree, Type, Tree, Seq[Tree])] = apply match {
          case val_(resultVal, Apply(Method.call(matrix, mthd, tpe, Seq(f)), impl), _)
            if (symbol.alternatives contains mthd) && (Type.of(mthd).finalResultType <:< VectorTpe) =>
            Some(resultVal, matrix, Type.result(f), f, impl)
          case _ => None
        }
      }
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    sealed trait Def { val s: Symbol }
    case class Transformation(s: Symbol) extends Def
    case class Synthetic(s: Symbol) extends Def
    case class Operation(s: Symbol) extends Def

    case class Chain(s: Seq[Def])

    def getRoots(tree: Tree, mSym: Symbol): Chain = {
      import Term._
      import universe._

      val meta = new Core.Meta(tree)

      val c = List.newBuilder[Def]

      def getValDef(sym: Symbol): Unit = {
        val mCreation = meta.valdef(sym).head
        val root = mCreation.collect[Unit] {
          case Method.call(a, LA.m_toMatrix, b, Seq(f)) =>
            c += Transformation(f.symbol)
          case Method.call(a, LA.m_vecToMatrix, b, Seq(f)) =>
            c += Transformation(f.symbol)
          // from bag
          case Method.call(a, mthd, b, Seq(f)) // maybe just omit check?
            if (mthd == LA.m_toMatrix) ||
               (mthd == LA.m_vecToMatrix) ||
               (LA.m_indexedToMatrix.alternatives contains mthd) =>
            c += Transformation(f.symbol)

          case m@Apply(Apply(TypeApply(sel(_, n), _), Seq(_, _)), Seq(f))
            if LA.m_indexedToMatrix.alternatives contains n => // indexed /w explicit size
            c += Transformation(f.symbol)

          case sel(_, m) if LA.m_syntheticCtors contains m =>
            c += Synthetic(m)

          // consider control flow as well!
          case Method.call(a, mthd, b, Seq(f)) if LA.methods contains mthd =>
            println(s"$a")
            c += Operation(a.symbol)
            getValDef(a.symbol)
        }
      }
      getValDef(mSym)
      Chain(c.result())
    }
  }

}
