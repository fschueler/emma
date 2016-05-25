package eu.stratosphere.emma.compiler.lang.linalg

import eu.stratosphere.emma.compiler.Common
import eu.stratosphere.emma.compiler.lang.comprehension.Comprehension
import eu.stratosphere.emma.compiler.lang.core.Core

trait AlgebraicTransformations extends Common with Comprehension {
  self: Core =>

  import Term._
  import Tree._
  import universe._

  private[compiler] object AlgebraicTransformations {

    def applyRules(tree: Tree): Tree = {
      import eu.stratosphere.emma.api.lara._

      val VectorTpe = Type.weak[Vector[_]]
      val MatrixTpe = Type.weak[Matrix[_]]

      val meta = new Core.Meta(tree)

      sealed trait Def { val s: Symbol }
      case class Transformation(s: Symbol) extends Def
      case class Synthetic(s: Symbol) extends Def

      /**
        * Returns the origin of matrix `sym`, which can either a databag or a synthetic ctor.
        *
        * @param sym the symbol of the matrix
        * @return
        */
      def getRoots(sym: Symbol): Def = {
        val mCreation = meta.valdef(sym).head

        // TODO: check for indexed versions as we have possible
        // "synthetic" sparse values we have to consider in the
        // agg method over the bag as well
        val root = mCreation.collect[Def] {
          case Method.call(a, LA.m_toMatrix, b, Seq(f)) =>
            println(s"$a, $b, $f, ${Type.of(f)}")
            Transformation(f.symbol)
          case Method.call(a, LA.m_vecToMatrix, b, Seq(f)) =>
            println(s"$a, $b, $f, ${Type.of(f)}")
            Transformation(f.symbol)
          // from bag
          case Method.call(a, mthd, b, Seq(f)) // maybe just omit check?
            if (mthd == LA.m_toMatrix) ||
              (mthd == LA.m_vecToMatrix) ||
              (LA.m_indexedToMatrix.alternatives contains mthd) =>
            println(s"$a, $b, $f, ${Type.of(f)}")
            Transformation(f.symbol)

          case m@Apply(Apply(TypeApply(sel(_, n), _), Seq(_, _)), Seq(f))
            if LA.m_indexedToMatrix.alternatives contains n => // indexed /w explicit size
            println(s"$f, $n")
            Transformation(f.symbol)

          case sel(a, LA.m_transpose) =>
            Synthetic(a.symbol)
        }
        root.head
      }

      // find out if the valdef for this symbol is of the form val x$1 = X.transpose()
      def isTransposed(tree: Tree): Boolean = {
        meta.valdef(tree.symbol) match {
          case Some(val_(_, a@app(mthd, _, _), _))
            if LA.m_transpose.alternatives contains mthd.symbol => true
          case _ => false
        }
      }

      def transform: Tree => Tree = preWalk {
        /* find all matrix multiplications of the type M x V -> V */
        case orig@Method.call(x@matrix, m@mthd, Seq(tpe), Seq(y@v))
          if (LA.m_mult.alternatives contains mthd)
            && (Type.of(mthd).finalResultType <:< VectorTpe) =>

          // In case the multiplication is of the form M.t x V -> V we want to rewrite it as (M x v.t).t -> V
          if (isTransposed(x) && !isTransposed(y)) {
            val nx = getRoots(x.symbol) match {
              case Synthetic(s) => s
              case _ => x
            }

            Method.call(resolve(y.symbol), LA.m_transpose.asTerm, tpe)()
          }
          orig
      }

      val res = (transform andThen Core.dce) (tree)
      res
    }
  }
}
