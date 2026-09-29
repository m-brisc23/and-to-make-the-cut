package com.tomakethecut.core.testing

import kotlinx.coroutines.CoroutineDispatcher
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

/**
 * A dispatcher that runs work inline but records every dispatch, so a test can assert that
 * a class really moved its work onto the dispatcher it was given (i.e. that it's main-safe).
 */
class CountingDispatcher : CoroutineDispatcher() {
    private val count = AtomicInteger()

    val dispatches: Int get() = count.get()

    override fun isDispatchNeeded(context: CoroutineContext): Boolean = true

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        count.incrementAndGet()
        block.run()
    }
}
