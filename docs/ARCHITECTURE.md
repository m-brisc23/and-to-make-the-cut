# Architecture and design decisions

This document explains why the codebase is built the way it is, written as the review notes a
senior engineer would leave for the team. Every decision had alternatives and costs something.
Each section says what the cost is and at what point I'd change my mind.

---

## 1. The big picture

```
┌──────────────────────────── :app ────────────────────────────┐
│  MainActivity → NavHost      AppConfigModule (BuildConfig)   │
└───────┬──────────────────────────────┬───────────────────────┘
        │                              │
┌───────▼────────┐  ┌──────────────────▼─┐
│ :feature:      │  │ :feature:player    │   UI layer
│   cutodds      │  │                    │   Composables + ViewModels
└───────┬────────┘  └─────────┬──────────┘
        └──────────┬──────────┘
            ┌──────▼──────┐
            │  :core:ui   │   theme, chart, LoadState, shared components
            └──────┬──────┘
            ┌──────▼──────┐
            │ :core:domain│   use cases, repository *interfaces*, odds math   (pure Kotlin)
            └──────┬──────┘
            ┌──────▼──────┐
            │ :core:model │   AmericanOdds, Probability, Tournament, …       (pure Kotlin)
            └─────────────┘
            ┌─────────────┐
            │ :core:data  │   implements domain interfaces: Retrofit, DTOs,  (pure Kotlin)
            └─────────────┘   mappers, cache, MockApiInterceptor
```

**How a tap flows through it.** Say you tap *Rory McIlroy* on the board:

1. `CutOddsBoardScreen` calls `onPlayerClick(tournamentId, playerId)`. The screen doesn't know
   where that leads. The app's `NavHost` handles it by navigating to `PlayerDetailRoute(ids)`.
2. `PlayerDetailViewModel` reads the ids from `SavedStateHandle` and runs two independent loads:
   `GetPlayerCutOddsUseCase` and `GetPlayerStatsUseCase`.
3. The odds use case asks `CutOddsRepository`, an interface defined in the domain, for the market.
   `DefaultCutOddsRepository` in `:core:data` already has it in its memory cache from the board
   screen, so it returns immediately.
4. The stats use case fetches 2026 and 2025 in parallel through `StatsApi`. In mock mode,
   `MockApiInterceptor` answers from a JSON fixture inside OkHttp, so Retrofit, the JSON converter
   and the mappers all run exactly as they would against a real server.
5. The ViewModel combines everything into a single immutable `PlayerDetailUiState`. Compose
   collects it with `collectAsStateWithLifecycle()` and renders it.

State flows down and events flow up. That's unidirectional data flow (UDF), which is Google's
recommended app architecture.

---

## 2. Decision log

### D1. Multi-module, with the domain and data layers as pure Kotlin/JVM

**Decision.** `:core:model`, `:core:domain`, `:core:data` and `:core:testing` use the
`kotlin("jvm")` plugin, not the Android library plugin.

**Why.**
- **Speed and honesty.** Their tests run on the plain JVM in milliseconds, with no Robolectric and
  no emulator. And because `android.*` isn't on their classpath, nobody can reach for a `Context`
  in the data layer.
- **Enforced boundaries.** A feature module can't import Retrofit, because it doesn't depend on
  `:core:data`. Code review doesn't have to catch that; the compiler refuses.
- **Build parallelism.** Gradle builds sibling modules concurrently and only recompiles what
  changed.

**Trade-off.** There's more build configuration, and you have to think about which module owns
what. Hilt in a JVM module needs `hilt-core` + `ksp(hilt-compiler)` instead of the usual Android
setup, which is a little unfamiliar.

**When I'd change it.** For a weekend prototype a single module is fine. The split pays off once
there are two or more developers or a CI time budget.

### D2. A domain layer with use cases

Google's guide calls the domain layer *optional*. I kept it because each use case here carries
real logic that would otherwise leak into ViewModels:

- `GetCutOddsBoardUseCase` pivots flat price lists into chart-ready timelines and finds the best
  price.
- `GetPlayerCutOddsUseCase` joins the market with tournament metadata in parallel.
- `GetPlayerStatsUseCase` fans out over seasons and treats "no stats this season" as normal.
- `filterAndSort` and `featured()` are pure functions, so they're trivially testable and reusable
  from a widget or a Wear tile.

