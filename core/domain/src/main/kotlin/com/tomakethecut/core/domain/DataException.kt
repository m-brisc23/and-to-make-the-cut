package com.tomakethecut.core.domain

/**
 * Errors the data layer is allowed to surface.
 *
 * The data layer translates Retrofit/OkHttp exceptions into these, so nothing above
 * it ever needs to import `retrofit2.HttpException` — swapping Retrofit for Ktor would
 * not ripple into ViewModels.
 */
sealed class DataException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Network(cause: Throwable) : DataException("Network unavailable", cause)
    class NotFound(what: String) : DataException("$what not found")
    class Server(val code: Int) : DataException("Server error $code")
    class Parse(cause: Throwable) : DataException("Unexpected response format", cause)
}
