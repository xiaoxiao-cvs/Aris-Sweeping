package com.arisweeping.core;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * ArisSweeping - 智能实体清理模组
 * 
 * 主要功能：
 * - 异步清理掉落物和过密畜牧实体
 * - 智能任务管理和撤销系统
 * - 游戏内可视化配置界面
 * - 高性能多线程处理架构
 */
@Mod(ArisSweepingMod.MODID)
public class ArisSweepingMod {
    public static final String MODID = "arisweeping";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    // 全局单例管理器 - 延迟初始化
    private static volatile com.arisweeping.async.AsyncTaskManager taskManager;
    private static volatile com.arisweeping.data.ConfigData configData;
    private static volatile com.arisweeping.tasks.SmartTaskManager smartTaskManager;
    
    public ArisSweepingMod() {
        LOGGER.info("Initializing ArisSweeping mod...");
        
        // 注册模组事件总线
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        
        // 注册Forge事件总线
        MinecraftForge.EVENT_BUS.register(this);
        
        LOGGER.info("ArisSweeping mod initialization completed.");
    }
    
    /**
     * 通用设置阶段 - 模组加载时执行
     */
    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Starting ArisSweeping common setup...");
        
        event.enqueueWork(() -> {
            // 初始化配置系统
            ModConfig.initialize();
            
            // 注册网络包处理器
            // NetworkRegistry.initialize();
            
            LOGGER.info("ArisSweeping common setup completed.");
        });
    }
    
    /**
     * 服务器启动事件 - 初始化服务端组件
     */
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Server starting, initializing ArisSweeping server components...");
        
        try {
            // 初始化异步任务管理器
            taskManager = new com.arisweeping.async.AsyncTaskManager();
            
            // 初始化智能任务管理器
            smartTaskManager = new com.arisweeping.tasks.SmartTaskManager();
            
            // 初始化配置数据
            configData = new com.arisweeping.data.ConfigData();
            
            // 启动清理任务调度器
            scheduleCleaningTasks();
            
            LOGGER.info("ArisSweeping server components initialized successfully.");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize ArisSweeping server components", e);
        }
    }
    
    /**
     * 启动清理任务调度器
     */
    private static void scheduleCleaningTasks() {
        if (taskManager != null && smartTaskManager != null) {
            LOGGER.debug("Starting cleaning task scheduler...");
            // 这里将在后续实现具体的调度逻辑
            // smartTaskManager.startPeriodicCleaning();
        }
    }
    
    /**
     * 获取异步任务管理器实例
     */
    public static com.arisweeping.async.AsyncTaskManager getTaskManager() {
        return taskManager;
    }
    
    /**
     * 获取智能任务管理器实例
     */
    public static com.arisweeping.tasks.SmartTaskManager getSmartTaskManager() {
        return smartTaskManager;
    }
    
    /**
     * 获取配置数据实例
     */
    public static com.arisweeping.data.ConfigData getConfigData() {
        return configData;
    }
    
    /**
     * 更新配置数据
     */
    public static void updateConfigData(com.arisweeping.data.ConfigData newConfigData) {
        configData = newConfigData;
        LOGGER.debug("Configuration data updated");
    }
}