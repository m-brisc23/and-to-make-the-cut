package com.tomakethecut.core.ui

import com.tomakethecut.core.ui.chart.ChartGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChartGeometryTest {

    private val g = ChartGeometry(left = 100f, top = 0f, right = 500f, bottom = 200f, pointCount = 5)

    @Test
    fun `points are spread evenly across the plot`() {
        assertEquals(100f, g.xFor(0))
        assertEquals(300f, g.xFor(2))
        assertEquals(500f, g.xFor(4))
    }

    @Test
    fun `probability maps bottom to top`() {
        assertEquals(200f, g.yFor(0f))
        assertEquals(100f, g.yFor(0.5f))
        assertEquals(0f, g.yFor(1f))
    }

    @Test
    fun `touches snap to the nearest point`() {
        assertEquals(1, g.indexAt(210f)) // 200 is point 1, 300 is point 2
        assertEquals(2, g.indexAt(260f))
    }

    @Test
    fun `touches outside the plot clamp to the ends`() {
        assertEquals(0, g.indexAt(-50f))
        assertEquals(4, g.indexAt(9_999f))
    }

    @Test
    fun `single point is centred and always selected`() {
        val single = g.copy(pointCount = 1)
        assertEquals(300f, single.xFor(0))
        assertEquals(0, single.indexAt(123f))
    }

    @Test
    fun `empty chart has nothing to select`() {
        assertNull(g.copy(pointCount = 0).indexAt(200f))
    }
}
