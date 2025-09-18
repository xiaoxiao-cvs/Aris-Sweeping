package com.arisweeping.core;

import com.arisweeping.data.StatisticsCollector;
import com.arisweeping.network.PacketHandler;

/**
 * 模组初始化管理器
 * 负责协调各个系统的初始化顺序和依赖关系
 */
public class ModInitializer {
    
    private static boolean initialized = false;
    private static final Object lock = new Object();
    
    /**
     * 初始化所有模组系统
     */
    public static void initializeAll() {
        synchronized (lock) {
            if (initialized) {
                ArisLogger.warn("模组系统已经初始化，跳过重复初始化");
                return;
            }
            
            try {
                long totalStartTime = System.currentTimeMillis();
                ArisLogger.logStartupPhase("INIT", "开始初始化Aris Sweeping模组系统...");
                
                // 阶段1：基础功能完善
                initializePhase1();
                
                initialized = true;
                long totalDuration = System.currentTimeMillis() - totalStartTime;
                ArisLogger.logStartupSuccess("Aris Sweeping模组", totalDuration);
                
            } catch (Exception e) {
                ArisLogger.logStartupFailure("模组初始化", e);
                throw new RuntimeException("模组初始化失败", e);
            }
        }
    }
    
    /**
     * 阶段1：基础功能完善
     * - ModConfig类 - 配置系统核心
     * - 网络通信 - 基础数据包和注册  
     * - 统计收集器 - 数据持久化基础
     */
    private static void initializePhase1() {
        ArisLogger.logStartupPhase("PHASE1", "开始阶段1：基础功能完善");
        
        // 1. 初始化配置系统
        try {
            ArisLogger.info("正在初始化配置系统...");
            ModConfig.initialize();
            ArisLogger.info("✓ 配置系统初始化完成");
        } catch (Exception e) {
            ArisLogger.error("✗ 配置系统初始化失败", e);
            throw e;
        }
        
        // 2. 初始化网络通信
        try {
            ArisLogger.info("正在初始化网络通信系统...");
            PacketHandler.register();
            ArisLogger.info("✓ 网络通信系统初始化完成");
        } catch (Exception e) {
            ArisLogger.error("✗ 网络通信系统初始化失败", e);
            throw e;
        }
        
        // 3. 初始化统计收集器
        try {
            ArisLogger.info("正在初始化统计收集器...");
            StatisticsCollector.initialize();
            
            // 记录模组启动事件
            StatisticsCollector collector = StatisticsCollector.getInstance();
            collector.recordEvent("mod_startup", "system");
            collector.incrementCounter("mod_startups");
            
            ArisLogger.info("✓ 统计收集器初始化完成");
        } catch (Exception e) {
            ArisLogger.error("✗ 统计收集器初始化失败", e);
            throw e;
        }
        
        ArisLogger.info("阶段1基础功能完善 - 初始化完成");
    }
    
    /**
     * 检查模组是否已初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * 关闭模组系统
     */
    public static void shutdown() {
        synchronized (lock) {
            if (!initialized) {
                return;
            }
            
            try {
                ArisLogger.info("正在关闭Aris Sweeping模组系统...");
                
                // 保存统计数据
                try {
                    StatisticsCollector collector = StatisticsCollector.getInstance();
                    collector.recordEvent("mod_shutdown", "system");
                    collector.saveStatistics();
                    ArisLogger.info("✓ 统计数据已保存");
                } catch (Exception e) {
                    ArisLogger.error("保存统计数据失败", e);
                }
                
                // 保存配置
                try {
                    ModConfig.saveConfig();
                    ArisLogger.info("✓ 配置已保存");
                } catch (Exception e) {
                    ArisLogger.error("保存配置失败", e);
                }
                
                initialized = false;
                ArisLogger.info("Aris Sweeping模组系统已关闭");
                
            } catch (Exception e) {
                ArisLogger.error("模组关闭时发生错误", e);
            }
        }
    }
    
    /**
     * 获取系统状态摘要
     */
    public static SystemStatus getSystemStatus() {
        return new SystemStatus(
            initialized,
            ModConfig.isInitialized(),
            StatisticsCollector.getInstance() != null,
            System.currentTimeMillis()
        );
    }
    
    /**
     * 系统状态类
     */
    public static class SystemStatus {
        private final boolean modInitialized;
        private final boolean configInitialized;
        private final boolean statisticsInitialized;
        private final long timestamp;
        
        public SystemStatus(boolean modInitialized, boolean configInitialized, 
                          boolean statisticsInitialized, long timestamp) {
            this.modInitialized = modInitialized;
            this.configInitialized = configInitialized;
            this.statisticsInitialized = statisticsInitialized;
            this.timestamp = timestamp;
        }
        
        // Getters
        public boolean isModInitialized() { return modInitialized; }
        public boolean isConfigInitialized() { return configInitialized; }
        public boolean isStatisticsInitialized() { return statisticsInitialized; }
        public long getTimestamp() { return timestamp; }
        
        public boolean isAllSystemsReady() {
            return modInitialized && configInitialized && statisticsInitialized;
        }
        
        @Override
        public String toString() {
            return String.format("SystemStatus{mod=%s, config=%s, stats=%s, ready=%s}", 
                modInitialized, configInitialized, statisticsInitialized, isAllSystemsReady());
        }
    }
}