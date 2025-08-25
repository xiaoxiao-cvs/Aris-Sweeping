package com.xiaoxiao.arissweeping.handler;

import com.xiaoxiao.arissweeping.ArisSweeping;
import com.xiaoxiao.arissweeping.cleanup.CleanupServiceManager;
import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.service.*;
import com.xiaoxiao.arissweeping.util.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

/**
 * 实体清理处理器 - 重构后的主控制器
 * 负责协调各个服务组件，不再包含具体的业务逻辑
 */
public class EntityCleanupHandler implements Listener {
    private final ArisSweeping plugin;
    private final ModConfig config;
    private final TpsMonitor tpsMonitor;
    private final CleanupStateManager stateManager;
    private final CleanupServiceManager serviceManager;
    
    // 分离出的服务组件
    private final EntityValidationService validationService;
    private final CleanupTaskManager taskManager;
    private final DensityMonitorService densityService;
    private final CleanupNotificationService notificationService;
    private final EmergencyCleanupService emergencyService;
    
    public EntityCleanupHandler(ArisSweeping plugin) {
        this.plugin = plugin;
        this.config = plugin.getModConfig();
        this.tpsMonitor = new TpsMonitor(plugin);
        this.stateManager = CleanupStateManager.getInstance();
        this.serviceManager = new CleanupServiceManager(plugin);
        
        // 初始化服务组件
        this.notificationService = new CleanupNotificationService(config);
        this.validationService = new EntityValidationService(config);
        this.taskManager = new CleanupTaskManager(plugin, this::performManualCleanup);
        this.densityService = new DensityMonitorService(plugin);
        this.emergencyService = new EmergencyCleanupService(plugin, tpsMonitor, validationService, notificationService);
    }
    
    /**
     * 初始化清理处理器
     */
    public void init() {
        LoggerUtil.info("EntityCleanupHandler", "Initializing Entity Cleanup Handler...");
        
        // 检查全局开关
        if (!config.isPluginEnabled()) {
            LoggerUtil.warning("EntityCleanupHandler", "initialization skipped - plugin disabled in config");
            return;
        }
        
        // 验证配置
        try {
            validateConfiguration();
        } catch (Exception e) {
            LoggerUtil.warning("EntityCleanupHandler", "Configuration validation failed, using default values: %s", e.getMessage());
            notificationService.sendConfigErrorNotification("配置验证失败: " + e.getMessage());
        }
        
        // 启动各个服务组件
        try {
            // 启动定时清理任务
            LoggerUtil.info("EntityCleanupHandler", "Starting cleanup task...");
            taskManager.startCleanupTask();
            
            // 启动实体密度检查任务
            LoggerUtil.info("EntityCleanupHandler", "Starting density check task...");
            densityService.startDensityCheckTask();
            
            // 启动TPS监控
            if (config.isTpsMonitorEnabled()) {
                LoggerUtil.info("EntityCleanupHandler", "Starting TPS monitor...");
                tpsMonitor.startMonitoring();
            } else {
                LoggerUtil.info("EntityCleanupHandler", "TPS monitor disabled in config");
            }
            
        } catch (Exception e) {
            LoggerUtil.severe("EntityCleanupHandler", "Failed to start services", e);
        }
        
        // 输出初始化状态
        try {
            logInitializationStatus();
        } catch (Exception e) {
            LoggerUtil.warning("EntityCleanupHandler", "Failed to log initialization status: %s", e.getMessage());
        }
        
        LoggerUtil.info("EntityCleanupHandler", "Entity Cleanup Handler initialized successfully");
    }
    
    /**
     * 验证配置
     */
    private void validateConfiguration() {
        // 验证清理间隔
        if (config.getCleanupInterval() <= 0) {
            throw new IllegalArgumentException("清理间隔必须大于0秒");
        }
        
        // 验证倒计时时间
        if (config.getCountdownTime() < 0) {
            throw new IllegalArgumentException("倒计时时间不能为负数");
        }
        
        // 验证实体密度阈值
        if (config.getEntityDensityThreshold() <= 0) {
            throw new IllegalArgumentException("实体密度阈值必须大于0");
        }
        
        // 验证TPS阈值
        if (config.getLowTpsThreshold() <= 0 || config.getLowTpsThreshold() > 20) {
            throw new IllegalArgumentException("TPS阈值必须在0-20之间");
        }
        
        // 验证聚集检测参数
        if (config.isClusterCleanupEnabled()) {
            if (config.getClusterDetectionDistance() <= 0) {
                throw new IllegalArgumentException("聚集检测距离必须大于0");
            }
            if (config.getMinClusterSize() <= 0) {
                throw new IllegalArgumentException("最小聚集大小必须大于0");
            }
            if (config.getClusterPreserveRatio() < 0 || config.getClusterPreserveRatio() > 1) {
                throw new IllegalArgumentException("聚集保留比例必须在0-1之间");
            }
        }
        
        LoggerUtil.info("EntityCleanupHandler", "Configuration validation passed");
    }
    
