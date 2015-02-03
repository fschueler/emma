package eu.stratosphere.emma.macros.program.comprehension

import eu.stratosphere.emma.macros.program.comprehension.rewrite.ComprehensionNormalization
import eu.stratosphere.emma.macros.program.util.ProgramUtils

import scala.collection.mutable
import eu.stratosphere.emma.macros.program.ContextHolder
import eu.stratosphere.emma.macros.program.controlflow.ControlFlowModel

import scala.reflect.macros._

private[emma] trait ComprehensionAnalysis[C <: blackbox.Context]
  extends ContextHolder[C]
  with ControlFlowModel[C]
  with ComprehensionModel[C]
  with ComprehensionNormalization[C]
  with ProgramUtils[C] {

  import c.universe._

  /**
   * A set of API method symbols to be comprehended.
   */
  protected object api {
    val moduleSymbol = rootMirror.staticModule("eu.stratosphere.emma.api.package")
    val bagSymbol = rootMirror.staticClass("eu.stratosphere.emma.api.DataBag")

    val apply = bagSymbol.companion.info.decl(TermName("apply"))
    val read = moduleSymbol.info.decl(TermName("read"))
    val write = moduleSymbol.info.decl(TermName("write"))
    val stateful = moduleSymbol.info.decl(TermName("stateful"))
    val fold = bagSymbol.info.decl(TermName("fold"))
    val map = bagSymbol.info.decl(TermName("map"))
    val flatMap = bagSymbol.info.decl(TermName("flatMap"))
    val withFilter = bagSymbol.info.decl(TermName("withFilter"))
    val groupBy = bagSymbol.info.decl(TermName("groupBy"))
    val minus = bagSymbol.info.decl(TermName("minus"))
    val plus = bagSymbol.info.decl(TermName("plus"))
    val distinct = bagSymbol.info.decl(TermName("distinct"))
    val minBy = bagSymbol.info.decl(TermName("minBy"))
    val maxBy = bagSymbol.info.decl(TermName("maxBy"))
    val min = bagSymbol.info.decl(TermName("min"))
    val max = bagSymbol.info.decl(TermName("max"))
    val sum = bagSymbol.info.decl(TermName("sum"))
    val product = bagSymbol.info.decl(TermName("product"))
    val count = bagSymbol.info.decl(TermName("count"))
    val exists = bagSymbol.info.decl(TermName("exists"))
    val forall = bagSymbol.info.decl(TermName("forall"))
    val empty = bagSymbol.info.decl(TermName("empty"))

    val methods = Set(
      read, write,
      stateful,
      fold,
      map, flatMap, withFilter,
      groupBy,
      minus, plus, distinct,
      minBy, maxBy, min, max, sum, product, count, exists, forall, empty
    ) ++ apply.alternatives

    val monadic = Set(map, flatMap, withFilter)
  }

  // --------------------------------------------------------------------------
  // Comprehension Store Constructor
  // --------------------------------------------------------------------------

  /**
   * Comprehension store constructor.
   *
   * @param cfGraph The control flow graph for the comprehended algorithm.
   */
  def createComprehensionView(cfGraph: CFGraph) = {

    // step #1: compute the set of maximal terms that can be translated to comprehension syntax
    val root = cfGraph.nodes.find(_.inDegree == 0).get
    // implicit helpers
    implicit def cfBlockTraverser: CFGraph#OuterNodeTraverser = root.outerNodeTraverser()

    // step #1: compute the set of maximal terms that can be translated to comprehension syntax
    val terms = (for (block <- cfBlockTraverser; stmt <- block.stats) yield {
      // find all value applications on methods from the comprehended API for this statement
      implicit var comprehendedTerms = mutable.Set(stmt.filter({
        case Apply(fun, _) if api.methods.contains(fun.symbol) => true
        case _ => false
      }): _*)

      // FIXME: the naming of the whole reduction procedure is misleading, rethink and reconsolidate
      // what happens here is that effectively we are looking for "inverse" links as compared to the traversal
      // order in the "comprehend" method; so effecively we are looking for "comprehended children", not "parents"
      // the two "do-while" loops can be probably merged into one routine "comprehendedChild" which reflects the
      // matching patterns leading to recursive calls in the "comprehend" method in a reversed way

      // reduce by removing obsolete terms
      var obsolete = mutable.Set.empty[Tree]
      // a) remove applies that will be comprehended with their parent selector
      do {
        comprehendedTerms = comprehendedTerms diff obsolete
        obsolete = for (t <- comprehendedTerms; p <- comprehendedSelectParent(t)) yield p
      } while (obsolete.nonEmpty)
      // b) remove applies that will be comprehended with their enclosing flatMap
      do {
        comprehendedTerms = comprehendedTerms diff obsolete
        obsolete = for (t <- comprehendedTerms; c <- comprehendedFlatMapBody(t)) yield c
      } while (obsolete.nonEmpty)

      // return the reduced set of applies
      comprehendedTerms
    }).flatten.toSet

    // step #2: create ComprehendedTerm entries for the identified terms
    val comprehendedTerms = mutable.Seq((for (t <- terms) yield {
      val id = TermName(c.freshName("comprehension"))
      val definition = comprehendedTermDefinition(t)
      val comprehension = normalize(ExpressionRoot(comprehend(Nil)(t) match {
        case root@combinator.Write(_, _, _) => root
        case root@combinator.Fold(_, _, _, _) => combinator.FoldSink(comprehendedTermName(definition, id), root)
        case root: Expression => combinator.TempSink(comprehendedTermName(definition, id), root)
      }))

      ComprehendedTerm(id, t, comprehension, definition)
    }).toSeq: _*)

    // step #3: build the comprehension store
    new ComprehensionView(comprehendedTerms)
  }

  /**
   * Checks whether the parent expression in a selector chain is also comprehended.
   *
   * @param t the expression to be checked
   * @param comprehendedTerms the set of comprehended terms
   * @return An option holding the comprehended parent term (if such exists).
   */
  private def comprehendedSelectParent(t: Tree)(implicit comprehendedTerms: mutable.Set[Tree]) = t match {
    // FIXME: make this consistent with the comprehend() method patterns
    case Apply(Apply(TypeApply(Select(_, _), _), _), List(parent)) if comprehendedTerms.contains(parent) =>
      Some(parent)
    case Apply(TypeApply(Select(_, _), _), List(parent)) if comprehendedTerms.contains(parent) =>
      Some(parent)
    case Apply(TypeApply(Select(parent, _), _), _) if comprehendedTerms.contains(parent) =>
      Some(parent)
    case Apply(Select(parent, _), _) if comprehendedTerms.contains(parent) =>
      Some(parent)
    case Apply(parent@Apply(_, _), _) if comprehendedTerms.contains(parent) =>
      Some(parent)
    case _ =>
      Option.empty[Tree]
  }

  /**
   * Checks whether the body of the function passed to a flatMap is a comprehension term.
   *
   * @param t the expression to be checked
   * @param comprehendedTerms the set of comprehended terms
   * @return An option holding the comprehended function body term (if such exists).
   */
  private def comprehendedFlatMapBody(t: Tree)(implicit comprehendedTerms: mutable.Set[Tree]) = t match {
    case Apply(fun, (f: Function) :: Nil) if fun.symbol == api.flatMap && api.monadic.contains(f.body.symbol) && comprehendedTerms.contains(f.body) =>
      Some(f.body)
    case _ =>
      Option.empty[Tree]
  }

  /**
   * Looks up a definition term (ValDef or Assign) for a comprehended term.
   *
   * @param t The term to lookup.
   * @return The (optional) definition for the term.
   */
  private def comprehendedTermDefinition(t: Tree)(implicit cfBlockTraverser: TraversableOnce[CFBlock]) = {
    var optTree = Option.empty[Tree]
    for (block <- cfBlockTraverser; s <- block.stats) s.foreach({
      case vd@ValDef(_, name: TermName, _, rhs) if t == rhs =>
        optTree = Some(vd)
      case vd@Assign(Ident(name: TermName), rhs) if t == rhs =>
        optTree = Some(vd)
      case _ =>
    })
    optTree
  }

  /**
   * Looks up a definition term (ValDef or Assign) for a comprehended term.
   *
   * @param t The term to lookup.
   * @return The (optional) definition for the term.
   */
  private def comprehendedTermName(t: Option[Tree], default: TermName) = t.getOrElse(default) match {
    case ValDef(_, name: TermName, _, rhs) => name
    case Assign(Ident(name: TermName), rhs) => name
    case _ => default
  }

  // --------------------------------------------------------------------------
  // Comprehension Constructor
  // --------------------------------------------------------------------------

  /**
   * Recursive comprehend method.
   *
   * @param vars The Variable environment for the currently lifted tree
   * @param tree The tree to be lifted
   * @return A lifted, MC syntax version of the given tree
   */
  private def comprehend(vars: List[Variable])(tree: Tree, input: Boolean = true): Expression = {

    // ignore a top-level Typed node (side-effect of the Algebra inlining macros)
    val t = tree match {
      case Typed(inner, _) => inner
      case _ => tree
    }

    // translate based on matched expression type
    t match {

      // -----------------------------------------------------
      // Monad Ops
      // -----------------------------------------------------

      // in.map(fn)
      case Apply(TypeApply(select@Select(in, _), List(tpt)), List(fn@Function(List(arg), body))) if select.symbol == api.map =>
        val v = Variable(arg.name, arg.tpt)

        val bind = Generator(arg.name, comprehend(vars)(in))
        val head = comprehend(v :: vars)(body, input = false)

        Comprehension(head, bind :: Nil)

      // in.flatMap(fn)
      case Apply(TypeApply(select@Select(in, _), List(tpt)), List(fn@Function(List(arg), body))) if select.symbol == api.flatMap =>
        val v = Variable(arg.name, arg.tpt)

        val bind = Generator(arg.name, comprehend(vars)(in))
        val head = comprehend(v :: vars)(body, input = false)

        MonadJoin(Comprehension(head, bind :: Nil))

      // in.withFilter(fn)
      case Apply(select@Select(in, _), List(fn@Function(List(arg), body))) if select.symbol == api.withFilter =>
        val v = Variable(arg.name, arg.tpt)

        val bind = Generator(arg.name, comprehend(vars)(in))
        val filter = Filter(comprehend(v :: vars)(body))
        val head = comprehend(v :: vars)(q"${arg.name}", input = false)

        Comprehension(head, bind :: filter :: Nil)

      // -----------------------------------------------------
      // Grouping and Set operations
      // -----------------------------------------------------

      // in.groupBy(k)
      case Apply(TypeApply(select@Select(in, _), List(tpt)), List(k@Function(List(arg), body))) if select.symbol == api.groupBy =>
        combinator.Group(k, comprehend(Nil)(in))

      // in.minus(subtrahend)
      case Apply(TypeApply(select@Select(in, _), List(_)), List(subtrahend)) if select.symbol == api.minus =>
        combinator.Diff(comprehend(Nil)(in), comprehend(Nil)(subtrahend))

      // in.plus(addend)
      case Apply(TypeApply(select@Select(in, _), List(_)), List(addend)) if select.symbol == api.plus =>
        combinator.Union(comprehend(Nil)(in), comprehend(Nil)(addend))

      // in.distinct()
      case Apply(select@Select(in, _), Nil) if select.symbol == api.distinct =>
        combinator.Distinct(comprehend(Nil)(in))

      // -----------------------------------------------------
      // Aggregates
      // -----------------------------------------------------

      // in.minBy()(n)
      case Apply(TypeApply(select@Select(in, _), List(tpt)), List(Function(List(x, y), body))) if select.symbol == api.minBy =>
        // replace the body of the fn to use 'u', 'v' parameters instead of the given arguments
        // FIXME: changes semantics if 'u' and 'v' are defined in the body
        val bodyNew = substitute(body, Map(x.name.toString -> Ident(TermName("u")), y.name.toString -> Ident(TermName("v"))))

        // quasiquote fold operators using the minBy parameter function
        val empty = c.typecheck(q"Option.empty[$tpt]")
        val sng = c.typecheck(q"(x: $tpt) => Some(x)")
        val union = c.typecheck( q"""(x: Option[$tpt], y: Option[$tpt]) =>
            if (x.isEmpty && y.isDefined) y
            else if (x.isDefined && y.isEmpty) x
            else for (u <- x; v <- y) yield if ($bodyNew) u else v
        """)

        combinator.Fold(empty, sng, union, comprehend(Nil)(in))

      // in.maxBy()(n)
      case Apply(TypeApply(select@Select(in, _), List(tpt)), List(Function(List(x, y), body))) if select.symbol == api.maxBy =>
        // replace the body of the fn to use 'u', 'v' parameters instead of the given arguments
        // FIXME: changes semantics if 'u' and 'v' are defined in the body
        val bodyNew = substitute(body, Map(x.name.toString -> Ident(TermName("u")), y.name.toString -> Ident(TermName("v"))))

        // quasiquote fold operators using the minBy parameter function
        val empty = c.typecheck(q"Option.empty[$tpt]")
        val sng = c.typecheck(q"(x: $tpt) => Some(x)")
        val union = c.typecheck( q"""(x: Option[$tpt], y: Option[$tpt]) =>
          if (x.isEmpty && y.isDefined) y
          else if (x.isDefined && y.isEmpty) x
          else for (u <- x; v <- y) yield if ($bodyNew) v else u
        """)

        combinator.Fold(empty, sng, union, comprehend(Nil)(in))

      // in.min()(n)
      case Apply(Apply(TypeApply(select@Select(in, _), List(tpt)), Nil), n :: l :: Nil) if select.symbol == api.min =>
        combinator.Fold(c.typecheck(q"$l.max"), c.typecheck(q"(x: $tpt) => x"), c.typecheck(q"(x: $tpt, y: $tpt) => $n.min(x, y)"), comprehend(Nil)(in))

      // in.max()(n)
      case Apply(Apply(TypeApply(select@Select(in, _), List(tpt)), Nil), n :: l :: Nil) if select.symbol == api.max =>
        combinator.Fold(c.typecheck(q"$l.min"), c.typecheck(q"(x: $tpt) => x"), c.typecheck(q"(x: $tpt, y: $tpt) => $n.max(x, y)"), comprehend(Nil)(in))

      // in.sum()(n)
      case Apply(Apply(TypeApply(select@Select(in, _), List(tpt)), Nil), n :: Nil) if select.symbol == api.sum =>
        combinator.Fold(c.typecheck(q"$n.zero"), c.typecheck(q"(x: $tpt) => x"), c.typecheck(q"(x: $tpt, y: $tpt) => $n.plus(x, y)"), comprehend(Nil)(in))

      // in.product()(n)
      case Apply(Apply(TypeApply(select@Select(in, _), List(tpt)), Nil), n :: Nil) if select.symbol == api.product =>
        combinator.Fold(c.typecheck(q"$n.one"), c.typecheck(q"(x: $tpt) => x"), c.typecheck(q"(x: $tpt, y: $tpt) => $n.times(x, y)"), comprehend(Nil)(in))

      // in.count()(n)
      case Apply(select@Select(in, _), Nil) if select.symbol == api.count =>
        val comprehendedIn = comprehend(Nil)(in)
        combinator.Fold(c.typecheck(q"0L"), c.typecheck(q"(x: ${comprehendedIn.tpe}) => 1L"), c.typecheck(q"(x: Long, y: Long) => x + y"), comprehendedIn)

      // in.exists()(n)
      case Apply(select@Select(in, _), List(fn@Function(List(arg), body))) if select.symbol == api.exists =>
        val comprehendedIn = comprehend(Nil)(in)
        combinator.Fold(c.typecheck(q"false"), c.typecheck(q"(x: ${comprehendedIn.tpe}) => ${substitute(body, arg.name, q"x")}"), c.typecheck(q"(x: Boolean, y: Boolean) => x || y"), comprehendedIn)

      // in.forall()(n)
      case Apply(select@Select(in, _), List(fn@Function(List(arg), body))) if select.symbol == api.forall =>
        val comprehendedIn = comprehend(Nil)(in)
        combinator.Fold(c.typecheck(q"false"), c.typecheck(q"(x: ${comprehendedIn.tpe}) => ${substitute(body, arg.name, q"x")}"), c.typecheck(q"(x: Boolean, y: Boolean) => x && y"), comprehendedIn)

      // in.empty()(n)
      case Apply(select@Select(in, _), Nil) if select.symbol == api.empty =>
        val comprehendedIn = comprehend(Nil)(in)
        combinator.Fold(c.typecheck(q"false"), c.typecheck(q"(x: ${comprehendedIn.tpe}) => true"), c.typecheck(q"(x: Boolean, y: Boolean) => x || y"), comprehendedIn)

      // ----------------------------------------------------------------------
      // Environment & Host Language Connectors
      // ----------------------------------------------------------------------

      // write[T](location, ofmt)(in)
      case Apply(Apply(TypeApply(method, List(_)), location :: ofmt :: Nil), List(in: Tree)) if method.symbol == api.write =>
        combinator.Write(location, ofmt, comprehend(vars)(in))

      // read[T](location, ifmt)
      case Apply(TypeApply(method, List(tpt)), location :: ifmt :: Nil) if method.symbol == api.read =>
        combinator.Read(location, ifmt)

      // temp result identifier
      case ident@Ident(TermName(_)) if input =>
        combinator.TempSource(ident)

      // interpret as black box Scala expression (default)
      case _ =>
        ScalaExpr(vars, typechecked(vars, t)) // trees created by the caller with q"..." have to be explicitly typechecked
    }
  }

  // --------------------------------------------------------------------------
  // Logical Optimizations
  // --------------------------------------------------------------------------

  /**
   * Inlines comprehended ValDefs occurring only once with their parents.
   *
   * @param tree The original program tree.
   * @param cfGraph The control flow graph for the comprehended algorithm.
   * @param comprehensionView A view over the comprehended terms in the tree.
   * @return An inlined inlined tree.
   */
  def inlineComprehensions(tree: Tree)(implicit cfGraph: CFGraph, comprehensionView: ComprehensionView) = {

    var inlinedTree = tree

    // find all valdefs that can be inlined
    var valdefs = (for (cv <- comprehensionView.terms; d <- cv.definition) yield cv.definition.collect({
      // make sure that the associated definition is a non-mutable ValDef
      case valdef@ValDef(mods, name: TermName, _, rhs) if (mods.flags | Flag.MUTABLE) != mods.flags =>
        // get the ValDef symbol
        val symbol = d.symbol

        // get the identifiers referencing this ValDef symbol
        val idents = tree.collect({
          case x@Ident(_) if x.symbol == symbol => x
        })

        // if the symbol is referenced only once, inline the ValDef rhs in place of the ident
        if (idents.size == 1) Some(valdef) else Option.empty[ValDef]
    })).flatten.flatten

    while (valdefs.nonEmpty) {
      // get a ValDef to inline
      val valdef = valdefs.head
      // inline current ValDef in the tree
      inlinedTree = inline(inlinedTree, valdef)
      // inline in all other ValDefs and continue with those
      valdefs = for (other <- valdefs.filter(_.symbol != valdef.symbol)) yield inline(other, valdef).asInstanceOf[ValDef]
    }

    c.typecheck(inlinedTree)
  }
}
