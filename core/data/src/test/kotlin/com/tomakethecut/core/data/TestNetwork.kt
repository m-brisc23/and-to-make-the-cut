package com.tomakethecut.core.data

import com.tomakethecut.core.data.di.NetworkModule
import com.tomakethecut.core.data.network.OddsApi
import com.tomakethecut.core.data.network.StatsApi
import dagger.Lazy
import okhttp3.Interceptor
import okhttp3.OkHttpClient

/**
 * Builds the *production* Retrofit stack via NetworkModule, optionally with an extra
 * interceptor, so tests exercise exactly what ships.
 */
class TestNetwork(
    config: ApiConfig = ApiConfig(
        oddsBaseUrl = "https://odds.test/",
        statsBaseUrl = "https://stats.test/",
        useMockApi = true,
    ),
    extraInterceptor: Interceptor? = null,
) {
    private val json = NetworkModule.provideJson()
    private val client: OkHttpClient = NetworkModule.provideOkHttpClient(config).let { base ->
        if (extraInterceptor == null) base else base.newBuilder().apply { interceptors().add(0, extraInterceptor) }.build()
    }
    private val lazyClient = Lazy { client }

    val oddsApi: OddsApi = NetworkModule.provideOddsApi(NetworkModule.provideOddsRetrofit(config, lazyClient, json))
    val statsApi: StatsApi = NetworkModule.provideStatsApi(NetworkModule.provideStatsRetrofit(config, lazyClient, json))
}
