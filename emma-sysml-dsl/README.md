# SystemML Scala DSL
Proof of Concept

### Purpose

The SystemML Scala DSL enables data scientists to write algorithms for SystemML
using Scala syntax with specific `Matrix` and `Vector` types. These types come with the promise that every operation on them will executed using the SystemML compiler and runtime.

### Implementation

The DSL will be implemented using Scala macros. This allows for a holistic view of the program and efficient optimizations. The user interface to execution on SystemML will be a call of the `parallelize` macro of the form presented below:

```Scala
def algorithm(params...) = parallelize {
  // program code goes here
}

val result: T = algorithm.execute()
```

The macro takes the representation of the code that the user wrote inside the call and translates into a String of DML code.
This transformation is enables by the [Emma]() compiler project. By making use of this project, the DSL will benefit from any optimization that is implemented in the Emma compiler.

In further iterations of the DSL, the generation of DML strings could be replaced by a direct translation to one of SystemML's intermediate program representation.

It is planned to have the DSL as an independent module inside of the SystemML project while for current development it is more convenient to have it inside the Emma project.

### Current API

The current API to write user code for SystemML includes a `Matrix` and `Vector` type, where vector is just syntactic sugar for a column matrix with a single column in SystemML.
Additionally, we provide all builtin functions that SystemML currently supports. One further improvement might be the implementation of convenience methods that are desugared into DML constructs.
A first implementaiton of the API can be found in the current [development branch](https://github.com/fschueler/emma/tree/sysml-dsl/emma-sysml-dsl/src/main/scala/eu/stratosphere/emma/sysml/api)

An implementation of the [tsne](https://en.wikipedia.org/wiki/T-distributed_stochastic_neighbor_embedding) algorithm using the current API can be found [here](https://github.com/fschueler/emma/blob/sysml-dsl/emma-sysml-dsl/src/main/scala/eu/stratosphere/emma/sysml/examples/TSNE.scala).

To enable **rapid prototyping**, the user can develop algorithms and run them as simple Scala code which is currently backed by the [scala-breeze](https://github.com/scalanlp/breeze) library. The required code will look just as the example above without the call of the `parallelize` macro.

### Prototype

A running prototype is implemented in the current specification for the translation of Scala code to DML that can be found [here](https://github.com/fschueler/emma/blob/sysml-dsl/emma-sysml-dsl/src/test/scala/eu/stratosphere/emma/sysml/macros/RewriteMacrosSpec.scala)

As an example we will look at the "Matrix Multiplication" test from the Spec:

```Scala
"Matrix Multiplication" in {

  def dml(args: Any*): String = parallelize {
    val A = Matrix.rand(5, 3)
    val B = Matrix.rand(3, 7)
    A %*% B
   }

  val exp: String =
    """
      |A = rand(rows=5, cols=3)
      |B = rand(rows=3, cols=7)
      |A %*% B
    """.stripMargin.trim

  dml shouldEqual exp
}
```
The above snippet shows how the macro is currently used. The expression inside the `parallelize` macro is transformed into the string that is expected in the value definition `exp`.
The defined function `dml(args: Any*): String` can take any number of arguments that can be used inside the function body, e.g. matrix dimensions, numbers of iterations, and others.

In the final implementation, the function `dml` will not return a string but an `Algorithm` instance that can be executed and internally uses the MLContext to pass the generated script with potential outside parameters (such as Spark DataFrames) to SystemML.

On calling `Algorithm.execute()` SystemML will execute the generated DML script and return the output.