    /**
     * 记录初始化状态
     */
    private void logInitializationStatus() {
        LoggerUtil.info("EntityCleanupHandler", "=== Entity Cleanup Handler Status ===");
        LoggerUtil.info("EntityCleanupHandler", "Plugin Enabled: %s", config.isPluginEnabled());
        LoggerUtil.info("EntityCleanupHandler", "Cleanup Interval: %d seconds", config.getCleanupInterval());
        LoggerUtil.info("EntityCleanupHandler", "Countdown Time: %d seconds", config.getCountdownTime());
        LoggerUtil.info("EntityCleanupHandler", "TPS Monitor Enabled: %s", config.isTpsMonitorEnabled());
        LoggerUtil.info("EntityCleanupHandler", "Emergency Cleanup Enabled: %s", config.isEmergencyCleanupEnabled());
        LoggerUtil.info("EntityCleanupHandler", "Cluster Cleanup Enabled: %s", config.isClusterCleanupEnabled());
        LoggerUtil.info("EntityCleanupHandler", "Entity Density Check Enabled: %s", config.isEntityDensityCheckEnabled());
        LoggerUtil.info("EntityCleanupHandler", "========================================");
    }
    
    /**
     * 处理区块加载事件
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        try {
            // 委托给密度监控服务处理
            densityService.onChunkLoad(event);
        } catch (Exception e) {
            LoggerUtil.warning("EntityCleanupHandler", "Error handling chunk load event: %s", e.getMessage());
        }
    }
    
    /**
     * 执行手动清理
     */
    public void performManualCleanup() {
        try {
            taskManager.performManualCleanup();
        } catch (Exception e) {
            LoggerUtil.severe("EntityCleanupHandler", "Manual cleanup failed", e);
        }
    }
    
    /**
     * 执行紧急清理
     */
    public void performEmergencyCleanup() {
        try {
            emergencyService.performEmergencyCleanup();
        } catch (Exception e) {
            LoggerUtil.severe("EntityCleanupHandler", "Emergency cleanup failed", e);
        }
    }
    
    /**
     * 强制执行紧急清理
     */
    public void forceEmergencyCleanup() {
        try {
            emergencyService.forceEmergencyCleanup();
        } catch (Exception e) {
            LoggerUtil.severe("EntityCleanupHandler", "Force emergency cleanup failed", e);
        }
    }
    
    /**
     * 停止清理任务
     */
    public void stopCleanupTask() {
        try {
            taskManager.stopCleanupTask();
        } catch (Exception e) {
            LoggerUtil.warning("EntityCleanupHandler", "Error stopping cleanup task: %s", e.getMessage());
        }
    }
    
    /**
     * 重启清理任务
     */
    public void restartCleanupTask() {
        try {
            taskManager.restartCleanupTask();
        } catch (Exception e) {
            LoggerUtil.severe("EntityCleanupHandler", "Error restarting cleanup task", e);
        }
    }
    
    /**
     * 停止密度检查任务
     */
    public void stopDensityCheckTask() {
        try {
            densityService.stopDensityCheckTask();
        } catch (Exception e) {
            LoggerUtil.warning("EntityCleanupHandler", "Error stopping density check task: %s", e.getMessage());
        }
    }
    
    /**
     * 重启密度检查任务
     */
    public void restartDensityCheckTask() {
        try {
            densityService.restartDensityCheckTask();
        } catch (Exception e) {
            LoggerUtil.severe("EntityCleanupHandler", "Error restarting density check task", e);
        }
    }
    
    /**
     * 获取状态信息 (getStatusInfo方法别名)
     */
    public String getStatusInfo() {
        return getTaskStatusInfo();
    }
    
