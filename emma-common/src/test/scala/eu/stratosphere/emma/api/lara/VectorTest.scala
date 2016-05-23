package eu.stratosphere.emma.api.lara

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class VectorTest extends BaseTest {

  var vector: Vector[Int] = _
  val values: Array[Int] = Array(1, 2, 3, 4, 5)

  before {
    vector = Vector(values)
  }

  "vector properties" - {
    "orientation" in {
      vector.rowVector shouldBe false
    }
    "length" in {
      vector.length shouldBe values.length
    }
    "fields" in {
      vector.get(2) shouldBe values(2)
    }
    "invalid indexes" in {
      a[IndexOutOfBoundsException] should be thrownBy {
        vector.get(values.length + 3)
      }
    }
  }

  "vector x scalar" - {
    "+" in {
      val res = vector + 3
      res.toArray shouldBe values.map(_ + 3)
    }
    "-" in {
      val res = vector - 3
      res.toArray shouldBe values.map(_ - 3)
    }
    "*" in {
      val res = vector * 3
      res.toArray shouldBe values.map(_ * 3)
    }
    "/" in {
      val res = vector / 3
      res.toArray shouldBe values.map(_ / 3)
    }
  }

  "vector x vector" - {
    "+" in {
      val other = Vector(values)
      val res = vector + other
      res.toArray shouldBe values.zip(values).map(e => e._1 + e._2)
    }
    "-" in {
      val other = Vector(values)
      val res = vector - other
      res.toArray shouldBe values.zip(values).map(e => e._1 - e._2)
    }
    "*" in {
      val other = Vector(values)
      val res = vector * other
      res.toArray shouldBe values.zip(values).map(e => e._1 * e._2)
    }
    "/" in {
      val other = Vector(values)
      val res = vector / other
      res.toArray shouldBe values.zip(values).map(e => e._1 / e._2)
    }
    "inner (dot) product" in {
      val other = Vector(values)
      val res = vector.dot(other)
      res shouldBe values.zip(values).map(e => e._1 * e._2).reduce(_ + _)
    }
    "outer product" in {
      val other = Vector(values)
      val res = vector %*% other
      val expected = for (i <- values.indices; j <- values.indices) yield {
        values(i) * values(j)
      }
      res.toArray shouldBe expected
    }
  }

  "vector transformations" - {
    "aggregation" in {
      vector.aggregate(_ + _) should be(values.reduce(_ + _))
    }
    "fold" in {
      vector.fold[Double](0.0)(a => a * 1.0, (a, b) => a * b * 3.0) should be {
        values.foldLeft[Double](0.0)((d, i) => d * i * 3.0)
      }
    }
    "transpose" in {
      vector.rowVector should be(false)
      val transposed = vector.transpose()
      transposed.rowVector should be(true)
    }
    "diagonal" in {
      val res = vector.diag()
      val expected = for (i <- values.indices; j <- values.indices) yield {
        if (i == j) values(i) else 0
      }
      res.toArray should be(expected)
    }
    "map" in {
      val res = vector.map(a => a * 3.0)
      res.toArray should be(values.map(_ * 3.0))
    }
  }
}
