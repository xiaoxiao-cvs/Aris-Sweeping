package com.xiaoxiao.arissweeping.livestock;

import com.xiaoxiao.arissweeping.config.ModConfig;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * 畜牧业调度器 - 专门负责任务调度管理
 * 从LivestockMonitor中分离出来，遵循单一职责原则
 */
public class LivestockScheduler {
    private final Plugin plugin;
    private final ModConfig config;
    private final LivestockMonitor monitor;
    private final LivestockSmartCleanupService smartCleanupService;
    
    private BukkitTask livestockMonitorTask;
    private BukkitTask smartCleanupTask;
    
    public LivestockScheduler(Plugin plugin, ModConfig config, LivestockMonitor monitor, LivestockSmartCleanupService smartCleanupService) {
        this.plugin = plugin;
        this.config = config;
        this.monitor = monitor;
        this.smartCleanupService = smartCleanupService;
    }
    
    /**
     * 启动所有调度任务
     */
    public void startScheduling() {
        if (!config.isPluginEnabled() || !config.isScheduledChecksEnabled()) {
            plugin.getLogger().info("[LivestockScheduler] 调度功能已在配置中禁用，跳过启动");
            return;
        }
        
        stopScheduling();
        
        if (config.getMaxAnimalsPerChunk() <= 0) {
            plugin.getLogger().warning("[LivestockScheduler] 配置中的动物密度阈值无效: " + config.getMaxAnimalsPerChunk());
            return;
        }
        
        startMainMonitorTask();
        startSmartCleanupTask();
        
        plugin.getLogger().info(String.format(
            "[LivestockScheduler] 任务调度已启动 - 监控间隔: %d秒, 智能清理: %s",
            config.getLivestockDensityCheckInterval(),
            config.isSmartCleanupEnabled() ? "启用" : "禁用"
        ));
    }
    
    /**
     * 停止所有调度任务
     */
    public void stopScheduling() {
        if (livestockMonitorTask != null) {
            livestockMonitorTask.cancel();
            livestockMonitorTask = null;
        }
        
        if (smartCleanupTask != null) {
            smartCleanupTask.cancel();
            smartCleanupTask = null;
        }
        
        plugin.getLogger().info("[LivestockScheduler] 任务调度已停止");
    }
    
    /**
     * 重启调度
     */
    public void restartScheduling() {
        plugin.getLogger().info("[LivestockScheduler] 重启任务调度...");
        stopScheduling();
        
        // 延迟启动以确保完全停止
        new BukkitRunnable() {
            @Override
            public void run() {
                startScheduling();
            }
        }.runTaskLater(plugin, 20L); // 1秒延迟
    }
    
    /**
     * 启动主监控任务
     */
    private void startMainMonitorTask() {
        long monitorInterval = config.getLivestockDensityCheckInterval() * 20L;
        livestockMonitorTask = new BukkitRunnable() {
            @Override
            public void run() {
                monitor.performMonitoringCheck();
            }
        }.runTaskTimerAsynchronously(plugin, 0L, monitorInterval);
    }
    
    /**
     * 启动智能清理任务
     */
    private void startSmartCleanupTask() {
        if (!config.isSmartCleanupEnabled()) {
            return;
        }
        
        long cleanupInterval = config.getSmartCleanupInterval() * 20L;
        smartCleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                smartCleanupService.performSmartCleanupCheck();
            }
        }.runTaskTimerAsynchronously(plugin, cleanupInterval, cleanupInterval);
    }
    
    /**
     * 获取调度状态信息
     */
    public String getSchedulingStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== 畜牧业调度状态 ===\n");
        status.append("监控任务: ").append(livestockMonitorTask != null ? "运行中" : "已停止").append("\n");
        status.append("智能清理: ").append(smartCleanupTask != null ? "运行中" : "已停止").append("\n");
        return status.toString();
    }
    
    /**
     * 检查是否有任务在运行
     */
    public boolean isScheduling() {
        return livestockMonitorTask != null || smartCleanupTask != null;
    }
}