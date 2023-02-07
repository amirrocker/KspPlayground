package de.yello.consumer

import de.yello.processor.annotation.IntSummable

fun main() {
    println("result of IntSummable is: ${Foo(1, 2).sumInts()}")
}

@IntSummable
data class Foo(
    val bar: Int,
    val baz: Int
)