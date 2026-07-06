package com.aiwork.vpo.tracking;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * 保存单个村民当前路径的追踪状态。
 * <p>
 * 用于判断路径是否已过期、防止旧路径回调污染新路径状态、统一清理依赖。
 */
public class VillagerPathState {

    private final UUID villagerId;
    private final ResourceKey<Level> dimension;
    private final int pathVersion;
    private final Set<BlockDependencyKey> dependencies;
    private final long registeredGameTime;
    private long lastTouchedGameTime;
    private boolean invalidated;
    private WakeupReason wakeupReason;

    public VillagerPathState(UUID villagerId, ResourceKey<Level> dimension, int pathVersion, long gameTime) {
        this.villagerId = villagerId;
        this.dimension = dimension;
        this.pathVersion = pathVersion;
        this.dependencies = new HashSet<>();
        this.registeredGameTime = gameTime;
        this.lastTouchedGameTime = gameTime;
        this.invalidated = false;
        this.wakeupReason = null;
    }

    public UUID villagerId() {
        return villagerId;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public int pathVersion() {
        return pathVersion;
    }

    public Set<BlockDependencyKey> dependencies() {
        return dependencies;
    }

    public long registeredGameTime() {
        return registeredGameTime;
    }

    public long lastTouchedGameTime() {
        return lastTouchedGameTime;
    }

    public void touch(long gameTime) {
        this.lastTouchedGameTime = gameTime;
    }

    public boolean invalidated() {
        return invalidated;
    }

    public void markInvalid(WakeupReason reason) {
        this.invalidated = true;
        this.wakeupReason = reason;
    }

    public WakeupReason wakeupReason() {
        return wakeupReason;
    }

    public boolean isExpired(long currentGameTime, long ttlTicks) {
        return (currentGameTime - registeredGameTime) > ttlTicks;
    }
}
