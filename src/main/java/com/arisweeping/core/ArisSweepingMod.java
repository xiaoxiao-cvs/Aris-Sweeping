package com.arisweeping.core;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

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
    
    // 全局单例管理器 - 延迟初始化
    private static volatile com.arisweeping.async.AsyncTaskManager taskManager;
    private static volatile com.arisweeping.data.ConfigData configData;
    private static volatile com.arisweeping.tasks.SmartTaskManager smartTaskManager;
    
    public ArisSweepingMod() {
        // 输出启动横幅
        ArisLogger.printStartupBanner();
        ArisLogger.logStartupPhase("INIT", "正在初始化 ArisSweeping 模组...");
        
        // 注册模组事件总线
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        
        // 注册Forge事件总线
        MinecraftForge.EVENT_BUS.register(this);
        
        ArisLogger.info("ArisSweeping 模组初始化完成");
    }
    
    /**
     * 通用设置阶段 - 模组加载时执行
     */
    private void commonSetup(final FMLCommonSetupEvent event) {
        long startTime = System.currentTimeMillis();
        ArisLogger.logStartupPhase("SETUP", "开始通用设置阶段...");
        
        event.enqueueWork(() -> {
            // 使用新的初始化管理器
            ArisLogger.info("正在初始化模组系统...");
            ModInitializer.initializeAll();
            
            long duration = System.currentTimeMillis() - startTime;
            ArisLogger.logStartupSuccess("通用设置", duration);
        });
    }
    
    /**
     * 服务器启动事件 - 初始化服务端组件
     */
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        long startTime = System.currentTimeMillis();
        ArisLogger.logStartupPhase("SERVER", "服务器启动，初始化服务端组件...");
        
        try {
            // 初始化异步任务管理器
            ArisLogger.info("正在初始化异步任务管理器...");
            taskManager = new com.arisweeping.async.AsyncTaskManager();
            
            // 初始化智能任务管理器
            ArisLogger.info("正在初始化智能任务管理器...");
            smartTaskManager = new com.arisweeping.tasks.SmartTaskManager();
            
            // 初始化配置数据
            ArisLogger.info("正在加载配置数据...");
            configData = new com.arisweeping.data.ConfigData();
            
            // 启动清理任务调度器
            scheduleCleaningTasks();
            
            long duration = System.currentTimeMillis() - startTime;
            ArisLogger.logStartupSuccess("ArisSweeping 服务端组件", duration);
        } catch (Exception e) {
            ArisLogger.logStartupFailure("ArisSweeping 服务端组件", e);
        }
    }
    
    /**
     * 启动清理任务调度器
     */
    private static void scheduleCleaningTasks() {
        if (taskManager != null && smartTaskManager != null) {
            ArisLogger.debug("正在启动清理任务调度器...");
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
        ArisLogger.debug("配置数据已更新");
    }
}