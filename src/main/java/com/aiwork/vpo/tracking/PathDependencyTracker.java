package com.aiwork.vpo.tracking;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import com.aiwork.vpo.config.ModConfig;

/**
 * 路径依赖追踪器——模组核心协调器。
 * <p>
 * 管理 {@link PathDependencyIndex} 双向索引与待处理唤醒队列。
 * 所有公开方法应在服务端主线程调用。
 */
public class PathDependencyTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger("VillagerPathOpt");

    private static PathDependencyTracker instance;

    private final PathDependencyIndex index = new PathDependencyIndex();
    private final Map<UUID, WakeupRecord> pendingWakeups = new LinkedHashMap<>();

    private int pathVersionCounter = 0;

    private PathDependencyTracker() {
    }

    public static void init() {
        instance = new PathDependencyTracker();
    }

    public static PathDependencyTracker get() {
        return instance;
    }

    // ---- 路径生命周期 ----

    /**
     * 村民获得新路径时调用。清除旧路径依赖，注册新路径依赖。
     */
    public void onPathCreated(UUID villagerId, ResourceKey<Level> dimension,
                              long gameTime, Set<BlockDependencyKey> dependencies) {
        if (!ModConfig.enableVillagerPathTracking.get()) return;

        int version = ++pathVersionCounter;
        VillagerPathState state = index.registerVillagerPath(villagerId, dimension, version, gameTime);

        int count = 0;
        int maxDeps = ModConfig.maxDependenciesPerPath.get();
        for (BlockDependencyKey dep : dependencies) {
            if (count >= maxDeps) {
                LOGGER.debug("Villager {} path exceeded max dependencies {}, stop tracking", villagerId, maxDeps);
                break;
            }
            index.addDependency(villagerId, dep);
            count++;
        }

        pendingWakeups.remove(villagerId);

        if (ModConfig.debugLogging.get()) {
            LOGGER.info("Path registered: villager={}, version={}, deps={}", villagerId, version, count);
        }
    }

    /**
     * 村民路径停止或完成时调用。清除该村民的所有路径依赖。
     */
    public void onPathCompleted(UUID villagerId, WakeupReason reason) {
        VillagerPathState state = index.getVillagerState(villagerId);
        if (state == null) return;

        state.markInvalid(reason);
        index.removeVillager(villagerId);
        pendingWakeups.remove(villagerId);

        if (ModConfig.debugLogging.get()) {
            LOGGER.info("Path cleared: villager={}, reason={}", villagerId, reason);
        }
    }

    /**
     * 村民死亡、卸载、换维度时调用。
     */
    public void onVillagerRemoved(UUID villagerId) {
        index.removeVillager(villagerId);
        pendingWakeups.remove(villagerId);
    }

    // ---- 方块变化 ----

    /**
     * 方块发生变化时调用。若该方块被某村民路径依赖，则将村民加入唤醒队列。
     */
    public void onBlockChanged(BlockDependencyKey blockKey, WakeupReason reason, long gameTime) {
        if (!ModConfig.enableVillagerPathTracking.get()) return;

        Set<UUID> affectedVillagers = index.getVillagersDependingOn(blockKey);
        if (affectedVillagers.isEmpty()) return;

        for (UUID villagerId : affectedVillagers) {
            VillagerPathState state = index.getVillagerState(villagerId);
            if (state == null || state.invalidated()) continue;

            pendingWakeups.put(villagerId, new WakeupRecord(
                    villagerId, state.dimension(), state.pathVersion(),
                    reason, blockKey, gameTime
            ));

            if (ModConfig.debugLogging.get()) {
                LOGGER.info("Wakeup enqueued: villager={}, reason={}, block={}", villagerId, reason, blockKey);
            }
        }
    }

    // ---- Tick 处理 ----

    /**
     * 每个服务端 tick 调用。取出待处理唤醒并返回。
     */
    public List<WakeupRecord> drainPendingWakeups(int maxPerTick) {
        List<WakeupRecord> result = new ArrayList<>(Math.min(maxPerTick, pendingWakeups.size()));
        var iterator = pendingWakeups.entrySet().iterator();
        while (iterator.hasNext() && result.size() < maxPerTick) {
            result.add(iterator.next().getValue());
            iterator.remove();
        }
        return result;
    }

    /**
     * TTL 兜底清理。
     */
    public int cleanupExpired(long currentGameTime) {
        long ttl = ModConfig.pathStateTtlTicks.get();
        int cleaned = index.cleanupExpired(currentGameTime, ttl);

        pendingWakeups.entrySet().removeIf(entry -> {
            VillagerPathState state = index.getVillagerState(entry.getKey());
            return state == null || state.invalidated();
        });

        if (cleaned > 0 && ModConfig.debugLogging.get()) {
            LOGGER.info("TTL cleanup: removed {} expired path states", cleaned);
        }
        return cleaned;
    }

    // ---- 维度/服务器生命周期 ----

    public void onDimensionUnload(ResourceKey<Level> dimension) {
        index.clearDimension(dimension);
        pendingWakeups.entrySet().removeIf(entry -> entry.getValue().dimension().equals(dimension));
    }

    public void onServerStop() {
        index.clearAll();
        pendingWakeups.clear();
        pathVersionCounter = 0;
    }

    // ---- 调试信息 ----

    public int getTrackedVillagerCount() {
        return index.getTrackedVillagerCount();
    }

    public int getTotalBlockDependencies() {
        return index.getTotalBlockDependencies();
    }

    public int getPendingWakeupCount() {
        return pendingWakeups.size();
    }

    public VillagerPathState getState(UUID villagerId) {
        return index.getVillagerState(villagerId);
    }

    public void clearAllDebug() {
        index.clearAll();
        pendingWakeups.clear();
    }
}