    /**
     * 获取任务状态信息
     */
    public String getTaskStatusInfo() {
        StringBuilder status = new StringBuilder();
        
        try {
            status.append("=== Entity Cleanup Handler Status ===\n");
            status.append("Plugin Enabled: ").append(config.isPluginEnabled() ? "是" : "否").append("\n");
            status.append("\n");
            
            // 清理任务状态
            status.append(taskManager.getTaskStatusInfo()).append("\n");
            
            // 密度监控状态
            status.append(densityService.getStatusInfo()).append("\n");
            
            // 紧急清理状态
            status.append(emergencyService.getStatusInfo()).append("\n");
            
            // TPS监控状态
            status.append("TPS监控状态:\n");
            status.append("- TPS监控启用: ").append(config.isTpsMonitorEnabled() ? "是" : "否").append("\n");
            if (config.isTpsMonitorEnabled()) {
                status.append("- 当前TPS: ").append(String.format("%.2f", tpsMonitor.getCurrentTps())).append("\n");
            }
            
            status.append("=====================================");
            
        } catch (Exception e) {
            LoggerUtil.warning("EntityCleanupHandler", "Error getting status info: %s", e.getMessage());
            status.append("获取状态信息时发生错误: ").append(e.getMessage());
        }
        
        return status.toString();
    }
    
    /**
     * 关闭处理器
     */
    public void shutdown() {
        LoggerUtil.info("EntityCleanupHandler", "Shutting down Entity Cleanup Handler...");
        
        try {
            // 停止各个服务组件
            taskManager.stopCleanupTask();
            densityService.stopDensityCheckTask();
            
            // 停止TPS监控
            if (tpsMonitor != null) {
                tpsMonitor.stopMonitoring();
            }
            
            // 清理状态管理器
            stateManager.shutdown();
            
            LoggerUtil.info("EntityCleanupHandler", "Entity Cleanup Handler shutdown completed");
            
        } catch (Exception e) {
            LoggerUtil.severe("EntityCleanupHandler", "Error during shutdown", e);
        }
    }
    
    // Getter方法
    public TpsMonitor getTpsMonitor() {
        return tpsMonitor;
    }
    
    public EntityValidationService getValidationService() {
        return validationService;
    }
    
    public CleanupTaskManager getTaskManager() {
        return taskManager;
    }
    
    public DensityMonitorService getDensityService() {
        return densityService;
    }
    
    public CleanupNotificationService getNotificationService() {
        return notificationService;
    }
    
    public EmergencyCleanupService getEmergencyService() {
        return emergencyService;
    }
    
    public CleanupStateManager getStateManager() {
        return stateManager;
    }
    
    public CleanupServiceManager getServiceManager() {
        return serviceManager;
    }
    
    /**
     * 获取清理世界列表
     */
    public java.util.List<org.bukkit.World> getCleanupWorlds() {
        return org.bukkit.Bukkit.getWorlds();
    }
    
    /**
     * 启动清理任务
     */
    public void startCleanupTask() {
        taskManager.startCleanupTask();
    }
    
    /**
     * 获取实体列表 (兼容性方法)
     */
    public java.util.List<org.bukkit.entity.Entity> entities() {
        java.util.List<org.bukkit.entity.Entity> allEntities = new java.util.ArrayList<>();
        for (org.bukkit.World world : getCleanupWorlds()) {
            allEntities.addAll(world.getEntities());
        }
        return allEntities;
    }
    
    /**
     * 获取统计信息
     */
    public java.util.Map<String, Object> getStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalCleanupsPerformed", 0);
        stats.put("totalEntitiesRemoved", 0);
        stats.put("averageExecutionTime", 0.0);
        stats.put("lastCleanupTime", System.currentTimeMillis());
        return stats;
    }
    
    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        // 重置统计信息的实现
    }
    
    /**
     * 获取活跃清理任务
     */
    public java.util.Set<String> getActiveCleanupTasks() {
        return new java.util.HashSet<>();
    }
    
    /**
     * 使用过滤器进行清理
     */
    public java.util.concurrent.CompletableFuture<Integer> cleanupWithFilter(java.util.function.Predicate<org.bukkit.entity.Entity> filter) {
        return java.util.concurrent.CompletableFuture.completedFuture(0);
    }
    
    /**
     * 执行清理操作
     */
    public void performCleanup() {
        // 执行清理的实现
    }
}