package com.aiwork.vpo.util;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

import com.aiwork.vpo.tracking.BlockDependencyKey;

/**
 * 从 {@link Path} 对象中提取路径依赖的方块位置。
 * <p>
 * 第一阶段记录：
 * <ul>
 *   <li>路径节点脚下方块（实体站立位置）</li>
 *   <li>脚下方块下方（支撑方块）</li>
 *   <li>头部位置方块（实体占用空间上半部分）</li>
 * </ul>
 */
public final class PathNodeExtractor {

    private PathNodeExtractor() {
    }

    /**
     * 从路径中提取方块依赖集合。
     *
     * @param path      路径对象
     * @param dimension 所在维度
     * @return 依赖方块集合，若 path 为 null 则返回空集合
     */
    public static Set<BlockDependencyKey> extractDependencies(Path path, ResourceKey<Level> dimension) {
        Set<BlockDependencyKey> deps = new HashSet<>();
        if (path == null) return deps;

        int nodeCount = path.getNodeCount();
        for (int i = 0; i < nodeCount; i++) {
            Node node = path.getNode(i);
            if (node == null) continue;

            BlockPos feetPos = new BlockPos(node.x, node.y, node.z);

            // 脚下方块（实体站立位置）
            deps.add(new BlockDependencyKey(dimension, feetPos));
            // 支撑方块（脚下一格）
            deps.add(new BlockDependencyKey(dimension, feetPos.below()));
            // 头部方块（脚上一格）
            deps.add(new BlockDependencyKey(dimension, feetPos.above()));
        }

        return deps;
    }
}
