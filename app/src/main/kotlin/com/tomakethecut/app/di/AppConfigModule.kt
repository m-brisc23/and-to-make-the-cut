package com.tomakethecut.app.di

import com.tomakethecut.app.BuildConfig
import com.tomakethecut.core.data.ApiConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Bridges build configuration into the data layer. This is the *only* place that reads
 * BuildConfig, so :core:data stays variant-agnostic and trivially testable.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {

    @Provides
    @Singleton
    fun provideApiConfig(): ApiConfig = ApiConfig(
        oddsBaseUrl = BuildConfig.ODDS_BASE_URL,
        statsBaseUrl = BuildConfig.STATS_BASE_URL,
        useMockApi = BuildConfig.USE_MOCK_API,
        mockLatencyMillis = BuildConfig.MOCK_LATENCY_MS,
        enableHttpLogging = BuildConfig.DEBUG,
    )
}
