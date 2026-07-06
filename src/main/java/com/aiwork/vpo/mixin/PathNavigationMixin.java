package com.aiwork.vpo.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.aiwork.vpo.tracking.BlockDependencyKey;
import com.aiwork.vpo.tracking.PathDependencyTracker;
import com.aiwork.vpo.tracking.WakeupReason;
import com.aiwork.vpo.util.PathNodeExtractor;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;

/**
 * Mixin 注入 {@link PathNavigation}，在路径创建、停止、完成时通知追踪器。
 * <p>
 * 只对 {@link Villager} 实体生效。
 */
@Mixin(PathNavigation.class)
public abstract class PathNavigationMixin {

    @Shadow
    @Final
    protected Mob mob;

    @Shadow
    public abstract boolean isDone();

    /**
     * 路径设置完成后，提取路径节点并注册依赖。
     */
    @Inject(method = "moveTo(Lnet/minecraft/world/level/pathfinder/Path;D)Z", at = @At("RETURN"))
    private void vpo$onPathSet(Path path, double speed, CallbackInfoReturnable<Boolean> cir) {
        if (path == null) return;
        if (!(this.mob instanceof Villager villager)) return;
        if (this.mob.level().isClientSide()) return;

        ResourceKey<Level> dim = this.mob.level().dimension();
        long gameTime = this.mob.level().getGameTime();
        Set<BlockDependencyKey> deps = PathNodeExtractor.extractDependencies(path, dim);

        PathDependencyTracker.get().onPathCreated(villager.getUUID(), dim, gameTime, deps);
    }

    /**
     * 导航停止时清除路径依赖。
     */
    @Inject(method = "stop", at = @At("RETURN"))
    private void vpo$onStop(CallbackInfo ci) {
        if (!(this.mob instanceof Villager villager)) return;
        if (this.mob.level().isClientSide()) return;

        PathDependencyTracker.get().onPathCompleted(villager.getUUID(), WakeupReason.PATH_REPLACED);
    }

    /**
     * 每 tick 结束时检查路径是否已完成，若完成则清除依赖。
     */
    @Inject(method = "tick", at = @At("RETURN"))
    private void vpo$onTickEnd(CallbackInfo ci) {
        if (!(this.mob instanceof Villager villager)) return;
        if (this.mob.level().isClientSide()) return;

        if (this.isDone()) {
            PathDependencyTracker.get().onPathCompleted(villager.getUUID(), WakeupReason.PATH_COMPLETED);
        }
    }
}
