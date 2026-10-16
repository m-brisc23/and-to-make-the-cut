package com.tomakethecut.core.data.repository

import com.tomakethecut.core.domain.DataException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

/**
 * The single place where transport-specific exceptions become domain exceptions.
 * Everything above this line speaks [DataException] only.
 */
internal suspend fun <T> apiCall(what: String, block: suspend () -> T): T =
    try {
        block()
    } catch (e: HttpException) {
        throw if (e.code() == 404) DataException.NotFound(what) else DataException.Server(e.code())
    } catch (e: SerializationException) {
        throw DataException.Parse(e)
    } catch (e: IOException) {
        throw DataException.Network(e)
    }
