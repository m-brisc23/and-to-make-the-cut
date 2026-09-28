package com.tomakethecut.core.domain.usecase

import com.tomakethecut.core.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Test

class BoardFilterSortTest {

    private val market = TestData.market(
        players = listOf(
            // Scheffler: 80% -> 80%, no movement
            TestData.playerOdds(
                player = TestData.player("scheffler", "Scottie Scheffler", "USA"),
                prices = listOf(TestData.price("s0", yes = -400, no = 400), TestData.price("s2", yes = -400, no = 400)),
            ),
            // Lowry: 50% -> 75%, +25
            TestData.playerOdds(
                player = TestData.player("lowry", "Shane Lowry", "IRL"),
                prices = listOf(TestData.price("s0", yes = -110, no = -110), TestData.price("s2", yes = -300, no = 300)),
            ),
            // Clark: 60% -> 20%, -40
            TestData.playerOdds(
                player = TestData.player("clark", "Wyndham Clark", "USA"),
                prices = listOf(TestData.price("s0", yes = -150, no = 150), TestData.price("s2", yes = 400, no = -400)),
            ),
            // Unpriced
            TestData.playerOdds(player = TestData.player("amateur", "Amateur Andy", "USA"), prices = emptyList()),
        ),
    )
    private val entries = market.toBoard().entries

    @Test
    fun `most likely sorts descending with unpriced last`() {
        assertEquals(
            listOf("scheffler", "lowry", "clark", "amateur"),
            entries.filterAndSort("", BoardSort.MOST_LIKELY).map { it.player.id },
        )
    }

    @Test
    fun `least likely sorts ascending with unpriced still last`() {
        assertEquals(
            listOf("clark", "lowry", "scheffler", "amateur"),
            entries.filterAndSort("", BoardSort.LEAST_LIKELY).map { it.player.id },
        )
    }

    @Test
    fun `biggest movers uses absolute change`() {
        assertEquals(
            listOf("clark", "lowry", "scheffler", "amateur"),
            entries.filterAndSort("", BoardSort.BIGGEST_MOVERS).map { it.player.id },
        )
    }

    @Test
    fun `query matches name case-insensitively`() {
        assertEquals(listOf("lowry"), entries.filterAndSort("  LOW ", BoardSort.NAME).map { it.player.id })
    }

    @Test
    fun `query also matches an exact country code`() {
        assertEquals(listOf("lowry"), entries.filterAndSort("irl", BoardSort.NAME).map { it.player.id })
    }
}
