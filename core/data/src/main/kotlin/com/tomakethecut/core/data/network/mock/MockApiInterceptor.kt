package com.tomakethecut.core.data.network.mock

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * A fake backend that lives *inside* OkHttp.
 *
 * Why mock at the HTTP layer instead of swapping in a fake repository?
 *  - Retrofit, the JSON converter, DTOs, mappers, error handling and caching all run for
 *    real. When the real API arrives, the only thing that changes is where bytes come from.
 *  - The fixtures double as living API documentation / a contract for the backend team.
 *
 * Trade-off: fixture JSON must be kept in sync with DTOs — covered by FixtureContractTest.
 *
 * It's an *application* interceptor that never calls `chain.proceed()`, so no request ever
 * leaves the device in mock mode.
 */
class MockApiInterceptor(
    private val fixtures: FixtureSource,
    private val latencyMillis: Long = 0,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (latencyMillis > 0) Thread.sleep(latencyMillis)

        val body = if (request.method == "GET") resolveFixturePath(request.url)?.let(fixtures::read) else null
        val (code, message, json) = when (body) {
            null -> Triple(404, "Not Found", """{"error":"not_found","path":"${request.url.encodedPath}"}""")
            else -> Triple(200, "OK", body)
        }
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(message)
            .body(json.toResponseBody(JSON))
            .build()
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        // Ids become file paths, so be strict: this rejects "../" and anything exotic.
        private const val ID = "[a-z0-9][a-z0-9-]{0,80}"
        private val TOURNAMENTS = Regex("/v1/golf/tournaments")
        private val MAKE_CUT = Regex("/v1/golf/tournaments/($ID)/markets/make-cut")
        private val STATS = Regex("/v1/players/($ID)/seasons/(\\d{4})/stats")

        /** Maps a request URL to a fixture path, or null when no route matches. Pure, so it's easy to test. */
        fun resolveFixturePath(url: HttpUrl): String? {
            val path = url.encodedPath
            TOURNAMENTS.matchEntire(path)?.let {
                val season = url.queryParameter("season")?.takeIf { s -> s.matches(Regex("\\d{4}")) } ?: return null
                return "odds/tournaments_$season.json"
            }
            MAKE_CUT.matchEntire(path)?.let { return "odds/make_cut/${it.groupValues[1]}.json" }
            STATS.matchEntire(path)?.let { return "stats/${it.groupValues[1]}/${it.groupValues[2]}.json" }
            return null
        }
    }
}
