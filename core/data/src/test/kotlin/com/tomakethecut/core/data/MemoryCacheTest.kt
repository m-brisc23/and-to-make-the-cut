package com.tomakethecut.core.data

import com.tomakethecut.core.data.repository.MemoryCache
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryCacheTest {

    private val cache = MemoryCache<String, Int>()
    private var loads = 0

    private suspend fun load(key: String = "k") = cache.getOrLoad(key) { ++loads }

    @Test
    fun `second read is served from cache`() = runTest {
        load()
        load()
        assertEquals(1, loads)
    }

    @Test
    fun `force refresh reloads`() = runTest {
        load()
        cache.getOrLoad("k", forceRefresh = true) { ++loads }
        assertEquals(2, loads)
    }

    @Test
    fun `different keys load independently`() = runTest {
        load("a")
        load("b")
        assertEquals(2, loads)
    }

    @Test
    fun `concurrent requests for the same key share one load`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val first = async { cache.getOrLoad("k") { gate.await(); ++loads } }
        val second = async { cache.getOrLoad("k") { ++loads } }
        yield()
        gate.complete(Unit)

        assertEquals(1, first.await())
        assertEquals(1, second.await())
        assertEquals(1, loads)
    }
}
