package eu.stratosphere.emma
package compiler.lang.core

import compiler.Common
import compiler.lang.source.Source
import util.Monoids
import cats.std.all._
import shapeless._

import scala.collection.mutable

private[core] trait DML extends Common{
  this: Source with Core =>

  import Core.{Lang => core}
  import Source.{Lang => src}
  import UniverseImplicits._

  object DMLTransform {
    type D = Int => String // semantic domain (offset => string representation)
    val indent = 0

    /** contains all sources that have to be specified in the MLContext
      * with their names and the names in the dml script) */
    val sources: mutable.Set[(String, u.TermSymbol)] = mutable.Set.empty

    // map of all binding references to get the symbol for dataframes that are passed from outside the macro
    val bindingRefs: mutable.Map[String, u.TermSymbol] = mutable.Map.empty

    // the last seen lhs of a valdef
    var currLhs: u.TermSymbol = _

    val generateDML: u.Tree => String  = (tree: u.Tree) => {

      val ops = Set('=', '+', '-', '*', '/', '%', '<', '>', '&', '|', '!', '?', '^', '\\', '@', '#', '~')

      val matrixOps = Set("%*%")

      val constructors = Set("zeros", "rand")

      val builtins = Set("read", "write", "min", "max", "mean")

      val printSym = (sym: u.Symbol) => {
        val decName = sym.name.decodedName.toString.stripPrefix("unary_")
        val encName = sym.name.encodedName.toString.stripPrefix("unary_")

        decName
        // TODO we have to make sure to transform invalid references into valid ones or raise an exception
//        if (decName == encName && !kws.contains(decName) || sym.isMethod && decName.forall(ops.contains)) decName
//        else s"`$decName`"
      }

      val isConstructor = (sym: u.MethodSymbol) =>
        constructors.contains(sym.name.toString)

      val isMatrixOp = (sym: u.MethodSymbol) => {
        val s = sym.name.decodedName.toString
        matrixOps.contains(s)
      }

      val isBuiltin = (sym: u.MethodSymbol) => {
        val s = sym.name.decodedName.toString
        builtins.contains(s)
      }

      val isApply = (sym: u.MethodSymbol) =>
        sym.name == u.TermName("apply")

      val printMethod = (pre: String, sym: u.MethodSymbol, suf: String) =>
        if (isApply(sym)) ""
        else pre + printSym(sym) + suf

      val printConstructor = (sym: u.MethodSymbol, argss: Seq[Seq[D]], offset: Int) => {
        val dims = argss flatMap  (args => args map (arg => arg(offset)))

        val (rows, cols) = (dims(0), dims(1))

        sym.name match {
          case u.TermName("rand") => s"rand(rows=${rows}, cols=${cols})"
          case u.TermName("zeros") => s"matrix(0, rows=${rows}, cols=${cols})"
        }
      }

      val printMatrixOp = (target: D, sym: u.MethodSymbol, argss: Seq[Seq[D]], offset: Int) => {
        "A %*% B"
      }

      val printBuiltin = (target: D, sym: u.MethodSymbol, argss: Seq[Seq[D]], offset: Int) => {
        val args = argss flatMap (args => args map (arg => arg(offset)))

        sym.name match {
          case u.TermName("read") => {
            s"read(..${args})"
          }

          case u.TermName("write") => {
            val format = args(2) match {
              case "CSV" => """format="csv""""
              case _ => throw new RuntimeException(s"Unsopported output format: ${args(2)}")
            }
            s"write(${args(0)}, ${args(1)}, $format)"
          }

          case u.TermName(fname) => s"$fname(${args.mkString(", ")})"

          case _ =>
            abort(s"Unsopported builtin call: ${get.pos(sym)}", get.pos(sym))
        }
      }

      val isUnary = (sym: u.MethodSymbol) =>
        sym.name.decodedName.toString.startsWith("unary_")

      val printArgss = (argss: Seq[Seq[D]], offset: Int) =>
        (argss map (args => (args map (arg => arg(offset))).mkString("(", ", ", ")"))).mkString

      val printParams = (params: Seq[D], offset: Int) =>
        (params map (param => param(offset))).mkString("(", ", ", ")")

      val printParamss = (paramss: Seq[Seq[D]], offset: Int) =>
        (paramss map (params => printParams(params, offset))).mkString

      def printTpe(tpe: u.Type): String = {
        val tpeCons = tpe.typeConstructor
        val tpeArgs = tpe.typeArgs
        if (api.Sym.tuples contains tpeCons.typeSymbol) /* tuple type */ {
          (tpe.typeArgs map printTpe).mkString("(", ", ", ")")
        } else if (api.Sym.fun(Math.max(0, tpeArgs.size - 1)) == tpeCons.typeSymbol) /* function type */ {
          s"(${(tpe.typeArgs.init map printTpe).mkString(", ")}) => ${printTpe(tpe.typeArgs.last)}"
        } else if (tpeArgs.nonEmpty) /* applied higher-order type */ {
          s"${printSym(tpeCons.typeSymbol)}[${(tpe.typeArgs map printTpe).mkString(", ")}]"
        } else /* simple type */ {
          printSym(tpeCons.typeSymbol)
        }
      }

      val escape = (str: String) => str
        .replace("\b", "\\b")
        .replace("\n","\\n")
        .replace("\t", "\\t")
        .replace("\r","\\r")
        .replace("\f", "\\f")
        .replace("\"","\\\"")
        .replace("\\","\\\\")

      val alg = new Core.Algebra[D] {

        def empty: D = offset => ""
        (1.0, 1.0, 1.0)

        // Atomics
        def lit(value: Any): D = offset => value match {
          //@formatter:off
          case value: Char   => s""""${value}""""
          case value: String => s""""${escape(value)}""""
          case null          => "null"
          case value: Unit   => ""
          case _             => value.toString
          //@formatter:on
        }
        def this_(sym: u.Symbol): D = ???

        def bindingRef(sym: u.TermSymbol): D = offset => {
          bindingRefs.put(sym.name.decodedName.toString, sym)
          printSym(sym)
        }

        def moduleRef(target: u.ModuleSymbol): D = offset =>
          printSym(target)

        // Definitions
        def valDef(lhs: u.TermSymbol, rhs: D, flags: u.FlagSet): D = offset => {
          currLhs = lhs
          val rhsString = rhs(offset)
          // if we have a reference to a dataframe, we need to scratch it and put the reference as input to mlContext
          if (rhsString != "null")
            s"${printSym(lhs)} = $rhsString"
          else
            " "
        }

        def parDef(lhs: u.TermSymbol, rhs: D, flags: u.FlagSet): D = ???
        def defDef(sym: u.MethodSymbol, flags: u.FlagSet, tparams: S[u.TypeSymbol], paramss: SS[D], body: D): D = ???

        // Other
        def typeAscr(target: D, tpe: u.Type): D = ???

        def defCall(target: Option[D], method: u.MethodSymbol, targs: S[u.Type], argss: SS[D]): D = offset => {
          val s = target
          val args = argss flatMap (args => args map (arg => arg(offset)))
          (target, argss) match {
            case (Some(tgt), _) => {
              if (isConstructor(method))
                printConstructor(method, argss, offset)
              else if (isMatrixOp(method))
                printMatrixOp(tgt, method, argss, offset)
              else if (isBuiltin(method))
                printBuiltin(tgt, method, argss, offset)
                /** transform a call to fromDataFrame to a reference that will be set in MLContext */
              else if (method.name == u.TermName("fromDataFrame")) {
                // get the name of the referenced variable (dataframe)
                val in = bindingRefs.get(args.head) match {
                  case Some(termSym) => termSym
                  case _ =>
                    abort(s"Could not find reference to variable ${args.head} in Matrix.fromDataFrame", method.pos)
                }
                // memoize the pair of (lhs, reference name)
                sources.add((currLhs.name.decodedName.toString, in))
                // the original valdef is removed as references to the variable will be resolved from MLContext
                "null"
              }
              else
                " "
            }
//            case (Some(tgt), ((arg :: Nil) :: Nil)) if isApply(method) =>
//              s"${tgt(offset)}(${arg(offset)})"
//            case (Some(tgt), ((arg :: Nil) :: Nil)) =>
//              s"${tgt(offset)}${printMethod(" ", method, " ")}${arg(offset)}"
//            case (Some(tgt), Nil | (Nil :: Nil)) if targs.nonEmpty =>
//              s"${tgt(offset)}${printMethod(".", method, "")}[${(targs map printTpe).mkString}]"
//            case (Some(tgt), Nil) if isUnary(method) =>
//              s"${printSym(method)}${tgt(offset)}"
//            case (Some(tgt), _) if isApply(method) && api.Sym.tuples.contains(method.owner.companion) =>
//              s"${printArgss(argss, offset)}"
//            case (Some(tgt), _) =>    val testString = "print('hello world')"

//              s"${tgt(offset)}${printMethod(".", method, "")}${printArgss(argss, offset)}"
//            case (None, Nil | (Nil :: Nil)) if targs.nonEmpty =>
//              s"${printSym(method)}[${(targs map printTpe).mkString}]"
//            case (None, _) =>
//              s"${printSym(method)}${printArgss(argss, offset)}"
          }
        }
        def inst(target: u.Type, targs: Seq[u.Type], argss: SS[D]): D = ???
        def lambda(sym: u.TermSymbol, params: S[D], body: D): D = ???
        def branch(cond: D, thn: D, els: D): D = ???

        def let(vals: S[D], defs: S[D], expr: D): D = offset => {
          s"""{
              |${(vals map (val_ => (" " * (offset + indent)) + val_(offset + indent))).mkString("\n")}
              |${" " * (offset + indent)}${expr(offset + indent)}
              |${" " * offset}}
           """.stripMargin.trim

          val valsStr = (vals map (val_ => (" " * (offset + indent)) + val_(offset + indent))).mkString("\n")

          val defsStr = (defs map (def_ => (" " * (offset + indent)) + def_(offset + indent))).mkString("\n")
          Seq(
            valsStr,
            defsStr,
            " " * (offset + indent) + expr(offset + indent),
            " " * offset).filter(_.trim != "").mkString("\n")
        }

        // Comprehensions
        def comprehend(qs: S[D], hd: D): D = ???
        def generator(lhs: u.TermSymbol, rhs: D): D = ???
        def guard(expr: D): D = ???
        def head(expr: D): D = ???
        def flatten(expr: D): D = ???
      }
      Core.fold(alg)(tree)(0)
    }
  }
}
