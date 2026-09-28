package com.tomakethecut.core.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuspendRunCatchingTest {

    @Test
    fun `wraps ordinary exceptions`() = runTest {
        val result = suspendRunCatching { error("boom") }
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `wraps success`() = runTest {
        assertEquals(42, suspendRunCatching { 42 }.getOrThrow())
    }

    @Test(expected = CancellationException::class)
    fun `rethrows cancellation so structured concurrency still works`() = runTest {
        suspendRunCatching { throw CancellationException("cancelled") }
    }
}
