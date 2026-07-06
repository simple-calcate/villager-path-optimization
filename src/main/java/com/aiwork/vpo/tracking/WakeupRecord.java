package com.aiwork.vpo.tracking;

import java.util.UUID;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * 一条待处理的唤醒记录。
 * 村民被标记为"需要重新评估路径"后，会在下一个 server tick 被批处理。
 */
public record WakeupRecord(
        UUID villagerId,
        ResourceKey<Level> dimension,
        int pathVersion,
        WakeupReason reason,
        BlockDependencyKey changedBlock,
        long enqueueGameTime
) {
}
