# bingo-main → Paper Porting Roadmap

> Goal: port the achievement/card/scoring core from `bingo-main` into the
> AdvancementBinGo Paper plugin, keeping the core logic clean and production-ready.

## Architecture

```
core/       Pure Kotlin logic. No Bukkit/Paper dependencies.
storage/    SQLite statistics storage.
paper/      Bukkit/Paper implementations of core ports, map rendering, GUI.
plugin/     (future) JavaPlugin entry, commands, listeners, world/lobby management.
```

Current root module still contains the legacy Java plugin and will be moved into
`plugin/` or `paper/` in a later step.

## Done

- [x] Gradle multi-module setup: `core`, `storage`, `paper`
- [x] `core`: pure BingoObjective / BingoCard / Team / GameMode models
- [x] `core`: CardGenerator with easy/medium/hard weighted selection
- [x] `core`: ObjectiveTracker using platform ports
- [x] `core`: ScoreService with Lockout support
- [x] `storage`: SQLite StatsRepository
- [x] `paper`: AdvancementPort / ItemPort / StatsPort implementations
- [x] `paper`: ObjectiveResolver for Bukkit advancements
- [x] `paper`: Map renderer + map item factory (colors + numbers)

## Next Steps

- [ ] Per-room mode configuration and per-room waiting lobbies
- [ ] Room selection commands (`/bingo join <room>`)
- [ ] Independent per-room countdown and auto-start
- [ ] Item and Stats objective providers
- [ ] Lockout and Hidden Items game mode behavior
- [ ] GUI implementation (fallback if map is not good enough)
- [ ] Integrate Kotlin core into the existing Java plugin
- [ ] Keep async world reset, rejoin, and production features
