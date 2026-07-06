package com.aiwork.vpo.tracking;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * 路径依赖双向索引。
 * <ul>
 *   <li>{@code villagerId -> VillagerPathState}：村民当前路径状态与依赖方块集合</li>
 *   <li>{@code blockKey -> Set<villagerId>}：方块被哪些村民的路径依赖</li>
 * </ul>
 * 所有操作应在服务端主线程执行。
 */
public class PathDependencyIndex {

    private final Map<UUID, VillagerPathState> villagerStates = new HashMap<>();
    private final Map<BlockDependencyKey, Set<UUID>> blockToVillagers = new HashMap<>();

    // ---- 查询 ----

    public VillagerPathState getVillagerState(UUID villagerId) {
        return villagerStates.get(villagerId);
    }

    public Set<UUID> getVillagersDependingOn(BlockDependencyKey blockKey) {
        Set<UUID> set = blockToVillagers.get(blockKey);
        return set == null ? Collections.emptySet() : set;
    }

    public int getTrackedVillagerCount() {
        return villagerStates.size();
    }

    public int getTotalBlockDependencies() {
        return blockToVillagers.size();
    }

    // ---- 注册 ----

    /**
     * 为村民注册一条新路径状态。若村民已有旧状态，会先清除旧依赖。
     */
    public VillagerPathState registerVillagerPath(UUID villagerId, ResourceKey<Level> dimension,
                                                   int pathVersion, long gameTime) {
        // 清除旧路径依赖
        removeVillager(villagerId);

        VillagerPathState state = new VillagerPathState(villagerId, dimension, pathVersion, gameTime);
        villagerStates.put(villagerId, state);
        return state;
    }

    /**
     * 向村民当前路径添加一个方块依赖。
     */
    public void addDependency(UUID villagerId, BlockDependencyKey blockKey) {
        VillagerPathState state = villagerStates.get(villagerId);
        if (state == null) return;

        state.dependencies().add(blockKey);
        blockToVillagers.computeIfAbsent(blockKey, k -> new HashSet<>()).add(villagerId);
    }

    // ---- 清除 ----

    /**
     * 移除村民的所有路径依赖。
     */
    public void removeVillager(UUID villagerId) {
        VillagerPathState state = villagerStates.remove(villagerId);
        if (state == null) return;

        for (BlockDependencyKey blockKey : state.dependencies()) {
            Set<UUID> villagers = blockToVillagers.get(blockKey);
            if (villagers != null) {
                villagers.remove(villagerId);
                if (villagers.isEmpty()) {
                    blockToVillagers.remove(blockKey);
                }
            }
        }
    }

    /**
     * 清除指定维度的所有追踪状态（维度卸载时调用）。
     */
    public void clearDimension(ResourceKey<Level> dimension) {
        var iterator = villagerStates.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            VillagerPathState state = entry.getValue();
            if (state.dimension().equals(dimension)) {
                for (BlockDependencyKey blockKey : state.dependencies()) {
                    Set<UUID> villagers = blockToVillagers.get(blockKey);
                    if (villagers != null) {
                        villagers.remove(entry.getKey());
                        if (villagers.isEmpty()) {
                            blockToVillagers.remove(blockKey);
                        }
                    }
                }
                iterator.remove();
            }
        }
    }

    /**
     * 清空所有追踪状态（服务器停止时调用）。
     */
    public void clearAll() {
        villagerStates.clear();
        blockToVillagers.clear();
    }

    /**
     * 清除所有过期的路径状态（TTL 兜底）。
     */
    public int cleanupExpired(long currentGameTime, long ttlTicks) {
        int cleaned = 0;
        var iterator = villagerStates.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            VillagerPathState state = entry.getValue();
            if (state.isExpired(currentGameTime, ttlTicks)) {
                state.markInvalid(WakeupReason.TTL_EXPIRED);
                for (BlockDependencyKey blockKey : state.dependencies()) {
                    Set<UUID> villagers = blockToVillagers.get(blockKey);
                    if (villagers != null) {
                        villagers.remove(entry.getKey());
                        if (villagers.isEmpty()) {
                            blockToVillagers.remove(blockKey);
                        }
                    }
                }
                iterator.remove();
                cleaned++;
            }
        }
        return cleaned;
    }

    public Map<UUID, VillagerPathState> allStates() {
        return Collections.unmodifiableMap(villagerStates);
    }
}