**Trade-off.** It's one more class per operation. A use case that just forwards a repository call
is ceremony, and I'd skip it rather than add one for symmetry.

### D3. Repository interfaces live in the domain (Dependency Inversion)

`TournamentRepository`, `CutOddsRepository` and `PlayerStatsRepository` are declared in
`:core:domain` and implemented in `:core:data`. The high-level policy (use cases) doesn't depend on
low-level detail (Retrofit). Both depend on the abstraction.

The interfaces are **small and role-specific** (Interface Segregation). The player screen never
sees tournament-listing methods it doesn't need, and each fake in `:core:testing` is about ten
lines long.

### D4. Mock at the HTTP boundary, not the repository

**Decision.** `MockApiInterceptor` is an OkHttp *application interceptor* that never calls
`chain.proceed()`. It maps request URLs to JSON fixtures under `core/data/src/main/resources/mock/`.

**Why not just a fake repository?** A fake repository would bypass Retrofit, the converter, the DTOs,
the mappers, the error translation and the cache, which is where integration bugs actually live.
With the interceptor, going live means changing a base URL. Nothing else is different.

**Trade-offs.**
- The fixtures must stay in sync with the DTOs. `FixtureContractTest` and
  `RepositoryIntegrationTest` guard against drift.
- About 2 MB of JSON ships in the APK. For production I'd move the interceptor and fixtures into a
  `:core:data-mock` module used only as `debugImplementation`, and bind the interceptor through a
  Hilt multibinding so release builds never see it.
- The interceptor validates ids with a strict regex before turning them into file paths. It's
  still code that reads files based on input, so it's worth being careful. `MockApiInterceptorTest`
  covers path traversal.

### D5. Model an odds *aggregator*, not DraftKings directly

Sportsbooks don't offer public APIs. Their internal endpoints change without notice, and scraping
them usually breaks their terms of service. The realistic production design is an aggregator that
normalizes many books behind one contract. That's why `LineDto` has a `sportsbook` field and
`Sportsbook.fromApiKey()` exists. Adding a book is one enum entry and a colour.

### D6. DTOs ≠ domain models, and mapping is lenient

DTOs mirror the wire format: enums come in as `String`, and nullable fields match the JSON. Mappers
convert them to typed models and follow one rule:

> Be strict about what the app can't work without (ids, dates). Be lenient about everything else.

A line from an unknown book, or an impossible price like `+50`, is dropped. An unknown
`TournamentStatus` falls back to `UPCOMING`. One malformed price costs one data point, not the
whole leaderboard. `Json { ignoreUnknownKeys = true }` means a new server field can't crash old
app versions.

### D7. Value classes for odds and probabilities

`AmericanOdds(-450)` and `Probability(0.82)` are `@JvmInline value class`es. They cost nothing at
runtime, but the compiler now stops you from passing a price where a percentage is expected. Each
type enforces its own invariant in exactly one place: odds can't sit between −100 and +100, and a
probability stays within 0..1.

### D8. The odds math: removing the vig

A book prices "Make cut: Yes −120 / No +100". Their implied probabilities are 54.5% and 50%,
which add up to **104.5%**. The extra 4.5% is the bookmaker's margin (the *vig* or *overround*).
To compare books fairly, `OddsMath.noVigProbability` normalizes each side:
`p_yes / (p_yes + p_no)`. The **consensus** is the mean of the books' fair probabilities.

**Trade-offs.**
- Proportional de-vigging slightly misprices longshots compared with methods like Shin's or the
  power method. For a cut market, which is usually between 20% and 95%, the difference is noise.
- When a book only offers the Yes side, we fall back to its implied probability, which includes
  the vig. That's a known approximation and it's documented in code. We don't silently drop the
  point.
- A plain mean treats every book equally. Weighting by market sharpness would be more accurate,
  but it needs data we don't have.

### D9. In-memory cache instead of Room

`MemoryCache` is per-key and de-duplicates concurrent loads: the board and the detail screen
asking at the same moment produces one request.

