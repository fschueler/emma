package org.apache.sysml.compiler

import org.apache.sysml.compiler.lang.source.DML
import org.emmalanguage.compiler.lang.source.Source
import org.emmalanguage.compiler.{Common, Compiler, RuntimeCompiler}

trait DMLCompiler extends Compiler with DML {

  override val preProcess: Seq[u.Tree => u.Tree] = Seq(
    fixLambdaTypes,
    stubTypeTrees,
    unQualifyStatics,
    normalizeStatements,
    Source.dmlNormalize
  )

  /** The identity transformation with pre- and post-processing. */
  override def identity(typeCheck: Boolean = false): u.Tree => u.Tree =
  pipeline(typeCheck)()

  /** applies pre and post-[rocessing specific to the DML generation */
  override def pipeline(typeCheck: Boolean = false, withPre: Boolean = true, withPost: Boolean = true)
                       (transformations: (u.Tree => u.Tree)*): u.Tree => u.Tree = {
    val bld = Seq.newBuilder[u.Tree => u.Tree]
    //@formatter:off
    if (typeCheck) bld += { api.Type.check(_) }
    if (withPre)   bld ++= preProcess
    bld ++= transformations
    if (withPost)  bld ++= postProcess
    //@formatter:on
    Function.chain(bld.result())
  }

  lazy val generateDML: u.Tree => String = unQualifyStatics andThen DMLTransform.generateDML
}
