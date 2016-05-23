package eu.stratosphere
package emma.compiler

import org.scalactic._
import org.scalactic.Accumulation._

/** Common IR tools. */
trait Common extends ReflectUtil {

  import universe._

  // --------------------------------------------------------------------------
  // Emma API
  // --------------------------------------------------------------------------

  /** A set of API method symbols to be comprehended. */
  protected[emma] object API {
    //@formatter:off
    val moduleSymbol          = rootMirror.staticModule("eu.stratosphere.emma.api.package")
    val bagSymbol             = rootMirror.staticClass("eu.stratosphere.emma.api.DataBag")
    val groupSymbol           = rootMirror.staticClass("eu.stratosphere.emma.api.Group")
    val statefulSymbol        = rootMirror.staticClass("eu.stratosphere.emma.api.Stateful.Bag")
    val inputFmtSymbol        = rootMirror.staticClass("eu.stratosphere.emma.api.InputFormat")
    val outputFmtSymbol       = rootMirror.staticClass("eu.stratosphere.emma.api.OutputFormat")

    val apply                 = bagSymbol.companion.info.decl(TermName("apply"))
    val read                  = moduleSymbol.info.decl(TermName("read"))
    val write                 = moduleSymbol.info.decl(TermName("write"))
    val stateful              = moduleSymbol.info.decl(TermName("stateful"))
    val fold                  = bagSymbol.info.decl(TermName("fold"))
    val map                   = bagSymbol.info.decl(TermName("map"))
    val flatMap               = bagSymbol.info.decl(TermName("flatMap"))
    val withFilter            = bagSymbol.info.decl(TermName("withFilter"))
    val groupBy               = bagSymbol.info.decl(TermName("groupBy"))
    val minus                 = bagSymbol.info.decl(TermName("minus"))
    val plus                  = bagSymbol.info.decl(TermName("plus"))
    val distinct              = bagSymbol.info.decl(TermName("distinct"))
    val fetchToStateless      = statefulSymbol.info.decl(TermName("bag"))
    val updateWithZero        = statefulSymbol.info.decl(TermName("updateWithZero"))
    val updateWithOne         = statefulSymbol.info.decl(TermName("updateWithOne"))
    val updateWithMany        = statefulSymbol.info.decl(TermName("updateWithMany"))

    val methods = Set(
      read, write,
      stateful, fetchToStateless, updateWithZero, updateWithOne, updateWithMany,
      fold,
      map, flatMap, withFilter,
      groupBy,
      minus, plus, distinct
    ) ++ apply.alternatives

    val monadic = Set(map, flatMap, withFilter)
    val updateWith = Set(updateWithZero, updateWithOne, updateWithMany)

    // Type constructors
    val DATA_BAG              = typeOf[eu.stratosphere.emma.api.DataBag[Nothing]].typeConstructor
    val GROUP                 = typeOf[eu.stratosphere.emma.api.Group[Nothing, Nothing]].typeConstructor
    //@formatter:on
  }

  protected[emma] object IR {
    //@formatter:off
    val module                = rootMirror.staticModule("eu.stratosphere.emma.compiler.ir.package").asModule

    val flatten               = module.info.decl(TermName("flatten")).asTerm
    val generator             = module.info.decl(TermName("generator")).asTerm
    val comprehension         = module.info.decl(TermName("comprehension")).asTerm
    val guard                 = module.info.decl(TermName("guard")).asTerm
    val head                  = module.info.decl(TermName("head")).asTerm

    val comprehensionOps      = Set(flatten, generator, comprehension, guard, head)
    //@formatter:on
  }

