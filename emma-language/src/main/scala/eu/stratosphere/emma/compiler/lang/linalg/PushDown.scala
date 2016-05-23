package eu.stratosphere.emma.compiler.lang.linalg

import eu.stratosphere.emma.compiler.Common
import eu.stratosphere.emma.compiler.lang.comprehension.Comprehension
import eu.stratosphere.emma.compiler.lang.core.Core

trait PushDown extends Common with Comprehension {
  self: Core =>

  import Term._
  import Tree._
  import universe._

  private[compiler] object PushDown {

    def rowAggregates(tree: Tree): Tree = {
      import eu.stratosphere.emma.api.lara._

      val VectorTpe = Type.weak[Vector[_]]
      val MatrixTpe = Type.weak[Matrix[_]]

      val meta = new Core.Meta(tree)

      object fn {
        def unapply(tree: Tree): Option[(ValDef, Tree)] = tree match {
          case q"(${arg: ValDef}) => ${body: Tree}" =>
            Some(arg, body)
          case Term.ref(sym) =>
            meta.valdef(sym) map {
              case ValDef(_, _, tpe, Function(arg :: Nil, body)) =>
                (arg, body)
            }
        }
      }

      sealed trait Def {
        val s: Symbol
      }
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

          case sel(_, m) if LA.m_syntheticCtors contains m =>
            println(m)
            Synthetic(m)
        }
        root.head
      }


      val transform: Tree => Tree = postWalk {
        // TODO: get this running with app instead of Apply
        case val_(resultVal, Apply(Method.call(matrix, mthd, tpe, Seq(f)), impl), _)
          if (LA.m_rows.alternatives contains mthd) &&
            (Type.of(mthd).finalResultType <:< VectorTpe) =>

          // 1. See if `matrix` is used otherwise
          if (meta.valuses(matrix.symbol) > 1) {
            // true

          } else {
            // false
            // get creation of m
          }
          val res = getRoots(matrix.symbol) match {
            //            case Synthetic(s) =>
            case Transformation(s) =>
              val fn(_, functionBody) = f
              val functionResultType  = Type.result(f)
              val functionInputType   = Type.arg(1, Type.arg(1, f))
              val databagType         = Type.arg(1, Type.of(s))

              // FIXME: For now expect only a single aggregate function in the body
              // otherwise we ignore and do no optimization.
              // A possible solution is to convert the product to a vector an just use the
              // complete function.
              val block((innverFuncVal: ValDef) :: Nil, func) = functionBody
              val innerFunction = ref(Term.sym(innverFuncVal))

              // iterator over product
              val (_, pSym) = Term.name.freshTermAndSymbol("p", databagType)
              val rhs = Core.Language.sel(resolve(pSym), Term.member(pSym, Term.name("productIterator")))
              val (_, itrSym) = Term.name.freshTermAndSymbol("productIterator", rhs.tpe)
              val itrVal = Core.Language.val_(itrSym, rhs)

              // cast fields in tuple to func input type
              val (_, lamArgSym) = Term.name.freshTermAndSymbol("x", Type[Any])
              val asInCall = Core.Language.call(
                resolve(lamArgSym),
                Term.member(lamArgSym, Term.name("asInstanceOf")),
                functionInputType
              )()
              val lamp = Core.Language.lambda(lamArgSym)(Core.Language.let(Seq(), Seq(), asInCall))
              val (_, asInSym) = Term.name.freshTermAndSymbol("anonfun", lamp.tpe)
              val lampVal = Core.Language.val_(asInSym, lamp)

              // call map with instance of call on iterator
              val mapFun = Method.call(
                resolve(itrSym),
                Term.member(itrSym, Term.name("map")),
                functionResultType
              )(Seq(resolve(asInSym)))
              val (_, mapSym) = Term.name.freshTermAndSymbol("map", mapFun.tpe)
              val mapVal = Core.Language.val_(mapSym, mapFun)

              // call reduce with function f
              val red = Core.Language.call(
                resolve(mapSym),
                Term.member(mapSym, Term.name("reduce")),
                functionResultType
              )(Seq(innerFunction))
              val cs = new Comprehension.Syntax(API.bagSymbol)
              val agg = cs.comprehension(
                List(cs.generator(pSym, Core.Language.let(Seq(), Seq(), resolve(s)))),
                cs.head(Core.Language.let(Seq(itrVal, lampVal, mapVal, innverFuncVal), Seq(), red))
              )
              val (_, aggSym) = Term.name.freshTermAndSymbol("agg", agg.tpe)
              val aggVal = Core.Language.val_(aggSym, agg)

              // transform to vector
              val toVec = Core.Language.call(
                Core.Language.ref(LA.transSymbol),
                LA.v_toVector.asTerm,
                functionResultType
              )(Seq(resolve(aggSym)), impl)

              val let = Core.Language.let(Seq(aggVal), Seq(), toVec)
              val toVecVal = Core.Language.val_(resultVal, let)
              toVecVal
          }
          res
      }

      (transform andThen Core.dce andThen Core.anf andThen Core.simplify) (tree)
    }


  }

}
