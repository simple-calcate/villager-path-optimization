package com.aiwork.vpo.event;

import com.aiwork.vpo.tracking.PathDependencyTracker;

import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * 监听实体与世界生命周期事件，在村民死亡、卸载、维度卸载时清理路径依赖。
 */
public class EntityLifecycleEventHandler {

    @SubscribeEvent
    public void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Villager villager) {
            PathDependencyTracker tracker = PathDependencyTracker.get();
            if (tracker != null) {
                tracker.onVillagerRemoved(villager.getUUID());
            }
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Villager villager) {
            PathDependencyTracker tracker = PathDependencyTracker.get();
            if (tracker != null) {
                tracker.onVillagerRemoved(villager.getUUID());
            }
        }
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            PathDependencyTracker tracker = PathDependencyTracker.get();
            if (tracker != null) {
                tracker.onDimensionUnload(level.dimension());
            }
        }
    }
}
