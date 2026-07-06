package com.aiwork.vpo.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 模组配置。使用 NeoForge ModConfigSpec，所有字段为静态。
 */
public class ModConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue enableVillagerPathTracking = BUILDER
            .comment("Master switch for villager path dependency tracking.")
            .define("enableVillagerPathTracking", true);

    public static final ModConfigSpec.IntValue maxDependenciesPerPath = BUILDER
            .comment("Maximum block dependencies recorded per path.",
                    "If exceeded, the path is not tracked (degraded mode).")
            .defineInRange("maxDependenciesPerPath", 128, 1, 1024);

    public static final ModConfigSpec.IntValue maxWakeupsPerTick = BUILDER
            .comment("Maximum number of wakeups processed per server tick.",
                    "Prevents lag spikes from mass block changes.")
            .defineInRange("maxWakeupsPerTick", 16, 1, 256);

    public static final ModConfigSpec.IntValue pathStateTtlTicks = BUILDER
            .comment("Time-to-live for path states in ticks (1200 = 60 seconds).",
                    "Stale states are cleaned up as a safety net.")
            .defineInRange("pathStateTtlTicks", 1200, 100, 6000);

    public static final ModConfigSpec.BooleanValue trackOnlyVillagers = BUILDER
            .comment("Only track Villager entities, not all PathfinderMobs.")
            .define("trackOnlyVillagers", true);

    public static final ModConfigSpec.BooleanValue debugLogging = BUILDER
            .comment("Enable verbose debug logging for path tracking.")
            .define("debugLogging", false);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
