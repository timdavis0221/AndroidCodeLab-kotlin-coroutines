package com.future.lib

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take

fun makeFlow() = flow {
    println("sending first value")
    emit(1)
    delay(1000)

    println("first value collected, sending another value")
    emit(2)
    delay(1000)

    println("second value collected, sending a third value")
    emit(3)
}

@ExperimentalCoroutinesApi
suspend fun main() {
    // collect as a consumer, flow { ... } as a producer
    /*makeFlow().collect { value ->
        println("got $value")
    }
    println("flow is completed")*/

    val repeatableFlow = makeFlow().take(1)
    println("first collection")
    repeatableFlow.collect()
    println("collecting again")
    repeatableFlow.collect()
    println("second collection completed")
}





