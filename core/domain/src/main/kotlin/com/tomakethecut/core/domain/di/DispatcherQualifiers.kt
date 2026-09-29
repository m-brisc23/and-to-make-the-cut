package com.tomakethecut.core.domain.di

import javax.inject.Qualifier

/*
 * Threading model (see docs/ARCHITECTURE.md, D17):
 *  - Main:    UI state only. ViewModels start work here, but never do heavy work here.
 *  - IO:      blocking I/O — network calls, disk.
 *  - Default: CPU work — JSON mapping, odds math, pivoting timelines, filtering/sorting.
 *
 * Dispatchers are injected (never referenced as `Dispatchers.X` in business code) so tests
 * can substitute a TestDispatcher and stay deterministic.
 */

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
