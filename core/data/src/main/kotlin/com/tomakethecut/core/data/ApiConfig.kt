package com.tomakethecut.core.data

/**
 * Everything environment-specific about the network layer, supplied by the app module
 * (from BuildConfig). The data layer never reads BuildConfig itself, which keeps it a
 * reusable, variant-agnostic library.
 */
data class ApiConfig(
    val oddsBaseUrl: String,
    val statsBaseUrl: String,
    /** When true, requests are answered from bundled JSON fixtures and never hit the network. */
    val useMockApi: Boolean,
    /** Artificial latency for the mock server, so loading states are actually visible. */
    val mockLatencyMillis: Long = 0,
    val enableHttpLogging: Boolean = false,
)
