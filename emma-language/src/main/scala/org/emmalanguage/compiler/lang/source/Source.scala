/*
 * Copyright © 2014 TU Berlin (emma@dima.tu-berlin.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.emmalanguage
package compiler.lang.source

import compiler.Common


/** Source language. */
trait Source extends Common
  with Foreach2Loop
  with PatternMatching
  with SourceValidate {

  // -------------------------------------------------------------------------
  // Source language
  // -------------------------------------------------------------------------

  /** Source language. */
  object Source {

    /**
      * The grammar associated with the [[Lang]] objects and accepted by the [[Source.valid]] method
      * is as follows.
      *
      * {{{
      * Sym = TermSym
      *     | TypeSym
      *
      * TermSym = BindingSym
      *         | MethodSym
      *         | ModuleSym
      *
      * BindingSym = ParSym
      *            | ValSym
      *            | VarSym
      *
      * Atomic = Lit[A](value: A)
      *        | Ref(target: TermSym)
      *        | This(target: Sym)
      *
      * Ref = BindingRef(target: BindingSym)
      *     | ModuleRef(target: ModuleSym)
      *
      * BindingRef = ParRef(target: ParSym)
      *            | ValRef(target: ValSym)
      *            | VarRef(target: VarSym)
      *
      * BindingDef = ParDef(lhs: ParSym)
      *            | ValDef(lhs: ValSym, rhs: Term)
      *            | VarDef(lhs: VarSym, rhs: Term)
      *
      * Term = Atomic
      *      | Block(stats: Stat*, expr: Term)
      *      | Branch(cond: Term, thn: Term, els: Term)
      *      | DefCall(target: Term?, method: MethodSym, targs: Type*, argss: Term**)
      *      | Inst(clazz: Type, targs: Type*, argss: Term**)
      *      | Lambda(params: ParDef*, body: Term)
      *      | ModuleAcc(target: Term, member: ModuleSym)
      *      | PatMat(target: Term, cases: Case*)
      *      | TypeAscr(expr: Term, tpe: Type)
      *
      * Loop = DoWhile(cond: Term, body: Stat)
      *      | While(cond: Term, body: Stat)
      *
      * Stat = BindingDef(lhs: BindingSym, rhs: Term)
      *      | Loop(cond: Term, body: Stat)
      *      | VarMut(lhs: VarSym, rhs: Term)
      *      | Term
      *
      * Case = Case(pat: Pat, guard: Term, body: Term)
      *
      * Pat = PatAlt(alternatives: Pat*)
      *     | PatAny
      *     | PatAscr(target: Pat, tpe: Type)
      *     | PatAt(lhs: ValSym, rhs: Pat)
      *     | PatConst(target: TermSym)
      *     | PatLit[A](value: A)
      *     | PatExtr(extr: PatExtr', args: Pat*)
      *     | PatQual(qual: PatQual', member: TermSym)
      *     | PatVar(lhs: ValSym)
      *
      * PatQual' = PatConst(target: TermSym)
      *          | PatQual(qual: PatQual', member: TermSym)
      *
      * PatExtr' = TypeTree(tpe: Type)
      *          | UnApply(qual: PatQual', unApp: MethodSym)
      * }}}
      */
    object Lang {
      //@formatter:off

      // Empty
      val Empty = api.Empty

      // Atomics
      val Atomic = api.Atomic
      val Lit = api.Lit
      val Ref = api.TermRef
      val This = api.This

      // Bindings
      val BindingRef = api.BindingRef
      val BindingDef = api.BindingDef

      // Parameters
      val ParRef = api.ParRef
      val ParDef = api.ParDef

      // Values
      val ValRef = api.ValRef
      val ValDef = api.ValDef

      // Variables
      val VarRef = api.VarRef
      val VarDef = api.VarDef
      val VarMut = api.VarMut

      // Modules
      val ModuleRef = api.ModuleRef
      val ModuleAcc = api.ModuleAcc

      // Methods
      val DefCall = api.DefCall

      // Loops
      val Loop = api.Loop
      val While = api.While
      val DoWhile = api.DoWhile

      // Patterns
      val Pat = api.Pat
      val PatCase = api.PatCase
      val PatMat = api.PatMat

      // Terms
      val Term = api.Term
      val Block = api.Block
      val Branch = api.Branch
      val Inst = api.Inst
      val Lambda = api.Lambda
      val TypeAscr = api.TypeAscr

      //@formatter:on
    }

    abstract class Algebra[A] {

      type S[X] = Seq[X]
      type SS[X] = Seq[Seq[X]]

      def empty: A

      // Atomics
      def lit(value: Any): A
      def this_(sym: u.Symbol): A
      def bindingRef(sym: u.TermSymbol): A
      def moduleRef(target: u.ModuleSymbol): A

      // Definitions
      def valDef(lhs: u.TermSymbol, rhs: A, flags: u.FlagSet): A
      def parDef(lhs: u.TermSymbol, rhs: A, flags: u.FlagSet): A
      def defDef(sym: u.MethodSymbol, flags: u.FlagSet, tparams: S[u.TypeSymbol], paramss: SS[A], body: A): A

      // Other
      def typeAscr(target: A, tpe: u.Type): A
      def defCall(target: Option[A], method: u.MethodSymbol, targs: S[u.Type], argss: SS[A]): A
      def forLoop(target: A, targs: S[u.Type], argss: S[A]): A // this is a speciall defcall where method is foreach
      def inst(target: u.Type, targs: Seq[u.Type], argss: SS[A]): A
      def lambda(sym: u.TermSymbol, params: S[A], body: A): A
      def branch(cond: A, thn: A, els: A): A
      def block(stats: S[A], expr: A): A

      def loop(cond: A, body: A): A
      def varMut(lhs: u.TermSymbol, rhs: A): A
    }

    def fold[B](a: Algebra[B])(tree: u.Tree): B = {

      def fold(tree: u.Tree): B = {
        tree match {

          // Empty
          case Lang.Empty(_) =>
            a.empty
          // Atomics
          case Lang.Lit(value) =>
            a.lit(value)
          case Lang.This(sym) =>
            a.this_(sym)
          case Lang.ModuleRef(target) =>
            a.moduleRef(target)
          case Lang.BindingRef(sym) =>
            a.bindingRef(sym)

          // Definitions
          case Lang.ValDef(lhs, rhs, flags) =>
            a.valDef(lhs, fold(rhs), flags)
          case Lang.VarDef(lhs, rhs, flags) =>
            a.valDef(lhs, fold(rhs), flags)
          case Lang.ParDef(lhs, rhs, flags) =>
            a.parDef(lhs, fold(rhs), flags)


          // Other
          case Lang.TypeAscr(target, tpe) =>
            a.typeAscr(fold(target), tpe)
          case Lang.DefCall(Some(xs), method, targs, args@Seq(Lang.Lambda(_, Seq(_), body)))
            if method == api.Sym.foreach || method.overrides.contains(api.Sym.foreach) =>
            a.forLoop(fold(xs), targs, args.map(fold(_)))
          case Lang.DefCall(target, method, targs, argss@_*) =>
            a.defCall(target map fold, method, targs, argss map (_ map fold))
          case Lang.Inst(target, targs, argss@_*) =>
            a.inst(target, targs, argss map (_ map fold))
          case Lang.Lambda(sym, params, body) =>
            a.lambda(sym, params map fold, fold(body))
          case Lang.Branch(cond, thn, els) =>
            a.branch(fold(cond), fold(thn), fold(els))
          case Lang.Block(stats, expr) =>
            a.block(stats.map(fold(_)), fold(expr))
          case Lang.Loop(cond, body) =>
            a.loop(fold(cond), fold(body))
          case Lang.VarMut(lhs, rhs) =>
            a.varMut(lhs, fold(rhs))
        }
      }
      fold(tree)
    }

    // -------------------------------------------------------------------------
    // Validation API
    // -------------------------------------------------------------------------

    /** Delegates to [[SourceValidate.valid]]. */
    lazy val valid = SourceValidate.valid

    /** Delegates to [[SourceValidate.valid]]. */
    lazy val validate = (tree: u.Tree) => valid(tree).isGood

    // -------------------------------------------------------------------------
    // Source -> Source transformations API
    // -------------------------------------------------------------------------

    /**
      * Applies the following chain of transformations in order to bring the
      * Source tree into a regular form.
      *
      * - [[PatternMatching.destruct]]
      * - [[Foreach2Loop.transform]]
      */
    lazy val normalize = {
      PatternMatching.destruct
    } andThen {
      Foreach2Loop.transform
    } andThen {
      removeImplicits(API.implicitTypes)
    }

    lazy val dmlNormalize = {
      PatternMatching.destruct
    } andThen {
      removeImplicits(API.implicitTypes)
    }

    /** Removes implicit lists consisting of the following symbols. */
    def removeImplicits(types: Set[u.Type]): u.Tree => u.Tree = {

      object Matching {

        def matchingImplicits(args: List[u.Tree], params: List[u.Symbol]): Boolean = {
          // args binds to implicit parameter lsit
          lazy val isImplicit = params.forall(is(u.Flag.IMPLICIT))
          // args exclusively from the given `types` list
          lazy val isMatching = args.forall(arg => types.exists(_ =:= arg.tpe.typeConstructor))
          args.size == params.size && args.nonEmpty && isImplicit && isMatching
        }

        def unapply(tree: u.Tree): Option[u.Tree] = tree match {
          case api.DefCall(target, method, targs, argss@_*) =>
            argss.map(_.toList).toList zip method.paramLists match {
              case _ :+ Tuple2(args, params) if matchingImplicits(args, params) => Some(tree)
              case _ => None
            }
          case _ => None
        }
      }

      api.TopDown.transform {
        case Matching(api.DefCall(target, method, targs, argss@_*)) =>
          api.DefCall(target)(method, targs: _*)(argss.init: _*)
      }.andThen(_.tree)
    }
  }
}