  protected[emma] object LA {
    //@formatter:off
    val moduleSymbol          = rootMirror.staticModule("eu.stratosphere.emma.api.lara.package")
    val matrixSymbol          = rootMirror.staticClass("eu.stratosphere.emma.api.lara.Matrix")
    val vectorSymbol          = rootMirror.staticClass("eu.stratosphere.emma.api.lara.Vector")

    val transSymbol           = rootMirror.staticModule("eu.stratosphere.emma.api.lara.Transformations")

    // matrix construction
    val m_apply               = matrixSymbol.companion.info.decl(TermName("apply")) // alternatives
    val m_fill                = matrixSymbol.companion.info.decl(TermName("fill"))
    val m_zeros               = matrixSymbol.companion.info.decl(TermName("zeros"))
    val m_ones                = matrixSymbol.companion.info.decl(TermName("ones"))
    val m_eye                 = matrixSymbol.companion.info.decl(TermName("eye"))
    val m_rand                = matrixSymbol.companion.info.decl(TermName("rand"))

    // vector construction
    val v_apply               = vectorSymbol.companion.info.decl(TermName("apply")) // alternatives
    val v_fill                = vectorSymbol.companion.info.decl(TermName("fill"))
    val v_zeros               = vectorSymbol.companion.info.decl(TermName("zeros"))
    val v_zerosLike           = vectorSymbol.companion.info.decl(TermName("zerosLike"))
    val v_ones                = vectorSymbol.companion.info.decl(TermName("ones"))
    val v_rand                = vectorSymbol.companion.info.decl(TermName("rand")) // alternatives

    // matrix transformations
    val m_toMatrix            = transSymbol.info.decl(TermName("toMatrix"))
    val m_vecToMatrix         = transSymbol.info.decl(TermName("vecToMatrix"))
    val m_indexedToMatrix     = transSymbol.info.decl(TermName("indexedToMatrix")) // alternatives

    // vector transformations
    val v_toVector            = transSymbol.info.decl(TermName("toVector"))
    val v_indexedToVector     = transSymbol.info.decl(TermName("indexedToVector")) // alternatives

    val toBag                 = transSymbol.info.decl(TermName("toBag"))
    val toIndexedBag          = transSymbol.info.decl(TermName("toIndexedBag"))

    // matrix methods
    val m_+                   = matrixSymbol.info.decl(TermName("+").encodedName) // alternatives
    val m_-                   = matrixSymbol.info.decl(TermName("-").encodedName) // alternatives
    val m_*                   = matrixSymbol.info.decl(TermName("*").encodedName) // alternatives
    val m_/                   = matrixSymbol.info.decl(TermName("/").encodedName) // alternatives
    val m_mult                = matrixSymbol.info.decl(TermName("%*%").encodedName) // alternatives
    val m_diag                = matrixSymbol.info.decl(TermName("diag"))
    val m_transpose           = matrixSymbol.info.decl(TermName("transpose"))
    val m_row                 = matrixSymbol.info.decl(TermName("row"))
    val m_column              = matrixSymbol.info.decl(TermName("column"))
    val m_rows                = matrixSymbol.info.decl(TermName("rows")) // alternatives
    val m_cols                = matrixSymbol.info.decl(TermName("cols")) // alternatives
    val m_elements            = matrixSymbol.info.decl(TermName("elements"))
    val m_map                 = matrixSymbol.info.decl(TermName("map"))

    // vector methods
    val v_+                   = vectorSymbol.info.decl(TermName("+").encodedName) // alternatives
    val v_-                   = vectorSymbol.info.decl(TermName("-").encodedName) // alternatives
    val v_*                   = vectorSymbol.info.decl(TermName("*").encodedName) // alternatives
    val v_/                   = vectorSymbol.info.decl(TermName("/").encodedName) // alternatives
    val v_dot                 = vectorSymbol.info.decl(TermName("dot"))
    val v_mult                = vectorSymbol.info.decl(TermName("%*%").encodedName) // alternatives
    val v_transpose           = vectorSymbol.info.decl(TermName("transpose"))
    val v_diag                = vectorSymbol.info.decl(TermName("diag"))
    val v_aggregate           = vectorSymbol.info.decl(TermName("aggregate"))
    val v_fold                = vectorSymbol.info.decl(TermName("fold"))
    val v_map                 = vectorSymbol.info.decl(TermName("map"))


    val methods = Set( // matrix methods
      m_diag, m_transpose, m_row, m_column, m_elements, m_map
    ) ++ m_+.alternatives ++ m_-.alternatives ++ m_*.alternatives ++ m_/.alternatives ++
      m_mult.alternatives ++ m_rows.alternatives ++ m_cols.alternatives ++ Set( // vector methods
      v_dot, v_transpose, v_diag, v_aggregate, v_fold, v_map
    ) ++ v_+.alternatives ++ v_-.alternatives ++ v_*.alternatives ++
      v_/.alternatives ++ v_mult.alternatives ++ Set( // matrix constructors
      m_fill, m_zeros, m_ones, m_eye, m_rand
    ) ++ m_apply.alternatives ++ Set( // vector constructors
      v_fill, v_zeros, v_zerosLike, v_ones, v_rand
    ) ++ v_apply.alternatives ++ Set( // Transformations
      m_toMatrix, m_vecToMatrix, v_toVector
    ) ++ m_indexedToMatrix.alternatives ++ v_indexedToVector.alternatives ++
      toBag.alternatives ++ toIndexedBag.alternatives

    val m_syntheticCtors = Set(m_fill, m_zeros, m_ones, m_eye, m_rand) ++ m_apply.alternatives

    // Type constructors
    val MATRIX                = typeOf[eu.stratosphere.emma.api.lara.DenseMatrix[Nothing]].typeConstructor
    val VECTOR                = typeOf[eu.stratosphere.emma.api.lara.DenseMatrix[Nothing]].typeConstructor
    //@formatter:on
  }

  /** Common validation helpers. */
  object Validation {

    val ok = ()
    val pass = Good(ok)

    type Valid = Unit
    type Invalid = Every[Error]
    type Verdict = Valid Or Invalid
    type Validator = Tree =?> Verdict

    def validateAs(expected: Validator, tree: Tree,
      violation: => String = "Unexpected tree"): Verdict = {

      expected.applyOrElse(tree, (unexpected: Tree) => {
        Bad(One(Error(unexpected, violation)))
      })
    }

    def oneOf(allowed: Validator*): Validator =
      allowed.reduceLeft(_ orElse _)

    case class Error(at: Tree, violation: String) {
      override def toString = s"$violation:\n${Tree show at}"
    }

    case class all(trees: Seq[Tree]) {
      case class are(expected: Validator) {
        def otherwise(violation: => String): Verdict =
          if (trees.isEmpty) pass
          else trees validatedBy expected.orElse {
            case unexpected => Bad(One(Error(unexpected, violation)))
          } map (_.head)
      }
    }

    object all {
      def apply(tree: Tree, trees: Tree*): all =
        apply(tree +: trees)
    }

    implicit class And(verdict: Verdict) {
      def and(other: Verdict): Verdict =
        withGood(verdict, other) { case _ => ok }
    }
  }
}
