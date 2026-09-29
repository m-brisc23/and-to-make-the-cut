package com.tomakethecut.core.data.di

import com.tomakethecut.core.data.ApiConfig
import com.tomakethecut.core.data.network.OddsApi
import com.tomakethecut.core.data.network.StatsApi
import com.tomakethecut.core.data.network.mock.ClasspathFixtureSource
import com.tomakethecut.core.data.network.mock.MockApiInterceptor
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        // Forward compatibility: a new field on the server must not crash old app versions.
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(config: ApiConfig): OkHttpClient = OkHttpClient.Builder()
        .apply {
            if (config.useMockApi) {
                addInterceptor(MockApiInterceptor(ClasspathFixtureSource(), config.mockLatencyMillis))
            }
            if (config.enableHttpLogging) {
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
        }
        .build()

    @Provides
    @Singleton
    @OddsRetrofit
    fun provideOddsRetrofit(config: ApiConfig, client: Lazy<OkHttpClient>, json: Json): Retrofit =
        retrofit(config.oddsBaseUrl, client, json)

    @Provides
    @Singleton
    @StatsRetrofit
    fun provideStatsRetrofit(config: ApiConfig, client: Lazy<OkHttpClient>, json: Json): Retrofit =
        retrofit(config.statsBaseUrl, client, json)

    @Provides
    @Singleton
    fun provideOddsApi(@OddsRetrofit retrofit: Retrofit): OddsApi = retrofit.create()

    @Provides
    @Singleton
    fun provideStatsApi(@StatsRetrofit retrofit: Retrofit): StatsApi = retrofit.create()

    /**
     * `callFactory` + `dagger.Lazy` defers building OkHttpClient (TLS/platform setup,
     * interceptors) until the first request instead of at injection time.
     *
     * Note: Retrofit invokes the call factory on the *calling* thread. Laziness alone does not
     * keep this off the main thread — the repositories make every API call inside
     * `withContext(ioDispatcher)`, which is what actually guarantees it.
     */
    private fun retrofit(baseUrl: String, client: Lazy<OkHttpClient>, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .callFactory { request -> client.get().newCall(request) }
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
