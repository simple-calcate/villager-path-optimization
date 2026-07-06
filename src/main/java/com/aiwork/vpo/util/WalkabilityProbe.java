package com.aiwork.vpo.util;

import net.minecraft.world.level.block.state.BlockState;

/**
 * 通行性判断工具。
 * <p>
 * 第一阶段采用简单策略：方块状态变化即视为可能影响通行。
 * 主要过滤由 {@link com.aiwork.vpo.tracking.PathDependencyIndex} 的反向索引完成——
 * 只有路径依赖的方块变化才会触发唤醒。
 * 后续可扩展为碰撞箱对比、门/栅栏门状态检测等。
 */
public final class WalkabilityProbe {

    private WalkabilityProbe() {
    }

    /**
     * 判断方块状态变化是否可能影响通行性。
     *
     * @param oldState 旧方块状态
     * @param newState 新方块状态
     * @return true 表示可能影响通行
     */
    public static boolean mayAffectWalkability(BlockState oldState, BlockState newState) {
        if (oldState == newState) return false;
        if (oldState == null || newState == null) return true;
        // 第一阶段：方块类型变化即视为可能影响
        // 后续可加入碰撞箱对比等更精确的判断
        return oldState.getBlock() != newState.getBlock();
    }
}
