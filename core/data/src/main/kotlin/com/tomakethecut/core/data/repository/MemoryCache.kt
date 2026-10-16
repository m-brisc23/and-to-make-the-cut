package com.tomakethecut.core.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * A tiny in-memory, per-key cache that also de-duplicates concurrent loads: if the board
 * and the detail screen ask for the same market at once, only one request goes out.
 *
 * Trade-off vs. Room: this dies with the process and has no offline support. For a
 * read-only odds viewer that's acceptable (stale odds are worse than no odds). If we add
 * offline mode or notifications, Room becomes the single source of truth and this cache
 * goes away — the repository interface wouldn't change.
 */
class MemoryCache<K : Any, V : Any> {
    private val values = ConcurrentHashMap<K, V>()
    private val locks = ConcurrentHashMap<K, Mutex>()

    suspend fun getOrLoad(key: K, forceRefresh: Boolean = false, load: suspend () -> V): V {
        if (!forceRefresh) values[key]?.let { return it }
        val lock = locks.computeIfAbsent(key) { Mutex() }
        return lock.withLock {
            // Another caller may have loaded it while we waited for the lock.
            if (!forceRefresh) values[key]?.let { return@withLock it }
            load().also { values[key] = it }
        }
    }

    fun clear() {
        values.clear()
    }
}
