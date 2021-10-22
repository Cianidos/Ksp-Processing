@AnnotationTesting
class VerySimpleClass {
    @Memoize
    private fun fibonacci(n: Int): Int {
        if (n == 0 || n == 1) return 1
        return fibonacci(n - 1, false) + fibonacci(n - 2, false)
    }
}

fun main(args: Array<String>) {
    val v = VerySimpleClass()
    v.HelloWorld()
    println(v.fibonacci(45))
}