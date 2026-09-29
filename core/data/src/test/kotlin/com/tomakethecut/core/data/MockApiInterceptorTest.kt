package com.tomakethecut.core.data

import com.tomakethecut.core.data.network.mock.MockApiInterceptor
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MockApiInterceptorTest {

    private fun resolve(url: String) = MockApiInterceptor.resolveFixturePath(url.toHttpUrl())

    @Test
    fun `routes tournaments by season`() {
        assertEquals("odds/tournaments_2026.json", resolve("https://x/v1/golf/tournaments?season=2026"))
    }

    @Test
    fun `routes make cut markets`() {
        assertEquals(
            "odds/make_cut/masters-tournament-2025.json",
            resolve("https://x/v1/golf/tournaments/masters-tournament-2025/markets/make-cut"),
        )
    }

    @Test
    fun `routes player season stats`() {
        assertEquals("stats/rory-mcilroy/2025.json", resolve("https://x/v1/players/rory-mcilroy/seasons/2025/stats"))
    }

    @Test
    fun `tournaments without a season do not match`() {
        assertNull(resolve("https://x/v1/golf/tournaments"))
    }

    @Test
    fun `path traversal attempts are rejected`() {
        assertNull(resolve("https://x/v1/players/..%2F..%2Fsecrets/seasons/2025/stats"))
        assertNull(resolve("https://x/v1/golf/tournaments/..%2Fodds/markets/make-cut"))
    }

    @Test
    fun `unknown paths do not match`() {
        assertNull(resolve("https://x/v2/anything"))
    }
}
