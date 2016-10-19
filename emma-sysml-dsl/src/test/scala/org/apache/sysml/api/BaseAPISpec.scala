package org.apache.sysml.api

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FreeSpec, Matchers}

/**
  * Specification for the SystemML API
  *
  * This specification follows the DML Language reference from https://apache.github.io/incubator-systemml/dml-language-reference
  * The goal is to support all Types, Expressions, and Statements that SystemML offers.
  *
  * This is not the specification for the supported Scala source language!
  */
@RunWith(classOf[JUnitRunner])
trait BaseAPISpec extends FreeSpec with Matchers {

}
