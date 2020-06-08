package com.future.lib

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take

/*fun <T> createFlow(query: Query, tables: List<Boolean>): Flow<T> =
    flow {
        val changeTracker = tableChangeTracker(tables)

        while (true) {
            emit(suspendQuery(query))
            changeTracker.suspendUntilChanged()
        }
}*/

fun makeFlow() = flow {
    println("sending first value")
    delay(1000)
    emit(1)

    println("first value collected, sending another value")
    delay(1000)
    emit(2)

    println("second value collected, sending a third value")
    emit(3)
}

@ExperimentalCoroutinesApi
suspend fun main() {
    // collect as a consumer (terminal operator)
    // flow { ... } as a producer
    /*makeFlow().collect { value ->
        println("got $value")
    }
    println("flow is completed")*/

    val repeatableFlow = makeFlow().take(2)
    println("first collection")
    repeatableFlow.collect()
    println("collecting again")
    repeatableFlow.collect()
    println("second collection completed")
}





