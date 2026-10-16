package com.tomakethecut.core.model

/**
 * Seasons the app knows about, newest first.
 *
 * Trade-off: hard-coding is fine for a two-season demo. In production this list
 * would come from remote config or a `/seasons` endpoint so a new season doesn't
 * require an app release.
 */
object Seasons {
    val supported: List<Int> = listOf(2026, 2025)
    val current: Int = supported.first()
}
