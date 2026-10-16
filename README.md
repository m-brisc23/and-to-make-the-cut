# ToMakeTheCut

An Android app that tracks PGA TOUR players' odds of **making the cut**, as priced by DraftKings,
FanDuel, BetMGM and Caesars, and charts how those odds move over a tournament. Tap a player to see
an interactive per-sportsbook chart plus their PGA TOUR or Korn Ferry Tour statistics for the
2025 and 2026 seasons.

> **Demo data.** Sportsbooks don't publish free public APIs, so the app ships with a mock backend
> that serves simulated odds and stats. Player names, events, venues and several well-known 2025
> results are real; per-round scores, prices and stat lines are generated. See
> [Swapping in real APIs](#swapping-in-real-apis). Not betting advice.

## Features

- **Cut-odds board.** Choose the 2025 or 2026 season and a tournament, then see every priced player
  with their consensus make-cut probability, a sparkline of how it moved, the change since the
  opening line, the best available "Yes" price, and (once it's official) whether they made the cut.
  You can search by name or country code and sort by likelihood or by biggest movers. Pull down
  to refresh.
- **Interactive odds chart.** One line per sportsbook plus a bold consensus line. Drag across it
  to scrub through the tournament from the opening line to the end of R2. The legend chips toggle
  books on and off, and a price table shows the exact Yes/No prices and no-vig probability at the
  selected snapshot.
- **Player statistics.** Events played, cuts made, wins, top-10s, scoring average, driving and
  greens-in-regulation numbers, and strokes gained by category, plus recent results. Each season is
  labelled with its tour, so a 2025 Korn Ferry graduate shows KFT stats for 2025 and PGA TOUR stats
  for 2026.

## Tech stack

| Concern | Choice |
|---|---|
| Language | Kotlin 2.1 |
| UI | Jetpack Compose, Material 3, a hand-drawn Canvas chart |
| Architecture | MVVM + UDF, clean layers (UI → domain → data), multi-module |
| DI | Dagger Hilt (`hilt-core` in the pure-JVM data module) |
| Networking | Retrofit + OkHttp + kotlinx.serialization |
| Async | Coroutines and Flow (`StateFlow`, `stateIn(WhileSubscribed)`) |
| Navigation | Navigation Compose with type-safe `@Serializable` routes |
| Tests | JUnit4, kotlinx-coroutines-test, Turbine, hand-written fakes |
| Build | Gradle version catalog, typesafe project accessors, GitHub Actions CI |

## Module map

```
:app                 Application, Activity, NavHost, BuildConfig → ApiConfig
:feature:cutodds     Board screen + ViewModel            (Android library)
:feature:player      Player detail screen + ViewModel    (Android library)
:core:ui             Theme, chart, shared components     (Android library)
:core:domain         Repository interfaces, use cases, odds math  (pure Kotlin)
:core:data           Retrofit, DTOs, mappers, mock server, repos  (pure Kotlin)
:core:model          Domain types                        (pure Kotlin)
:core:testing        Fakes, test data builders, MainDispatcherRule (pure Kotlin)
tools/mockdata       Deterministic generator for the mock API fixtures
```

Features depend on `:core:ui` → `:core:domain`, and never on `:core:data`. Only `:app` sees
everything. **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** explains why it's split this way and
walks through each design decision and its trade-offs.

## Building and running

Requirements: Android Studio (Ladybug or newer), JDK 17, and Android SDK 35.

```bash
./gradlew :app:installDebug     # build and install on a device or emulator
./gradlew test                  # every unit test (JVM modules and Android local tests)
./gradlew :core:data:test       # a single module
```

The mock server adds 600 ms of latency (`MOCK_LATENCY_MS` in `app/build.gradle.kts`) so that
loading states show up in the demo. Set it to `0L` if you want instant responses.

### Regenerating mock data

```bash
python3 tools/mockdata/generate.py
```

The generator uses a fixed seed, so the output is byte-identical between runs. `FixtureContractTest`
checks that every tournament has a market and every priced player has stats for that season.

## Swapping in real APIs

1. In `app/build.gradle.kts`, set `USE_MOCK_API` to `false` and point `ODDS_BASE_URL` and
   `STATS_BASE_URL` at your providers. An odds aggregator (for example The Odds API, OddsJam or
   SportsDataIO) is the realistic choice here, because sportsbooks don't offer public APIs.
2. If the provider's JSON is shaped differently, add DTOs and a mapper in `:core:data` that produce
   the same domain models. Nothing in `:core:domain`, the features or the UI has to change.
3. Put API keys in an OkHttp interceptor that reads from `local.properties` or CI secrets. Never
   commit them.

## Project docs

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): design decisions, trade-offs and what I'd do next
- [tools/mockdata/generate.py](tools/mockdata/generate.py): how the simulated data is produced
