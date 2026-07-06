# Villager Path Optimization

A NeoForge mod for Minecraft 1.21 that optimizes villager pathfinding by tracking block dependencies along their navigation paths.

## Problem

Vanilla villagers recompute paths frequently, even when nothing along their route has changed. This wastes CPU cycles, especially in villages with many villagers.

## Solution

Instead of replacing the vanilla pathfinding AI, this mod takes a **passive tracking** approach:

1. **Track** — When a villager computes a path, the blocks along that path (floor, support, head-level) are registered as dependencies.
2. **Watch** — Block changes (break/place/neighbor notify) are monitored. If a changed block is on a villager's active path, that villager is enqueued for wakeup.
3. **Wake** — Each tick, a batch of villagers is gently woken by calling `navigation.stop()`, letting the vanilla AI re-evaluate on the next tick.
4. **Clean** — TTL cleanup (default 60s) prevents stale states from leaking memory.

This means villagers only recompute paths when something actually changes along their route — not on every arbitrary timer.

## Installation

### For Players

1. Install [NeoForge 1.21](https://neoforged.net/) (version 21.0.167+)
2. Download `villager_path_opt-0.1.0.jar` from [Releases](../../releases)
3. Place the JAR in your `.minecraft/mods/` folder
4. Launch the game

**Server-side only** — clients don't need to install this mod. Works in singleplayer, LAN, and dedicated servers.

### For Developers

```bash
git clone https://github.com/simple-calcate/villager-path-optimization.git
cd villager-path-optimization
gradle build --no-daemon
```

Requirements: JDK 21, Gradle 9.x

## Configuration

Config file: `.minecraft/config/villager_path_opt-common.toml`

| Option | Default | Description |
|--------|---------|-------------|
| `enableVillagerPathTracking` | `true` | Master switch |
| `maxDependenciesPerPath` | `128` | Max blocks tracked per path (1–1024) |
| `maxWakeupsPerTick` | `16` | Max wakeups processed per tick (1–256) |
| `pathStateTtlTicks` | `1200` | Time-to-live in ticks (100–6000, 1200 = 60s) |
| `trackOnlyVillagers` | `true` | Only track Villagers (not all PathfinderMobs) |
| `debugLogging` | `false` | Verbose logging for debugging |

## Architecture

```
com.aiwork.vpo
├── VillagerPathOptMod.java       # Mod entrypoint, config & event registration
├── config/
│   └── ModConfig.java             # NeoForge ModConfigSpec (6 options)
├── tracking/
│   ├── BlockDependencyKey.java    # Dimension + packed pos key
│   ├── VillagerPathState.java     # Per-villager path state (version, deps, TTL)
│   ├── PathDependencyIndex.java   # Bidirectional index: villager↔blocks
│   ├── PathDependencyTracker.java # Core coordinator (singleton)
│   ├── WakeupRecord.java          # Pending wakeup record
│   └── WakeupReason.java          # Enum: BLOCK_BROKEN, PATH_COMPLETED, ...
├── util/
│   ├── PathNodeExtractor.java     # Extract floor/support/head blocks from Path
│   └── WalkabilityProbe.java      # Simple walkability change detection
├── mixin/
│   └── PathNavigationMixin.java   # Hook moveTo/stop/tick on PathNavigation
└── event/
    ├── BlockChangeEventHandler.java   # BreakEvent, EntityPlaceEvent, NeighborNotifyEvent
    ├── EntityLifecycleEventHandler.java # EntityLeaveLevel, LivingDeath, LevelUnload
    └── ServerTickHandler.java      # ServerTickEvent.Post: TTL cleanup + batch wakeup
```

## How It Works

```
Villager starts pathfinding
    ↓
Mixin hooks moveTo(Path, speed)
    ↓
PathNodeExtractor extracts block dependencies
    ↓
PathDependencyIndex registers: villager → blocks, block → villagers
    ↓
  ┌─────────────────────────────────┐
  │ Player breaks a block on path   │
  │   ↓                             │
  │ BlockChangeEventHandler fires   │
  │   ↓                             │
  │ Index lookup: who depends on it?│
  │   ↓                             │
  │ Enqueue wakeup for that villager│
  └─────────────────────────────────┘
    ↓
ServerTickHandler.Post (every tick)
    ↓
Drain pending wakeups (batch of 16)
    ↓
For each: call navigation.stop()
    ↓
Vanilla AI re-evaluates next tick
```

## License

MIT

## Tech Stack

- Minecraft 1.21
- NeoForge 21.0.167
- Mixin 0.8.6 (via SpongePowered)
- JDK 21
- Gradle 9.x + ModDevGradle 2.0.141
