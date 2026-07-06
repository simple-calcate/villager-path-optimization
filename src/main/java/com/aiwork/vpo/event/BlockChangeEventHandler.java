package com.aiwork.vpo.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import com.aiwork.vpo.tracking.BlockDependencyKey;
import com.aiwork.vpo.tracking.PathDependencyTracker;
import com.aiwork.vpo.tracking.WakeupReason;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * 监听方块变化事件，将变化通知路径依赖追踪器。
 * <p>
 * 只有被路径依赖的方块变化才会触发唤醒，非依赖方块变化会被追踪器的反向索引过滤。
 */
public class BlockChangeEventHandler {

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        handleBlockChange(event.getLevel(), event.getPos(), WakeupReason.BLOCK_BROKEN);
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        handleBlockChange(event.getLevel(), event.getPos(), WakeupReason.BLOCK_PLACED);
    }

    @SubscribeEvent
    public void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        handleBlockChange(event.getLevel(), event.getPos(), WakeupReason.NEIGHBOR_NOTIFY);
    }

    private void handleBlockChange(LevelAccessor levelAccessor, BlockPos pos, WakeupReason reason) {
        if (levelAccessor.isClientSide()) return;
        if (!(levelAccessor instanceof Level level)) return;

        PathDependencyTracker tracker = PathDependencyTracker.get();
        if (tracker == null) return;

        BlockDependencyKey key = new BlockDependencyKey(level.dimension(), pos);
        tracker.onBlockChanged(key, reason, level.getGameTime());
    }
}