**Why not Room as the single source of truth?** Google recommends offline-first, and for many apps
that's right. Here, stale odds are *worse* than no odds, the data is read-only, and the app has
no offline use case. Room would add a schema, migrations and a second model layer for little
benefit.

**When I'd change it.** If we add push alerts ("Scheffler's cut odds dropped below 60%"), offline
browsing, or history beyond one session. The repository interfaces wouldn't change, so the rest
of the app wouldn't notice.

### D10. Errors: translate once, never leak

- `apiCall { }` in `:core:data` is the **only** place transport exceptions become domain
  `DataException`s (`Network`, `NotFound`, `Server`, `Parse`). Nothing above it imports
  `retrofit2.HttpException`.
- Use cases return `Result<T>` via `suspendRunCatching`, which **re-throws
  `CancellationException`**. The standard library's `runCatching` swallows it, which silently
  breaks structured concurrency. It's one of the most common coroutine bugs I see in reviews.
- The UI maps errors to an `ErrorKind` and then to a localized string. It never shows
  `exception.message`, which is unlocalized and can leak internals.
- "No stats for this season" is **not an error**. The repository returns `null` and caches that
  answer.

### D11. UI state: one immutable object per screen, one `LoadState` per section

- The board and the detail screen each expose **one** `StateFlow<UiState>`, derived with `combine`
  from inputs. Nothing mutates UI fields directly, so the state can't drift, and a test can assert
  a whole screen in one line.
- The detail screen's odds and stats sections each have their own `LoadState`. If the stats service
  is down, the chart still shows. A single screen-wide spinner or error would throw away data we
  already have.
- **Refresh keeps stale data.** A failed pull-to-refresh leaves the last good odds on screen and
  sets `transientError`, which the UI shows as a snackbar and then clears. That's Google's
  "events as state" guidance. It survives rotation, while a `Channel` could drop the event.
- The board ties the loaded market to the tournament it belongs to
  (`Pair<Tournament?, LoadState<CutOddsBoard>>`), so there's never a frame where a new tournament
  header sits above the previous tournament's players.

### D12. `StateFlow`, `WhileSubscribed(5_000)` and `SavedStateHandle`

- `stateIn(viewModelScope, WhileSubscribed(5_000), initial)` keeps upstream flows alive through a
  rotation, which takes well under 5 s, but stops them when the app is backgrounded. That saves
  battery and network.
- The season, tournament, search query and sort order live in **`SavedStateHandle`**, so they
  survive process death, not just configuration changes. Transient things like "is refreshing"
  don't need to.
- `loadWithRetry()` uses `transformLatest`, so hammering Retry cancels the in-flight request
  instead of racing it.

### D13. A hand-drawn Canvas chart

**Why not Vico or MPAndroidChart?** Our needs are narrow: one y-scale from 0 to 100%, gaps where a
book pulled its market, and a crosshair you can scrub. A Canvas implementation is about 200 lines
we fully control, with no library whose Compose-version compatibility we'd have to track.

**How it's built.**
- **Stateless.** The selected index is hoisted to the ViewModel, so the readout, the chart marker
  and the price table all stay in sync from one source of truth.
- **The maths is separate.** `ChartGeometry` (data↔pixel mapping and touch hit-testing) is plain
  Kotlin with its own unit tests. Hit-testing is the part most likely to have off-by-one bugs.
- **Colour rules.** Each sportsbook has a fixed hue slot. Hiding a book never repaints the others.
  The palette was validated for colour-vision-deficiency separation in both light and dark themes.
  Light-mode contrast is low for two hues, so the chart always ships with a legend *and* a price
  table (the same numbers as text). Status colours (made/missed cut) always come with an icon and
  a word, never colour alone.
- **Accessibility.** The chart has a `contentDescription` that summarizes it. List rows merge
  their semantics into one sentence ("Rory McIlroy, 83% chance to make the cut").

**When I'd change it.** If we need zoom or pan, multiple chart types, or polished animations,
a library earns its keep.

### D14. Type-safe navigation that passes only ids

Routes are `@Serializable` classes, so there are no string templates and no manual argument
parsing. Only **ids** travel through navigation, never whole objects. The destination loads from
the (cached) repository, so it behaves the same from a deep link, after process death, or when
you arrive from the board.

