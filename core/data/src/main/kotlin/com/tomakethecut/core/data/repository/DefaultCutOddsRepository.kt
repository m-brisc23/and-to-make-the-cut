package com.tomakethecut.core.data.repository

import com.tomakethecut.core.data.di.IoDispatcher
import com.tomakethecut.core.data.mapper.toModel
import com.tomakethecut.core.data.network.OddsApi
import com.tomakethecut.core.domain.repository.CutOddsRepository
import com.tomakethecut.core.model.MakeCutMarket
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultCutOddsRepository @Inject constructor(
    private val api: OddsApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CutOddsRepository {

    private val cache = MemoryCache<String, MakeCutMarket>()

    override suspend fun getMakeCutMarket(tournamentId: String, forceRefresh: Boolean): MakeCutMarket =
        cache.getOrLoad(tournamentId, forceRefresh) {
            val dto = apiCall("Make-cut market for $tournamentId") { api.getMakeCutMarket(tournamentId) }
            withContext(ioDispatcher) { dto.toModel() }
        }
}
