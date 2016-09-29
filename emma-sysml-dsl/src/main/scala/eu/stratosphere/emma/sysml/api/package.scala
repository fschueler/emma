package eu.stratosphere.emma.sysml

import breeze.linalg.{Matrix => _, Vector => _, _}
import eu.stratosphere.emma.api.SystemMLAlgorithm

import scala.language.experimental.macros
import eu.stratosphere.emma.macros.program.RewriteMacros
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.sysml.api.MLContext

package object api {

  private val conf = new SparkConf()
    .setMaster("local[2]")
    .setAppName("SystemML Spark App")

  private val sc: SparkContext = new SparkContext(conf)

  implicit lazy val mlctx: MLContext = new MLContext(sc)

  /**
    * The entry point for the systemML macro
    */

  final def parallelize[T](e: T): SystemMLAlgorithm[T] = macro RewriteMacros.impl[T]

  object :::

  def read(path: String): Matrix = ???

  def write(mat: Matrix, path: String, format: Format.FileFormat): Unit = ???

  def sum(mat: Matrix): Double = breeze.linalg.sum(mat.impl)

  def sum(vec: Vector): Double = breeze.linalg.sum(vec.impl)

  def rowSums(mat: Matrix): Vector = {
    val s: DenseVector[Double] = breeze.linalg.sum(mat.impl(*, ::))
    Vector(s, t = false)
  }

  def colSums(mat: Matrix): Vector = {
    val s: DenseVector[Double] = breeze.linalg.sum(mat.impl(::, *)).inner
    Vector(s, t = true)
  }

  def mean(mat: Matrix): Double = ???

  def rowMeans(mat: Matrix): Vector = ???

  def colMeans(mat: Matrix): Vector = {
    val v: breeze.linalg.DenseVector[Double] = breeze.stats.mean(mat.impl(::, *)).inner
    Vector(v, t = true)
  }

  def log(x: Double): Double = breeze.numerics.log(x)

  def log(mat: Matrix): Matrix = Matrix(breeze.numerics.log(mat.impl))

  def abs(x: Double): Double = breeze.numerics.abs(x)

  def exp(b: Vector): Vector = Vector(breeze.numerics.exp(b.impl))

  def rowIndexMax(mat: Matrix): Vector = Vector(breeze.linalg.argmax(mat.impl(::, *)).inner.map(_.toDouble))

  def pmax(mat: Matrix, s: Double): Matrix = Matrix(mat.impl.map(x => if (x > s) x else s))

  ///////////////////////////////////
  // Implicit Matrix and Vector Ops
  ///////////////////////////////////

  /** This allows operations with Vectors and Matrices as left arguments such as Double * Matrix */
  implicit class VectorOps(private val n: Double) extends AnyVal {
    def +(v: Vector): Vector = v + n

    def -(v: Vector): Vector = v - n

    def *(v: Vector): Vector = v * n

    def /(v: Vector): Vector = v / n
  }

  implicit class MatrixOps(private val n: Double) extends AnyVal{
    def +(v: Matrix): Matrix = v + n

    def -(v: Matrix): Matrix = v - n

    def *(v: Matrix): Matrix = v * n

    def /(v: Matrix): Matrix = v / n
  }

  object Format {
    sealed trait FileFormat
    case object CSV extends FileFormat
    case object BINARY extends FileFormat
  }
}
