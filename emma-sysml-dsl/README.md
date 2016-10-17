# SystemML Scala DSL
Proof of Concept

### Purpose

The SystemML Scala DSL enables data scientists to write algorithms for SystemML
using Scala syntax with specific `Matrix` and `Vector` types. These types come with the promise that every operation on them will be executed using the SystemML compiler and runtime.

### Implementation

The DSL will be implemented using two different approaches that use the same types.

One approach allows quick explorative analysis on the REPL by constructing DML code for the executed operations and lazily evaluating the corresponding data structures on request.

the second approach will use Scala macros and reuse the same `Matrix` and `Vector` types as the first approach. Using macros allows for a holistic view of the program and efficient optimizations. Additionally, it should be better suited to write full-fledged algorithms for SystemML. The user-interface to execution on SystemML will be a call to the `parallelize` macro of the form presented below:

```Scala
def algorithm(params...) = parallelize {
  // program code goes here
}

val result: T = algorithm.run()
```

The macro takes the representation (AST) of the code inside the braces and translates it into a String of DML code.
This transformation is enabled by the [Emma]() compiler project. By making use of this project, the DSL will benefit from any optimization that is implemented in the Emma compiler.

In further iterations of the DSL, the generation of DML strings could be replaced by a direct translation to one of SystemML's intermediate program representations.

It is planned to have the DSL as an independent module inside of the SystemML project while for current development it is more convenient to have it inside the Emma project.

### Current API

The current API to write user code for SystemML includes a `Matrix` and `Vector` type, where vector is just syntactic sugar for a column matrix with a single column in SystemML.
Additionally, we provide all builtin functions that SystemML currently supports. One further improvement might be the implementation of convenience methods that are desugared into DML constructs.
A first implementaiton of the API can be found in the current [development branch](https://github.com/fschueler/emma/tree/sysml-dsl/emma-sysml-dsl/src/main/scala/eu/stratosphere/emma/sysml/api)

An implementation of the [tsne](https://en.wikipedia.org/wiki/T-distributed_stochastic_neighbor_embedding) algorithm using the current API can be found [here](https://github.com/fschueler/emma/blob/sysml-dsl/emma-sysml-dsl/src/main/scala/eu/stratosphere/emma/sysml/examples/TSNE.scala).

~~To enable **rapid prototyping**, the user can develop algorithms and run them as simple Scala code which is currently backed by the [scala-breeze](https://github.com/scalanlp/breeze) library. The required code will look just as the example above without the call of the `parallelize` macro.~~ This feature is scratched since it's not necessary and might lead to considerable overhead and maintenance.

### Prototype

A running prototype is implemented in the current specification for the translation of Scala code to DML that can be found [here](https://github.com/fschueler/emma/blob/sysml-dsl/emma-sysml-dsl/src/test/scala/eu/stratosphere/emma/sysml/macros/RewriteMacrosSpec.scala)

As an example we will look at the "Matrix Multiplication" test from the Spec:

```Scala
"Matrix Multiplication" in {

  def dml(args: Any*): String = parallelize {
    val A = Matrix.rand(5, 3)
    val B = Matrix.rand(3, 7)
    val C = A %*% B
   }

  val exp: String =
    """
      |A = rand(rows=5, cols=3)
      |B = rand(rows=3, cols=7)
      |C = A %*% B
    """.stripMargin.trim

  dml shouldEqual exp
}
```
The above snippet shows how the macro is currently used. The expression inside the `parallelize` macro is transformed into the string that is expected in the value definition `exp`.
The defined function `dml(args: Any*): String` can take any number of arguments that can be used inside the function body, e.g. matrix dimensions, numbers of iterations, and others.

~~In the final implementation, the function `dml` will not return a string but an `Algorithm` instance that can be executed and internally uses the MLContext to pass the generated script with potential outside parameters (such as Spark DataFrames) to SystemML.~~

### Example

As an example that involves control flow (the for-loop) we show how the NMF algorithm can be implemented in the Scala DSL:

```Scala
val nmf = parallelize {
      val tfidf = Array(1.0, 2.0, 3.0, 4.0) // tfidf feature matrix coming from somewhere
      val k = 40
      val m, n = 2 // dimensions of tfidf
      val maxIters = 200

      val V = Matrix(tfidf, m, n) // initialize matrices
      var W = Matrix.rand(m, k)
      var H = Matrix.rand(k, n)

      for (i <- 0 to maxIters) { //main loop
        H = H * (W.t %*% V) / (W.t %*% (W %*% H))
        W = W * (V %*% H.t) / (W %*% (H %*% H.t))
      }

      (W, H) // return values
    }
```
We write the whole algorithm inside the `parallelize` block and specify our return value at the end of the block as it is usually done in Scala. the `parallelize` macro returns an instance of the class `SystemMLAlgorithm` which includes additional boilerplate code for execution in SystemML. To actually execute the generated code we call:

```Scala
val (w, h) = nmf.run()
```

This will run the generated code on SystemML and return the requested values. Internally, the `SystemMLAlgorithm.run()` method uses SystemML's `MLContext` to set input, output and program-parameters.

### Input and Output handling

In the case of NMF we did not load data from any external data-source. In most scenarios, our data will come from some outside source. We provide several ways of passing this data to SystemML. The easiest way is to read data from a file using the builtin `read(...)` primitive that directly maps to SystemML's builtin read primitive.

Additionally, data can be passed from a Spark `DataFrame` that has been created before. The following example shows the usage of passing a dataframe to SystemML:

```Scala
val numRows = 10000
val numCols = 1000
val data = sc.parallelize(0 to numRows-1).map { _ => Row.fromSeq(Seq.fill(numCols)(Random.nextDouble)) }
val schema = StructType((0 to numCols-1).map { i => StructField("C" + i, DoubleType, true) } )
val df = sqlContext.createDataFrame(data, schema)

val alg = parallelize {
      /* this should take a dataframeand set it as input to the MLContext */
      val matrix: Matrix = Matrix.fromDataFrame(df) // can we find out the metadata?

      val minOut = min(matrix)
      val maxOut = max(matrix)
      val meanOut = mean(matrix)

      (minOut, maxOut, meanOut)
    }

val  (minOut: Double, maxOut: Double, meanOut: Double) = alg.run()

println(s"The minimum is $minOut, maximum: $maxOut, mean: $meanOut")
```

An analysis pass will notice that the argument to the matrix constructor is a dataframe and set the corresponding input parameter in the `MLContext` used to run the code. Similarly, an analysis pass sets the parameters for output values that can then be printed from Scala.