`PlayerDetailViewModel` reads `savedStateHandle["playerId"]` directly instead of calling
`toRoute()`. `toRoute()` needs Android's `Bundle`, which would force Robolectric into otherwise
pure-JVM ViewModel tests.

Each feature exposes a `NavGraphBuilder` extension and takes callbacks for outbound navigation.
Features never depend on each other, and only `:app` wires them together.

### D15. Hilt details that matter

- **`@Binds` for interface→implementation** (`DataModule`) and **`@Provides`** only when
  construction logic is needed (`NetworkModule`).
- **Qualifiers** (`@OddsRetrofit`, `@StatsRetrofit`, `@IoDispatcher`). The two APIs live on
  different hosts, and injecting dispatchers lets tests pass `Dispatchers.Unconfined`.
- **`dagger.Lazy<OkHttpClient>` + Retrofit's `callFactory`** defers building OkHttp, which does
  disk and TLS setup, until the first request, keeping it off the startup path.
- **`ApiConfig` is provided by `:app` from `BuildConfig`**. That's the only place build variants
  are read, which keeps `:core:data` variant-agnostic and easy to test.

### D16. Build setup

- **Version catalog** (`gradle/libs.versions.toml`): one place to upgrade dependencies, and it
  works with Dependabot or Renovate.
- **No convention plugins yet.** With four Android modules, the repeated `android { }` block is
  easier to read than a `build-logic` included build. At around eight or more modules I'd extract
  convention plugins (as Now in Android does) so a new feature module is three lines.
- **CI** runs every unit test and assembles the debug APK on each push.

---

## 3. Testing strategy

```
          ▲ fewer, slower
          │   (not yet) Compose UI tests / screenshot tests
          │   ViewModel tests      feature/*/src/test      fakes + MainDispatcherRule
          │   Integration tests    core/data/src/test      real Retrofit + mock server
          │   Unit tests           core/*/src/test         pure functions, use cases
          ▼ many, fast
```

- **Fakes over mocks.** `:core:testing` provides hand-written fake repositories. Fakes behave like
  the real thing, survive refactors, and make tests read as behaviour rather than a list of
  `verify { }` calls. That's Google's stated preference too.
- **Test-data builders** (`TestData.tournament(status = UPCOMING)`) let each test override only
  the fields it cares about, so the intent of the test stays visible.
- **ViewModel tests collect `uiState` in `backgroundScope`**, exactly as the UI does, because
  `WhileSubscribed` flows don't run without a collector. That's a classic source of "my test sees
  only the initial state" confusion.
- **The data itself is tested.** `FixtureContractTest` checks referential integrity across all
  generated fixtures.
- **Not yet covered:** Compose UI tests and screenshot tests (Roborazzi or Paparazzi). The
  composables are split into a stateful `Screen` and a stateless `Content`, so adding them later
  needs no refactoring.

---

## 4. SOLID, concretely

| Principle | Where you can see it |
|---|---|
| **S**ingle responsibility | `OddsMath` does maths, `apiCall` translates errors, `MemoryCache` caches, each use case does one thing |
| **O**pen/closed | Adding a sportsbook is one enum entry. Adding a provider is new DTOs + mapper, with no changes to domain or UI |
| **L**iskov substitution | Fakes and the real repositories are interchangeable behind the same interfaces, and the tests prove it |
| **I**nterface segregation | Three small repository interfaces instead of one `GolfRepository` god-interface |
| **D**ependency inversion | Domain owns the interfaces, data implements them, and Hilt wires them at the edge (`:app`) |

---

## 5. What I'd do next

1. **Real providers** behind the existing interfaces, with API keys held in an interceptor and
   read from CI secrets.
2. **Live tournaments.** Poll every 60 s while a round is in progress, using
   `repeatOnLifecycle`-aware flows (never in the background), and add a cache TTL.
3. **Room + WorkManager** if we add price alerts or offline history (see D9).
4. **Move the mock server into its own debug-only module** (see D4).
5. **Screenshot tests** for the chart in light and dark themes, and a Compose UI test for the
   board → detail flow.
6. **Convention plugins** once the module count grows (see D16).
7. **Responsible-gambling features** such as the 21+ gate, links to help resources and regional
   availability. They're non-negotiable before any real-money data goes live.
