package com.tomakethecut.core.data.di

import com.tomakethecut.core.data.repository.DefaultCutOddsRepository
import com.tomakethecut.core.data.repository.DefaultPlayerStatsRepository
import com.tomakethecut.core.data.repository.DefaultTournamentRepository
import com.tomakethecut.core.domain.repository.CutOddsRepository
import com.tomakethecut.core.domain.repository.PlayerStatsRepository
import com.tomakethecut.core.domain.repository.TournamentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * `@Binds` (not `@Provides`) because we're only telling Dagger "use this implementation
 * for that interface" — it generates less code and makes the intent obvious.
 */
@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    fun bindTournamentRepository(impl: DefaultTournamentRepository): TournamentRepository

    @Binds
    fun bindCutOddsRepository(impl: DefaultCutOddsRepository): CutOddsRepository

    @Binds
    fun bindPlayerStatsRepository(impl: DefaultPlayerStatsRepository): PlayerStatsRepository
}
