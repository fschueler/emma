//package eu.stratosphere.emma.compiler.lang.linalg
//
//import eu.stratosphere.emma.compiler.Common
//import eu.stratosphere.emma.compiler.lang.comprehension.Comprehension
//import eu.stratosphere.emma.compiler.lang.core.Core
//
//trait PushDownCore extends Common with Comprehension {
//  self: Core =>
//
//  import Term._
//  import Tree._
//  import universe._
//
//  private[compiler] object PushDown {
//
//    def rowAggregates(tree: Tree): Tree = {
//      import eu.stratosphere.emma.api.lara._
//
//      val VectorTpe = Type.weak[Vector[_]]
//      val MatrixTpe = Type.weak[Matrix[_]]
//
//      val meta = new Core.Meta(tree)
//
//      object fn {
//        def unapply(tree: Tree): Option[(ValDef, Tree)] = tree match {
//          case q"(${arg: ValDef}) => ${body: Tree}" =>
//            Some(arg, body)
//          case Term.ref(sym) =>
//            meta.valdef(sym) map {
//              case ValDef(_, _, tpe, Function(arg :: Nil, body)) =>
//                (arg, body)
//            }
//        }
//      }
//
//      sealed trait Def { val s: Symbol }
//      case class Transformation(s: Symbol) extends Def
//      case class Synthetic(s: Symbol) extends Def
//
//      /**
//       * Returns the origin of matrix `sym`, which can either a databag or a synthetic ctor.
//       *
//       * @param sym the symbol of the matrix
//       * @return
//       */
//      def getRoots(sym: Symbol): Def = {
//        val mCreation = meta.valdef(sym).head
//
//        // TODO: check for indexed versions as we have possible
//        // "synthetic" sparse values we have to consider in the
//        // agg method over the bag as well
//        val root = mCreation.collect[Def] {
//          case Method.call(a, LA.m_toMatrix, b, Seq(f)) =>
//            println(s"$a, $b, $f, ${Type.of(f)}")
//            Transformation(f.symbol)
//          case Method.call(a, LA.m_vecToMatrix, b, Seq(f)) =>
//            println(s"$a, $b, $f, ${Type.of(f)}")
//            Transformation(f.symbol)
//          // from bag
//          case Method.call(a, mthd, b, Seq(f)) // maybe just omit check?
//            if (mthd == LA.m_toMatrix) ||
//              (mthd == LA.m_vecToMatrix) ||
//              (LA.m_indexedToMatrix.alternatives contains mthd) =>
//            println(s"$a, $b, $f, ${Type.of(f)}")
//            Transformation(f.symbol)
//
//          case m@Apply(Apply(TypeApply(sel(_, n), _), Seq(_, _)), Seq(f))
//            if LA.m_indexedToMatrix.alternatives contains n => // indexed /w explicit size
//            println(s"$f, $n")
//            Transformation(f.symbol)
//        }
//        root.head
//      }
//
////      val transform: Tree => Tree = preWalk {
////        // TODO: get this running with app instead of Apply
////        case Apply(m@Method.call(matrix, mthd, tpe, Seq(f)), impl)
////          if (LA.m_rows.alternatives contains mthd) &&
////            (Type.of(mthd).finalResultType <:< VectorTpe) =>
////
////          println(s"$matrix, $mthd, $tpe, $f, $impl")
////
////          // 1. See if `matrix` is used otherwise
////          if (meta.valuses(matrix.symbol) > 1) {
////            // true
////
////          } else {
////            // false
////            // get creation of m
////          }
////          val res = getRoots(matrix.symbol) match {
////            //            case Synthetic(s) =>
////            case Transformation(s) =>
////              val fn(arg, body) = f
////              val block((v: ValDef) :: Nil, func) = body
////              // TODO: get return type of INNER FUNCTION instead of body
////              val innerReduce = ref(Term.sym(v))
////              val u2 = Type.of(func)
////              val check = Type.result(innerReduce)
////
////              val dbType = Type.of(s)
////              val tupleType = Type.arg(1, dbType)
////
////              val (p, pSym) = Term.name.freshAndSym("p", tupleType)
////
////              // iterator
////              // <synthetic> val productIterator$1: Iterator[Any] = p.productIterator;
////              val rhs = Core.Language.sel(resolve(pSym), Term.member(pSym, Term.name("productIterator")))
////              val (itr, itrSym) = Term.name.freshAndSym("productIterator", rhs.tpe)
////              val itrVal = Core.Language.val_(itrSym, rhs)
////              //              <synthetic> val anonfun$1: Any => Int = ((x$5: Any) => {
////              //                x$5.asInstanceOf[Int]
////              //                });
////              val (lamArg, lamArgSym) = Term.name.freshAndSym("x", Type[Any])
////              val asInCall = Core.Language.call(
////                resolve(lamArgSym),
////                Term.member(lamArgSym, Term.name("asInstanceOf")),
////                Type.of(body)
////              )()
////              val lamp = Core.Language.lambda(lamArgSym)(block(asInCall))
////              val (asIn, asInSym) = Term.name.freshAndSym("anonfun", lamp.tpe)
////              val lampVal = Core.Language.val_(asInSym, lamp)
////              // <synthetic> val map$1: Iterator[Int] = productIterator$1.map[Int](anonfun$1);
////              val mapFun = Method.call(
////                resolve(itrSym),
////                Term.member(itrSym, Term.name("map")),
////                Type.of(body)
////              )(Seq(resolve(asInSym)))
////              val (map, mapSym) = Term.name.freshAndSym("map", mapFun.tpe)
////              val mapVal = Core.Language.val_(mapSym, mapFun)
////              // map$1.reduce[Int](anonfun$2)
////              val red = Core.Language.call(
////                resolve(mapSym),
////                Term.member(mapSym, Term.name("reduce")),
////                check
////              )(Seq(innerReduce))
////
////              val cs = new Comprehension.Syntax(API.bagSymbol)
////              val inner = Tree.inline(v)
////              val agg = cs.comprehension(
////                List(cs.generator(pSym, block(resolve(s)))),
////                cs.head(block(itrVal, lampVal, mapVal, inner, red))
////              )
////              // TODO: Can we reuse the orig. sym and just replace the type?
////              val (result, resultSym) = Term.name.freshAndSym("agg", agg.tpe)
////              val resultVal = Core.Language.val_(resultSym, agg)
////              resultVal
////              // transform to vector
////              val toVec = Core.Language.call(
////                resolve(LA.transSymbol),
////                LA.v_toVector.asTerm,
////                Type.of(body) // TODO: Check if we have correct result type
////              )(Seq(ref(resultSym)))
//////              val test = Term.name.freshAndSym("test", toVec.tpe)
//////              val toVecVal = val_(a, toVec)
////              Core.Language.let(Seq(resultVal), Seq(), toVec)
////          }
////          res
////      }
//
//
//      val transform: Tree => Tree = preWalk {
////        case v@val_(a,b,c) =>
////          println(s"$a,$b,$c")
////          v
//        // TODO: get this running with app instead of Apply
//        case v@val_(a, Apply(m@Method.call(matrix, mthd, tpe, Seq(f)), impl), _)
//          if (LA.m_rows.alternatives contains mthd) &&
//            (Type.of(mthd).finalResultType <:< VectorTpe) =>
//
//          println(s"$a, $matrix, $mthd, $tpe, $f, $impl")
//
//          // 1. See if `matrix` is used otherwise
//          if (meta.valuses(matrix.symbol) > 1) {
//            // true
//
//          } else {
//            // false
//            // get creation of m
//          }
//          val res = getRoots(matrix.symbol) match {
//            //            case Synthetic(s) =>
//            case Transformation(s) =>
//              val fn(arg, body) = f
//              val block((v: ValDef) :: Nil, func) = body
//              // TODO: get return type of INNER FUNCTION instead of body
//              val innerReduce = ref(Term.sym(v))
//              val u2 = Type.of(func)
//              val check = Type.result(innerReduce)
//
//              val dbType = Type.of(s)
//              val tupleType = Type.arg(1, dbType)
//
//              val (p, pSym) = Term.name.freshAndSym("p", tupleType)
//
//              // iterator
//              // <synthetic> val productIterator$1: Iterator[Any] = p.productIterator;
//              val rhs = Core.Language.sel(resolve(pSym), Term.member(pSym, Term.name("productIterator")))
//              val (itr, itrSym) = Term.name.freshAndSym("productIterator", rhs.tpe)
//              val itrVal = Core.Language.val_(itrSym, rhs)
////              <synthetic> val anonfun$1: Any => Int = ((x$5: Any) => {
////                x$5.asInstanceOf[Int]
////                });
//              val (lamArg, lamArgSym) = Term.name.freshAndSym("x", Type[Any])
//              val asInCall = Core.Language.call(
//                resolve(lamArgSym),
//                Term.member(lamArgSym, Term.name("asInstanceOf")),
//                Type.of(body)
//              )()
//              val lamp = Core.Language.lambda(lamArgSym)(Core.Language.let(Seq(), Seq(), asInCall))
//              val (asIn, asInSym) = Term.name.freshAndSym("anonfun", lamp.tpe)
//              val lampVal = Core.Language.val_(asInSym, lamp)
//              // <synthetic> val map$1: Iterator[Int] = productIterator$1.map[Int](anonfun$1);
//              val mapFun = Method.call(
//                resolve(itrSym),
//                Term.member(itrSym, Term.name("map")),
//                Type.of(body)
//              )(Seq(resolve(asInSym)))
//              val (map, mapSym) = Term.name.freshAndSym("map", mapFun.tpe)
//              val mapVal = Core.Language.val_(mapSym, mapFun)
//              // map$1.reduce[Int](anonfun$2)
//              val red = Core.Language.call(
//                resolve(mapSym),
//                Term.member(mapSym, Term.name("reduce")),
//                check
//              )(Seq(innerReduce))
//
//              val cs = new Comprehension.Syntax(API.bagSymbol)
//              val inner = Tree.inline(v)
//              val agg = cs.comprehension(
//                List(cs.generator(pSym, Core.Language.let(Seq(), Seq(), resolve(s)))),
//                cs.head(Core.Language.let(Seq(itrVal, lampVal, mapVal, v), Seq(), red))
//              )
//              // TODO: Can we reuse the orig. sym and just replace the type?
//              val (result, resultSym) = Term.name.freshAndSym("agg", agg.tpe)
//              val resultVal = Core.Language.val_(resultSym, agg)
//              resultVal
//              // transform to vector
//              val toVec = Core.Language.call(
//                resolve(LA.transSymbol),
//                LA.v_toVector.asTerm,
//                Type.of(a) // TODO: Check if we have correct result type
//              )(Seq(ref(resultSym)))
//
//              val appTrans = Core.Language.app(toVec.symbol.asMethod)(impl)
//
//              val test = Term.name.freshAndSym("test", toVec.tpe)
//              val toVecVal = val_(a, toVec)
//              toVecVal
//          }
//          res
//      }
//
//      (transform andThen Core.dce andThen Core.anf) (tree)
//    }
//
//
//  }
//}
