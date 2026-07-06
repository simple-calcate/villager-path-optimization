# Villager Path Optimization

A NeoForge mod for Minecraft 1.21 that tracks block dependencies along villager navigation paths and proactively notifies villagers when their path is obstructed.

> **Status: v0.1.0 — Proof of Concept**
>
> This is an experimental mod. The current version implements the dependency-tracking infrastructure and proactive wakeup mechanism. See [Limitations](#limitations) and [Roadmap](#roadmap) for details on what it does and doesn't achieve yet.

## What It Does

When a villager computes a path, the mod records the blocks along that path (floor, support block below, head-level block above) as **dependencies**. If any of those blocks change (broken, placed, or neighbor-notified) while the path is still active, the villager is enqueued for a **gentle wakeup** — `navigation.stop()` is called, and the vanilla AI re-evaluates on the next tick.

This is **proactive notification** instead of vanilla's **passive discovery** (where the villager walks into the obstacle before realizing the path is broken).

### How It Differs From Vanilla

| Aspect | Vanilla | This Mod |
|--------|---------|----------|
| Path recomputation | Only when AI goal triggers or villager gets stuck | Also triggered immediately when path block changes |
| Obstacle detection | Passive — villager walks into it, then `stop()` | Active — block change event fires, villager notified instantly |
| Path caching | None | None (planned) |
| CPU overhead | Zero | Small per-villager tracking cost (index lookups + event handling) |

## Limitations

**In its current form, the practical performance gain is limited.** Vanilla's passive discovery is already "good enough" for most scenarios because:

- Villager pathfinding frequency is low (triggered by AI goals, not every tick)
- Paths are typically short — the villager reaches the obstacle in a few seconds anyway
- The tracking overhead (memory + event handling) may offset the time saved

This mod is best understood as **infrastructure for future optimizations**, not a standalone performance boost.

## Roadmap

The dependency-tracking system built here can serve as a foundation for genuinely impactful optimizations:

- **Path caching** — Reuse paths with identical start/end points instead of recomputing A\*
- **Batch pathfinding** — Limit concurrent path computations per tick to smooth CPU spikes
- **Async pathfinding** — Spread A\* computation across multiple ticks
- **Predictive invalidation** — Anticipate which block changes will affect in-flight villagers

## Installation

### For Players

1. Install [NeoForge 1.21](https://neoforged.net/) (version 21.0.167+)
2. Download `villager_path_opt-0.1.0.jar` from [Releases](https://github.com/simple-calcate/villager-path-optimization/releases)
3. Place the JAR in your `.minecraft/mods/` folder
4. Launch the game

**Server-side only** — clients don't need to install this mod. Works in singleplayer, LAN, and dedicated servers. Whoever hosts the world/server needs the mod installed.

### For Developers

```bash
git clone https://github.com/simple-calcate/villager-path-optimization.git
cd villager-path-optimization

# Requires JDK 21
./gradlew build
# On Windows: gradlew.bat build
```

Build output: `build/libs/villager_path_opt-0.1.0.jar`

## Configuration

Config file: `.minecraft/config/villager_path_opt-common.toml`

| Option | Default | Range | Description |
|--------|---------|-------|-------------|
| `enableVillagerPathTracking` | `true` | — | Master switch |
| `maxDependenciesPerPath` | `128` | 1–1024 | Max blocks tracked per path; excess is silently dropped |
| `maxWakeupsPerTick` | `16` | 1–256 | Max wakeups processed per tick; prevents lag spikes from mass block changes |
| `pathStateTtlTicks` | `1200` | 100–6000 | Time-to-live in ticks (1200 = 60s); stale states auto-cleaned |
| `trackOnlyVillagers` | `true` | — | Only track Villager entities (not all PathfinderMobs) |
| `debugLogging` | `false` | — | Verbose logging: path registration, wakeups, TTL cleanup |

## Verifying It Works

1. Set `debugLogging = true` in the config file
2. Launch the game and enter a world with villagers
3. Check `logs/latest.log` for:
   - `Path registered: villager=..., version=..., deps=...` — when a villager starts pathfinding
   - `Wakeup enqueued: villager=..., reason=BLOCK_BROKEN, block=...` — when you break a block on a villager's path
   - `Path cleared: villager=..., reason=PATH_COMPLETED` — when a villager finishes walking
4. Break a block in front of a walking villager — the villager should re-evaluate its path immediately instead of walking into the gap

## Architecture

```
com.aiwork.vpo
├── VillagerPathOptMod.java            # @Mod entrypoint, config & event registration
├── config/
│   └── ModConfig.java                 # NeoForge ModConfigSpec (6 options)
├── tracking/
│   ├── BlockDependencyKey.java        # Dimension + packed pos — unique block identity
│   ├── VillagerPathState.java         # Per-villager state: version, deps, TTL timestamps
│   ├── PathDependencyIndex.java       # Bidirectional index: villager↔blocks
│   ├── PathDependencyTracker.java     # Core coordinator (singleton), manages wakeup queue
│   ├── WakeupRecord.java              # Pending wakeup record (villager, reason, block, time)
│   └── WakeupReason.java              # Enum: BLOCK_BROKEN, PATH_COMPLETED, TTL_EXPIRED, ...
├── util/
│   ├── PathNodeExtractor.java         # Extract floor/support/head blocks from Path nodes
│   └── WalkabilityProbe.java          # Block type comparison for walkability change detection
├── mixin/
│   └── PathNavigationMixin.java       # @Inject moveTo/stop/tick on PathNavigation (Villager only)
└── event/
    ├── BlockChangeEventHandler.java   # BreakEvent, EntityPlaceEvent, NeighborNotifyEvent
    ├── EntityLifecycleEventHandler.java # EntityLeaveLevel, LivingDeath, LevelUnload
    └── ServerTickHandler.java         # ServerTickEvent.Post: TTL cleanup + batch wakeup drain
```

### Flow

```
Villager calls moveTo(Path, speed)
  ↓
PathNavigationMixin intercepts (RETURN)
  ↓
PathNodeExtractor extracts: floor + below + above for each node
  ↓
PathDependencyIndex registers: villager→blocks, block→villagers
  ↓
── Block change happens (break/place/neighbor) ──
  ↓
BlockChangeEventHandler fires → index lookup: who depends on this block?
  ↓
Enqueue WakeupRecord for each affected villager
  ↓
ServerTickHandler.Post (every tick)
  ↓
Drain pending wakeups (batch of maxWakeupsPerTick)
  ↓
For each: find villager entity → verify path version → call navigation.stop()
  ↓
Vanilla AI re-evaluates on next tick (gentle wakeup, no forced recompute)
  ↓
── Cleanup ──
  ↓
TTL expiry (pathStateTtlTicks) → remove stale states
Path completion (isDone()) → clear dependencies
Villager death/unload → clear all state for that villager
```

## Tech Stack

- Minecraft 1.21
- NeoForge 21.0.167
- ModDevGradle 2.0.141
- Mixin (via NeoForge)
- JDK 21
- Gradle 9.x

## License

MIT
