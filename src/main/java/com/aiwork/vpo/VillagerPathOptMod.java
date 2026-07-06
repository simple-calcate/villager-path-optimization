package com.aiwork.vpo;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.aiwork.vpo.config.ModConfig;
import com.aiwork.vpo.event.BlockChangeEventHandler;
import com.aiwork.vpo.event.EntityLifecycleEventHandler;
import com.aiwork.vpo.event.ServerTickHandler;
import com.aiwork.vpo.tracking.PathDependencyTracker;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod(VillagerPathOptMod.MODID)
public class VillagerPathOptMod {

    public static final String MODID = "villager_path_opt";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VillagerPathOptMod(IEventBus modEventBus, ModContainer modContainer) {
        // 注册配置
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, ModConfig.SPEC);

        // 注册 NeoForge 事件处理器
        NeoForge.EVENT_BUS.register(new BlockChangeEventHandler());
        NeoForge.EVENT_BUS.register(new EntityLifecycleEventHandler());
        NeoForge.EVENT_BUS.register(new ServerTickHandler());
        NeoForge.EVENT_BUS.register(this);

        LOGGER.info("Villager Path Optimization mod loaded");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        PathDependencyTracker.init();
        LOGGER.info("PathDependencyTracker initialized");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        PathDependencyTracker tracker = PathDependencyTracker.get();
        if (tracker != null) {
            tracker.onServerStop();
            LOGGER.info("PathDependencyTracker cleaned up");
        }
    }
}
