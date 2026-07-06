package com.aiwork.vpo.event;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aiwork.vpo.config.ModConfig;
import com.aiwork.vpo.tracking.PathDependencyTracker;
import com.aiwork.vpo.tracking.VillagerPathState;
import com.aiwork.vpo.tracking.WakeupReason;
import com.aiwork.vpo.tracking.WakeupRecord;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.npc.Villager;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 服务端 tick 处理器。
 * <p>
 * 每个服务端 tick 结束后：
 * <ol>
 *   <li>执行 TTL 兜底清理</li>
 *   <li>批处理待处理的唤醒记录</li>
 *   <li>对需要重新评估路径的村民调用 {@code navigation.stop()}，让原版 AI 下一 tick 重新决策</li>
 * </ol>
 */
public class ServerTickHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("VillagerPathOpt");

    @SubscribeEvent
    public void onServerTickPost(ServerTickEvent.Post event) {
        PathDependencyTracker tracker = PathDependencyTracker.get();
        if (tracker == null) return;

        MinecraftServer server = event.getServer();

        // 1. TTL 兜底清理
        long gameTime = server.overworld().getGameTime();
        tracker.cleanupExpired(gameTime);

        // 2. 批处理唤醒
        int maxPerTick = ModConfig.maxWakeupsPerTick.get();
        List<WakeupRecord> wakeups = tracker.drainPendingWakeups(maxPerTick);

        for (WakeupRecord wakeup : wakeups) {
            processWakeup(server, tracker, wakeup);
        }
    }

    /**
     * 处理单条唤醒记录。
     * 温和策略：只停止当前导航，让原版 AI 下一 tick 重新决策。
     */
    private void processWakeup(MinecraftServer server, PathDependencyTracker tracker, WakeupRecord wakeup) {
        // 查找村民实体
        ServerLevel level = server.getLevel(wakeup.dimension());
        if (level == null) {
            tracker.onVillagerRemoved(wakeup.villagerId());
            return;
        }

        Entity entity = level.getEntity(wakeup.villagerId());
        if (!(entity instanceof Villager villager)) {
            tracker.onVillagerRemoved(wakeup.villagerId());
            return;
        }

        // 检查路径版本是否仍匹配
        VillagerPathState state = tracker.getState(wakeup.villagerId());
        if (state == null || state.pathVersion() != wakeup.pathVersion()) {
            return;
        }

        // 检查村民是否仍在导航
        PathNavigation navigation = villager.getNavigation();
        if (navigation.isDone()) {
            // 路径已完成，清理依赖
            tracker.onPathCompleted(wakeup.villagerId(), WakeupReason.PATH_COMPLETED);
            return;
        }

        // 温和唤醒：停止当前路径，让原版 AI 下一 tick 重新决策
        // stop() 会触发 mixin 的 stop hook，清理旧依赖
        // 如果原版 AI 下一 tick 决定重新寻路，moveTo hook 会注册新依赖
        if (ModConfig.debugLogging.get()) {
            LOGGER.info("Wakeup processed: villager={}, reason={}, stopping navigation",
                    wakeup.villagerId(), wakeup.reason());
        }

        navigation.stop();
    }
}
