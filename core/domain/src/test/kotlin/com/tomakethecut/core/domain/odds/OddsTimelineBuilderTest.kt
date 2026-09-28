package com.tomakethecut.core.domain.odds

import com.tomakethecut.core.model.Sportsbook
import com.tomakethecut.core.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OddsTimelineBuilderTest {

    private val snapshots = TestData.snapshots(3)

    @Test
    fun `series are index aligned with snapshots and gaps stay null`() {
        val odds = TestData.playerOdds(
            prices = listOf(
                TestData.price("s0", Sportsbook.DRAFTKINGS, yes = -150, no = 130),
                TestData.price("s2", Sportsbook.DRAFTKINGS, yes = -300, no = 240),
            ),
        )

        val timeline = odds.toTimeline(snapshots)

        val dk = timeline.byBook.getValue(Sportsbook.DRAFTKINGS)
        assertEquals(3, dk.size)
        assertNotNull(dk[0])
        assertNull(dk[1])
        assertNotNull(dk[2])
        assertNull(timeline.consensus[1])
    }

    @Test
    fun `books are ordered consistently regardless of api order`() {
        val odds = TestData.playerOdds(
            prices = listOf(
                TestData.price("s0", Sportsbook.CAESARS),
                TestData.price("s0", Sportsbook.DRAFTKINGS),
                TestData.price("s0", Sportsbook.FANDUEL),
            ),
        )

        val books = odds.toTimeline(snapshots).byBook.keys.toList()

        assertEquals(listOf(Sportsbook.DRAFTKINGS, Sportsbook.FANDUEL, Sportsbook.CAESARS), books)
    }

    @Test
    fun `consensus averages the books at each snapshot`() {
        val odds = TestData.playerOdds(
            prices = listOf(
                // symmetric markets => exactly 50% and ~66.7% no-vig
                TestData.price("s0", Sportsbook.DRAFTKINGS, yes = -110, no = -110),
                TestData.price("s0", Sportsbook.FANDUEL, yes = -200, no = 200),
            ),
        )

        val consensus = odds.toTimeline(snapshots).consensus[0]!!

        assertEquals((0.5 + 0.6667) / 2, consensus.value, 1e-3)
    }

    @Test
    fun `change since open is measured in percentage points`() {
        val odds = TestData.playerOdds(
            prices = listOf(
                TestData.price("s0", yes = -110, no = -110), // 50%
                TestData.price("s2", yes = -300, no = 300), // 75%
            ),
        )

        val change = odds.toTimeline(snapshots).changeSinceOpen!!

        assertEquals(25.0, change, 1e-6)
    }
}
