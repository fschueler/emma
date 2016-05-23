package eu.stratosphere.emma.compiler.lang.linalg

import eu.stratosphere.emma.api.lara.{Matrix, Vector}
import eu.stratosphere.emma.compiler.Common
import eu.stratosphere.emma.compiler.lang.comprehension.Comprehension
import eu.stratosphere.emma.compiler.lang.core.Core

trait LinAlg extends Common with Comprehension {
  self: Core =>

  import universe._
  import Tree._

  private[emma] object LinAlg {

    // -------------------------------------------------------------------------
    // Mock comprehension syntax language
    // -------------------------------------------------------------------------

    trait MatrixOps {
      val symbol: TermSymbol

      def apply(xs: Tree)(fn: Tree): Tree

      def unapply(tree: Tree): Option[(Tree, Tree)]

      def monadic(xs: Tree)(fn: Tree): Tree
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

//      object map extends MonadOp {
//
//        override val symbol = LA.m_rows
//
//        override def apply(xs: Tree)(f: Tree): Tree =
//          Method.call(xs, symbol, Type.arg(2, f))(f :: Nil)
//
//        override def unapply(apply: Tree): Option[(Tree, Tree)] = apply match {
//          case Method.call(xs, `symbol`, _, Seq(f)) => Some(xs, f)
//          case _ => None
//        }
//      }


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
