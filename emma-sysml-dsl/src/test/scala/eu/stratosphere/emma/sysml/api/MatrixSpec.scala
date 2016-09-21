package eu.stratosphere.emma.sysml.api

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
  * Specification for the Matrix abstraction and supported operations
  *
  */
@RunWith(classOf[JUnitRunner])
class MatrixSpec extends BaseAPISpec {

  /**
    * Data Types in SystemML include frame, matrix, and scalar
    * Value Types include double, integer, string, and boolean
    *
    * Matrices are only of type double. This is the specification for matrix construction similar to the `matrix()`
    * builtin function in SystemML
    * */
  "A Matrix" - {

    ///////////////////////////
    // Constructors
    ///////////////////////////

    "can be constructed" - {

      "with zeros" is pending
//      {
//        val B = Matrix.zeros(10, 10)
//
//        assert(B.impl === new DenseMatrix[Double](10, 10, Array.fill[Double](10*10)(0.0)))
//      }

      "with rand" is pending
//      {
//        val A = Matrix.rand(10, 10)
//      }

      "from reshaping an existing matrix" is pending
//      in {
//        val A = Matrix.zeros(10, 10)
//        val B = A.reshape(100, 1)
//      }

      "from a sequence or an array" in {
        val A = Matrix(Seq(4.0, 3.0, 2.0, 5.0, 7.0, 8.0), 3, 2)
        val B = Matrix(Array(4.0, 3.0, 2.0, 5.0, 7.0, 8.0), 3, 2)

        assert(A.impl === B.impl)
      }
    }

    ///////////////////////////
    // Accessors `apply()`
    ///////////////////////////

    "can be accessed" - {
      /* A =
          0 3 6
          1 4 7
          2 5 8
       */
      val A = Matrix(Seq(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0), 3, 3)

      "by element" in {
        val s = A(3, 3)

        assert(s === 9.0)
      }

      "by single row" in {
        val r = A(1, :::)
        val exp = Vector(Seq(1.0, 4.0, 7.0)).t

        assert(r.impl === exp.impl)
      }

      "by single column" in {
        val c = A(:::, 1)
        val exp = Vector(Seq(3.0, 4.0, 5.0))

        assert(c.impl === exp.impl)
      }

      "by multiple rows" in {
        val m = A(0 to 1, :::)
        val exp = Matrix(Seq(0.0, 1.0, 2.0, 3.0, 4.0, 5.0), 2, 3)

        assert(m.impl === exp.impl)
      }

      "by multiple columns" in {
        val m = A(:::, 0 to 1)
        val exp = Matrix(Seq(0.0, 1.0, 2.0, 3.0, 4.0, 5.0), 3, 2)

        assert(m.impl === exp.impl)
      }

      "by arbitrary submatrices" in {
        val m = A(0 to 1, 0 to 1)
        val exp = Matrix(Seq(0.0, 1.0, 2.0, 3.0), 2, 2)

        assert(m.impl == exp.impl)
      }
    }

    ///////////////////////////
    // Left-indexing `update()`
    ///////////////////////////

    "can be left-indexed" - {

      "by element" in {
        val A = Matrix.zeros(3, 3)
        A(2, 2) = 1.0
        val exp = Matrix(Seq(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0), 3, 3)

        assert(A.impl(1, 1) === 1.0)
      }

      "by row" in {
        val A = Matrix.zeros(3, 3)
        val b = Vector(Seq(0.0, 1.0, 2.0)).t

        A(1,:::) = b
        val exp = Matrix(Seq(0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 2.0, 0.0), 3, 3)

        assert(A.impl === exp.impl)
      }

      "by column" in {
        val A = Matrix.zeros(3, 3)
        val b = Vector(Seq(0.0, 1.0, 2.0))

        A(:::, 1) = b
        val exp = Matrix(Seq(0.0, 0.0, 0.0, 0.0, 1.0, 2.0, 0.0, 0.0, 0.0), 3, 3)

        assert(A.impl === exp.impl)
      }

      "by multiple rows" in {
        val A = Matrix.zeros(3, 3)
        val B = Matrix(Seq(0.0, 1.0, 2.0, 3.0, 4.0, 5.0), 2, 3)

        A(0 to 1, :::) = B
        val exp = Matrix(Seq(0.0, 1.0, 0.0, 2.0, 3.0, 0.0, 4.0, 5.0, 0.0), 3, 3)

        assert(A.impl === exp.impl)
      }

      "by multiple columns" in {
        val A = Matrix.zeros(3, 3)
        val B = Matrix(Seq(0.0, 1.0, 2.0, 3.0, 4.0, 5.0), 3, 2)

        A(:::, 0 to 1) = B
        val exp = Matrix(Seq(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 0.0, 0.0, 0.0), 3, 3)

        assert(A.impl === exp.impl)
      }

      "by arbitrary submatrices" in {
        val A = Matrix.zeros(3, 3)
        val B = Matrix(Seq(0.0, 1.0, 2.0, 3.0), 2, 2)

        A(0 to 1, 0 to 1) = B
        val exp = Matrix(Seq(0.0, 1.0, 0.0, 2.0, 3.0, 0.0, 0.0, 0.0, 0.0), 3, 3)

        assert(A.impl === exp.impl)
      }
    }

    "can not be left-indexed" - {

      "by row when assigned vector is not transposed" is pending

      "by column when assigned vector is transposed" is pending

      "by row when vector is wrong length" is pending

      "by column when vector is wrong length" is pending

      "by submatrix when submatrix dimensions do not fit" is pending
    }

    ///////////////////////////
    // Addition Operator
    ///////////////////////////

    "can be added with" - {

      "a double" in {
        val A = Matrix.rand(10, 10)
        val s = 2.0
        val R = A + s

        assert(R.impl === A.impl + s)
      }

      "a vector per column with matching length" in {
        val A = Matrix(Seq(1.0, 2.0, 3.0, 4.0), 2, 2)
        val b = Vector(Seq(1.0, 2.0))
        val R = A + b

        val expected = Matrix(Seq(2.0, 4.0, 4.0, 6.0), 2, 2)

        assert(R.impl === expected.impl)
      }

      "a vector per row with matching length" in {
        val A = Matrix(Seq(1.0, 2.0, 3.0, 4.0), 2, 2)
        val b = Vector(Seq(1.0, 2.0))
        val R = A + b.t

        val expected = Matrix(Seq(2.0, 4.0, 4.0, 6.0), 2, 2)

        assert(R.impl === expected.impl)
      }

      "a matrix of matching dimensions" in {
        val A = Matrix(Seq(1.0, 2.0, 3.0, 4.0), 2, 2)
        val B = Matrix(Seq(2.0, 1.0, 4.0, 3.0), 2, 2)
        val R = A + B

        val expected = Matrix(Seq(3.0, 3.0, 7.0, 7.0), 2, 2)

        assert(R.impl === expected.impl)
      }
    }

    "can not be added with" - {

      "a vector per column with non-matching length" in {
        val A = Matrix.rand(10, 10)
        val b = Vector.rand(9)

        an [IllegalArgumentException] should be thrownBy A + b
        val thrown = the [IllegalArgumentException] thrownBy A + b
        thrown.getMessage.toLowerCase should include ("vectors must have same length")
      }

      "a vector per row with non-matching length" in {
        val A = Matrix.rand(10, 10)
        val b = Vector.rand(9)

        an [IllegalArgumentException] should be thrownBy A + b.t
        val thrown = the [IllegalArgumentException] thrownBy A + b.t
        thrown.getMessage.toLowerCase should include ("vectors must have same length")
      }

      "a matrix of non-matching dimensions" in {
        val A = Matrix.rand(10, 10)
        val B = Matrix.rand(10, 11)

        an [IllegalArgumentException] should be thrownBy A + B
        val thrown = the [IllegalArgumentException] thrownBy A + B
        thrown.getMessage.toLowerCase should include ("dimension mismatch")
      }

    }

    ///////////////////////////
    // Subtraction Operator
    ///////////////////////////

    "can be subtracted with" - {

      "a double" in {
        val A = Matrix.rand(10, 10)
        val s = 2.0
        val R = A - s

        assert(R.impl === A.impl - s)
      }

      "a vector per column with matching length" in {
        val A = Matrix(Seq(1.0, 2.0, 3.0, 4.0), 2, 2)
        val b = Vector(Seq(1.0, 2.0))
        val R = A - b

        val expected = Matrix(Seq(0.0, 0.0, 2.0, 2.0), 2, 2)

        assert(R.impl === expected.impl)
      }

      "a vector per row with matching length" in {
        val A = Matrix(Seq(1.0, 2.0, 3.0, 4.0), 2, 2)
        val b = Vector(Seq(1.0, 2.0))
        val R = A - b.t

        val expected = Matrix(Seq(0.0, 0.0, 2.0, 2.0), 2, 2)

        assert(R.impl === expected.impl)
      }

      "a matrix of matching dimensions" in {
        val A = Matrix(Seq(1.0, 2.0, 3.0, 4.0), 2, 2)
        val B = Matrix(Seq(2.0, 1.0, 4.0, 3.0), 2, 2)
        val R = A - B

        val expected = Matrix(Seq(-1.0, 1.0, -1.0, 1.0), 2, 2)

        assert(R.impl === expected.impl)
      }
    }

    "can not be subtracted with" - {

      "a vector per column with non-matching length" in {
        val A = Matrix.rand(10, 10)
        val b = Vector.rand(9)

        an [IllegalArgumentException] should be thrownBy A - b
        val thrown = the [IllegalArgumentException] thrownBy A - b
        thrown.getMessage.toLowerCase should include ("vectors must have same length")
      }

      "a vector per row with non-matching length" in {
        val A = Matrix.rand(10, 10)
        val b = Vector.rand(9)

        an [IllegalArgumentException] should be thrownBy A - b.t
        val thrown = the [IllegalArgumentException] thrownBy A - b.t
        thrown.getMessage.toLowerCase should include ("vectors must have same length")
      }

      "a matrix of non-matching dimensions" in {
        val A = Matrix.rand(10, 10)
        val B = Matrix.rand(10, 11)

        an [IllegalArgumentException] should be thrownBy A - B
        val thrown = the [IllegalArgumentException] thrownBy A - B
        thrown.getMessage.toLowerCase should include ("dimension mismatch")
      }

    }

    ///////////////////////////
    // Multiplication Operator
    ///////////////////////////

    "can be multiplied with" - {

      "a double" in {
        val A = Matrix.zeros(10, 10)
        val s = 10.0
        val R = A * s

        assert(R.impl === Matrix.zeros(10,10).impl)
      }

      "a vector per column with matching length" in {
        val A = Matrix(Seq(1.0, 2.0, 3.0, 4.0), 2, 2)
        val b = Vector(Seq(1.0, 2.0))
        val R = A * b

        val expected = Matrix(Seq(1.0, 4.0, 3.0, 8.0), 2, 2)

        assert(R.impl === expected.impl)
      }

      "a vector per row with matching length" in {
        val A = Matrix(Seq(1.0, 2.0, 3.0, 4.0), 2, 2)
        val b = Vector(Seq(1.0, 2.0))
        val R = A * b.t

        val expected = Matrix(Seq(1.0, 4.0, 3.0, 8.0), 2, 2)

        assert(R.impl === expected.impl)
      }

      "a matrix of matching dimensions" in {
        val A = Matrix(Seq(1.0, 2.0, 3.0, 4.0), 2, 2)
        val B = Matrix(Seq(2.0, 1.0, 4.0, 3.0), 2, 2)
        val R = A * B

        val expected = Matrix(Seq(2.0, 2.0, 12.0, 12.0), 2, 2)

        assert(R.impl === expected.impl)
      }

      // Matrix products

      "a vector with matching inner dimensions" in {
        val A = Matrix.zeros(10, 10)
        val b = Vector.rand(10)
        val r = A %*% b
      }

      "a matrix with matching inner dimensions" in {
        val A = Matrix.zeros(10, 10)
        val B = Matrix.zeros(10, 9)
        val R = A %*% B

        assert(R.impl === Matrix.zeros(10, 9).impl)
      }
    }

    "can not be multiplied with" - {

      "a vector per column with non-matching length" in {
        val A = Matrix.rand(10, 10)
        val b = Vector.rand(9)

        an [IllegalArgumentException] should be thrownBy A * b
        val thrown = the [IllegalArgumentException] thrownBy A * b
        thrown.getMessage.toLowerCase should include ("vectors must have same length")
      }

      "a vector per row with non-matching length" in {
        val A = Matrix.rand(10, 10)
        val b = Vector.rand(9)

        an [IllegalArgumentException] should be thrownBy A * b.t
        val thrown = the [IllegalArgumentException] thrownBy A * b.t
        thrown.getMessage.toLowerCase should include ("vectors must have same length")
      }

      "a matrix of non-matching dimensions" in {
        val A = Matrix.rand(10, 10)
        val B = Matrix.rand(10, 11)

        an [IllegalArgumentException] should be thrownBy A * B
        val thrown = the [IllegalArgumentException] thrownBy A * B
        thrown.getMessage.toLowerCase should include ("dimension mismatch")
      }

      "a vector with non-matching inner dimensions" in {
        val A = Matrix.zeros(9, 9)
        val b = Vector.rand(10)

        an [IllegalArgumentException] should be thrownBy A %*% b
        val thrown = the [IllegalArgumentException] thrownBy A %*% b
        thrown.getMessage.toLowerCase should include ("dimension mismatch")
      }

      "a matrix with non-matching inner dimensions" in {
        val A = Matrix.zeros(10, 10)
        val B = Matrix.zeros(9, 9)

        an [IllegalArgumentException] should be thrownBy A %*% B
        val thrown = the [IllegalArgumentException] thrownBy A %*% B
        thrown.getMessage.toLowerCase should include ("dimension mismatch")
      }
    }

    ///////////////////////////
    // Division Operator
    ///////////////////////////

    "can be divided by" - {

      "a double" in {
        val A = Matrix.zeros(10, 10)
        val s = 10.0
        val R = A / s

        assert(R.impl === Matrix.zeros(10,10).impl)
      }

      "a vector per column with matching length" in {
        val A = Matrix(Seq(1.0, 2.0, 3.0, 4.0), 2, 2)
        val b = Vector(Seq(1.0, 2.0))
        val R = A / b

        val expected = Matrix(Seq(1.0, 1.0, 3.0, 2.0), 2, 2)

        assert(R.impl === expected.impl)
      }

      "a vector per row with matching length" in {
        val A = Matrix(Seq(1.0, 2.0, 3.0, 4.0), 2, 2)
        val b = Vector(Seq(1.0, 2.0))
        val R = A / b.t

        val expected = Matrix(Seq(1.0, 1.0, 3.0, 2.0), 2, 2)

        assert(R.impl === expected.impl)
      }

      "a matrix of matching dimensions" in {
        val A = Matrix(Seq(1.0, 2.0, 3.0, 4.0), 2, 2)
        val B = Matrix(Seq(2.0, 1.0, 4.0, 3.0), 2, 2)
        val R = A / B

        val expected = Matrix(Seq(0.5, 2.0, 0.75, 4.0 / 3.0), 2, 2)

        assert(R.impl === expected.impl)
      }
    }

    "can not be divided by" - {

      // TODO should A / 0.0 evaluate to infinity?
      "scalar zero" in {
        val A = Matrix.rand(10, 10)
        val s = 0.0

        an [IllegalArgumentException] should be thrownBy A / s
      }

      "a vector per column with non-matching length" in {
        val A = Matrix.rand(10, 10)
        val b = Vector.rand(9)

        an [IllegalArgumentException] should be thrownBy A / b
        val thrown = the [IllegalArgumentException] thrownBy A / b
        thrown.getMessage.toLowerCase should include ("vectors must have same length")
      }

      "a vector per row with non-matching length" in {
        val A = Matrix.rand(10, 10)
        val b = Vector.rand(9)

        an [IllegalArgumentException] should be thrownBy A / b.t
        val thrown = the [IllegalArgumentException] thrownBy A / b.t
        thrown.getMessage.toLowerCase should include ("vectors must have same length")
      }

      "a matrix of non-matching dimensions" in {
        val A = Matrix.rand(10, 10)
        val B = Matrix.rand(10, 11)

        an [IllegalArgumentException] should be thrownBy A / B
        val thrown = the [IllegalArgumentException] thrownBy A / B
        thrown.getMessage.toLowerCase should include ("dimension mismatch")
      }
    }

    ///////////////////////////
    // Exponentiation Operator
    ///////////////////////////

    "can be exponentiated" - {

      "by an integer" in {
        val values = Seq(1.0, 2.0, 3.0, 4.0)
        val A = Matrix(values, 2, 2)
        val R = A^2

        val expected = Matrix(Seq(1.0, 4.0, 9.0, 16.0), 2, 2)

        assert(R.impl === expected.impl)
      }
    }

    ///////////////////////////
    // Builtin Functions
    ///////////////////////////

    "can be summed" - {

      "by elements" in {
        val A = Matrix.zeros(10,10)
        val s = sum(A)  //TODO: should this be a member of matrix instead of a function in a package object?

        assert(s === 0.0)
      }
    }
  }
}
