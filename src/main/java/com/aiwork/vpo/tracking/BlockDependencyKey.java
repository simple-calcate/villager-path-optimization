package com.aiwork.vpo.tracking;

import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * 唯一标识一个被路径依赖的方块位置。
 * 包含维度信息，避免不同维度坐标冲突。
 */
public final class BlockDependencyKey {

    private final ResourceKey<Level> dimension;
    private final long packedPos;

    public BlockDependencyKey(ResourceKey<Level> dimension, BlockPos pos) {
        this.dimension = dimension;
        this.packedPos = pos.asLong();
    }

    public BlockDependencyKey(ResourceKey<Level> dimension, long packedPos) {
        this.dimension = dimension;
        this.packedPos = packedPos;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public long packedPos() {
        return packedPos;
    }

    public BlockPos pos() {
        return BlockPos.of(packedPos);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockDependencyKey other)) return false;
        return packedPos == other.packedPos && Objects.equals(dimension, other.dimension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, packedPos);
    }

    @Override
    public String toString() {
        return "BlockDepKey[" + dimension.location() + "@" + BlockPos.of(packedPos) + "]";
    }
}
