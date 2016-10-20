/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sysml.api.linalg

import scala.collection.immutable.NumericRange
import scala.util.Random

// TODO: sparsity
// TODO: make vector generic? what are possible value types?

/**
  * Vector class for SystemMl
  *
  * This is just a convenience alias for a [[Matrix]] with one of the dimensions equal to one.
  * When initializing a vector we assume that it is a column-vector. A vector of length n will therefore be treated as
  * a n x 1 matrix.
  *
  * @param impl the underlying breeze vector
  * @param isTransposed indicator if the vector is a row- or column vector. Default is column
  */
class Vector private(val impl: Array[Double], val isTransposed: Boolean = false){

  def length = impl.length

  def t: Vector = ???

  def update(idx: Int, value: Double): Vector = ???

  def update(idx: Range.Inclusive, value: Vector): Vector = ???

  def apply(idx: Int): Double = ???

  def apply(idx: Range.Inclusive) = ???

  //////////////////////////////////////////
  // V o scalar
  //////////////////////////////////////////

  def +(that: Double): Vector = ???

  def -(that: Double): Vector = ???

  def *(that: Double): Vector = ???

  def /(that: Double): Vector = ???

  //////////////////////////////////////////
  // V o V
  //////////////////////////////////////////

  def +(that: Vector): Vector = ???

  def -(that: Vector): Vector = ???

  def *(that: Vector): Vector = ???

  def /(that: Vector): Vector = ???

  //////////////////////////////////////////
  // V o Matrix
  //////////////////////////////////////////

  def %*%(that: Matrix): Vector = ???

}

/**
  * [[Vector]] Factory Constructors
  */
object Vector {

  def apply(values: Array[Double]): Vector = new Vector(values)

  def apply(values: Seq[Double]): Vector = new Vector(values.toArray)

  def apply(range: NumericRange[Double]): Vector = new Vector(range.toArray)

  def apply(range: Range.Partial[Double, NumericRange[Double]]): Vector = apply(range.by(1.0))

  def fill(length: Int)(gen: Int => Double): Vector = {
    require(length < Int.MaxValue)
    val array = new Array[Double](length)
    for (i <- 0 until length) {
      array(i) = gen(i)
    }
    new Vector(array)
  }

  // TODO: support more parameters (min, max, distribution, sparsity, seed)
  def rand(length: Int): Vector = ???

  def ones(length: Int): Vector = ???

  def zeros(length: Int): Vector = ???
}
