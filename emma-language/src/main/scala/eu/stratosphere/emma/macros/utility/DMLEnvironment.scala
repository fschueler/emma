package eu.stratosphere.emma.macros.utility

import eu.stratosphere.emma.compiler.MacroCompiler


class DMLEnvironment {
  /** variables that are referenced in the macro and have to be passed as input to MLContext */
  case class MLContextInput(varName: String, refName: String)

  val inputs = scala.collection.mutable.Set[MLContextInput]().empty
}
