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

import org.apache.spark.sql.DataFrame
import org.apache.sysml.api.linalg.api.:::

import scala.util.Random

// TODO: sparsity
// TODO: make matrix generic? what are possible value types?

/**
  * Matrix class for SystemML
  *
  * Represents the matrix that will be translated to SystemML's matrix type.
  *
  * @param impl the underlying breeze matrix to support numerical computations in Scala
  * @param rows number of rows of the matrix
  * @param cols number of columns of the matrix
  */
class Matrix private(val impl: Array[Double], val rows: Int, val cols: Int) {

  //////////////////////////////////////////
  // Constructors
  //////////////////////////////////////////


  //////////////////////////////////////////
  // Accessors
  //////////////////////////////////////////

  def apply(row: Int, col: Int): Double = ???

  def apply(row: Int, col: :::.type ): Vector = ???

  def apply(row: :::.type, col: Int): Vector = ???

  def apply(rows: Range.Inclusive, cols: :::.type): Matrix = ???

  def apply(rows: :::.type, cols: Range.Inclusive): Matrix = ???

  def apply(rows: Range.Inclusive, cols: Range.Inclusive): Matrix = ???

  //////////////////////////////////////////
  // Left Indexing assignments
  //////////////////////////////////////////

  // TODO make sure that the orientation of the vector (row/col) fits the assignment
  def update(row: Int, col: Int, value: Double): Matrix = ???

  def update(row: Int, col: :::.type, vec: Vector): Matrix = ???

  def update(row: :::.type, col: Int, vec: Vector): Matrix = ???

  def update(rows: Range.Inclusive, cols: :::.type, mat: Matrix): Matrix = ???

  def update(rows: :::.type, cols: Range.Inclusive, mat: Matrix): Matrix = ???

  def update(rows: Range.Inclusive, cols: Range.Inclusive, mat: Matrix): Matrix = ???

  //////////////////////////////////////////
  // M o scalar
  //////////////////////////////////////////

  def +(that: Double): Matrix = ???

  def -(that: Double): Matrix = ???

  def *(that: Double): Matrix = ???

  def /(that: Double): Matrix = ???

  //////////////////////////////////////////
  // columnwise M o vector (broadcast operators)
  //////////////////////////////////////////

  private def broadcastRows(mat: Matrix, vec: Vector, op: (Double, Double) => Double) = ???

  private def broadcastCols(mat: Matrix, vec: Vector, op: (Double, Double) => Double) = ???

  private def broadcast(mat: Matrix, vec: Vector)(op: (Double, Double) => Double) = ???

  def +(that: Vector): Matrix = broadcast(this, that)(_ + _)

  def -(that: Vector): Matrix = broadcast(this, that)(_ - _)

  def *(that: Vector): Matrix = broadcast(this, that)(_ * _)

  def /(that: Vector): Matrix = broadcast(this, that)(_ / _)

  //////////////////////////////////////////
  // cellwise M o M
  //////////////////////////////////////////

  def +(that: Matrix): Matrix = ???

  def -(that: Matrix): Matrix = ???

  def *(that: Matrix): Matrix = ???

  def /(that: Matrix): Matrix = ???

  //////////////////////////////////////////
  // M x M -> M and  M x V -> V
  //////////////////////////////////////////

  def %*%(that: Matrix): Matrix = ???

  def %*%(that: Vector): Vector = ???

  //////////////////////////////////////////
  // M operation
  //////////////////////////////////////////

  def t: Matrix = ???

  def ^(n: Int): Matrix = ???

  def map(f: Double => Double): Matrix = ???

  // TODO: Should this return Either[Vector, Matrix] depending on if one dimension is 1?
  /**
    * Reshapes the [[Matrix]] into a new format. cols * rows must equal the original number of elements.
    *
    * @param rows number of rows of the new matrix
    * @param cols number of columns of the new matrix
    * @param byRow if true, matrix is reshaped my row
    * @return new matrix with the new dimensions and rearranged values
    */
  def reshape(rows: Int, cols: Int, byRow: Boolean = true): Matrix = ???

  def copy: Matrix = ???
}

object Matrix {

  /**
    * This should be the primary way of constructing a [[Matrix]] from a sequence of values.
    * The [[Matrix]] is constructed column-major order, i.e. the [[Array]] (1, 2, 1, 2) with dimensions (2,2) will
    * generate the [[Matrix]]
    *   1 1
    *   2 2
    * @param impl the values that will be assignmed to the cells of the matrix in column-major order
    * @param rows number of rows of the generated matrix
    * @param cols number of columns of the generated matrix
    * @return a [[Matrix]] with values as cell entries and dimensionality (rows, cols)
    */
  def apply(impl: Array[Double], rows: Int, cols: Int): Matrix = new Matrix(impl, rows, cols)

  def apply(values: Seq[Double], rows: Int, cols: Int): Matrix = apply(values.toArray, rows, cols)

  def fromDataFrame(df: DataFrame): Matrix = ???

//  private[sysml] def fill(rows: Int, cols: Int)(gen: (Int, Int) => Double): Matrix = {
//    require(rows * cols < Int.MaxValue)
//    val array = new Array[Double](rows * cols)
//    for (i <- 0 until rows; j <- 0 until cols) {
//      array((i * cols) + j) = gen(i, j)
//    }
//    new Matrix(array, rows, cols)
//  }

  def zeros(rows: Int, cols: Int): Matrix = ??? // Matrix.fill(rows, cols)((i, j) => 0.0)

  // TODO: support more parameters (min, max, distribution, sparsity, seed)
  def rand(rows: Int, cols: Int): Matrix = ??? //Matrix.fill(rows, cols)((i, j) => Random.nextDouble())

  /** generate matrix with the vector on the diagonal */
  def diag(vec: Vector): Matrix = ??? //Matrix.fill(vec.length, vec.length)((i, j) => if (i == j) vec(i) else 0.0)

}

