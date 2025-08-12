package com.xiaoxiao.arissweeping.livestock;

import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.EntityHotspotDetector;
import com.xiaoxiao.arissweeping.util.EntityHotspotDetector.LivestockHotspotInfo;
import com.xiaoxiao.arissweeping.util.EntityHotspotDetector.LivestockStatistics;
import com.xiaoxiao.arissweeping.util.EntityHotspotDetector.SparkEntityMetrics;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 畜牧业监控器 - 负责定期监控畜牧业密度
 */
public class LivestockMonitor {
    private final Plugin plugin;
    private final ModConfig config;
    private final EntityHotspotDetector hotspotDetector;
    private final LivestockViolationDetector violationDetector;
    private final LivestockCleanupManager cleanupManager;
    private final LivestockPerformanceAnalyzer performanceAnalyzer;
    
    private BukkitTask livestockMonitorTask;
    private BukkitTask smartCleanupTask;
    private final AtomicBoolean isCleanupRunning = new AtomicBoolean(false);
    private LivestockStatistics lastStatistics;
    private long lastPerformanceCheck = 0;
    
    public LivestockMonitor(Plugin plugin, ModConfig config) {
        this.plugin = plugin;
        this.config = config;
        
        try {
            this.hotspotDetector = new EntityHotspotDetector((com.xiaoxiao.arissweeping.ArisSweeping) plugin);
            this.violationDetector = new LivestockViolationDetector(config);
            this.cleanupManager = new LivestockCleanupManager(plugin, config);
            this.performanceAnalyzer = new LivestockPerformanceAnalyzer(config);
            
            plugin.getLogger().info("[LivestockMonitor] 成功初始化畜牧业监控器");
        } catch (Exception e) {
            plugin.getLogger().severe("[LivestockMonitor] 初始化畜牧业监控器失败: " + e.getMessage());
            throw new RuntimeException("无法初始化畜牧业监控器", e);
        }
    }
    
    /**
     * 启动畜牧业监控
     */
    public void startMonitoring() {
        if (!config.isPluginEnabled() || !config.isScheduledChecksEnabled()) {
            plugin.getLogger().info("[LivestockMonitor] 畜牧业监控功能已在配置中禁用，跳过启动");
            return;
        }
        
        try {
            stopMonitoring();
            
            if (config.getMaxAnimalsPerChunk() <= 0) {
                plugin.getLogger().warning("[LivestockMonitor] 配置中的动物密度阈值无效: " + config.getMaxAnimalsPerChunk());
                return;
            }
            
            startMainMonitorTask();
            startSmartCleanupTask();
            
            plugin.getLogger().info(String.format(
                "[LivestockMonitor] 畜牧业监控已启动 - 监控间隔: %d秒, 智能清理: %s",
                config.getLivestockDensityCheckInterval(),
                config.isSmartCleanupEnabled() ? "启用" : "禁用"
            ));
            
        } catch (Exception e) {
            plugin.getLogger().severe("[LivestockMonitor] 启动畜牧业监控失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 停止畜牧业监控
     */
    public void stopMonitoring() {
        try {
            if (livestockMonitorTask != null) {
                livestockMonitorTask.cancel();
                livestockMonitorTask = null;
            }
            
            if (smartCleanupTask != null) {
                smartCleanupTask.cancel();
                smartCleanupTask = null;
            }
            
            cleanupManager.clearPendingCleanups();
            isCleanupRunning.set(false);
            lastStatistics = null;
            lastPerformanceCheck = 0;
            
            plugin.getLogger().info("[LivestockMonitor] 畜牧业监控已停止");
            
        } catch (Exception e) {
            plugin.getLogger().severe("[LivestockMonitor] 停止畜牧业监控时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 启动主监控任务
     */
    private void startMainMonitorTask() {
        long monitorInterval = config.getLivestockDensityCheckInterval() * 20L;
        livestockMonitorTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    performMonitoringCheck();
                } catch (Exception e) {
                    plugin.getLogger().severe("[LivestockMonitor] 监控检查异常: " + e.getMessage());
                    e.printStackTrace();
                }
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
                try {
                    SparkEntityMetrics metrics = hotspotDetector.getCurrentSparkMetrics();
                    if (metrics != null && performanceAnalyzer.shouldPerformSmartCleanup(metrics, lastPerformanceCheck)) {
                        lastPerformanceCheck = System.currentTimeMillis();
                        cleanupManager.performSmartCleanup(metrics);
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("[LivestockMonitor] 智能清理任务异常: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.runTaskTimerAsynchronously(plugin, cleanupInterval, cleanupInterval);
    }
    
    /**
     * 执行监控检查
     */
    private void performMonitoringCheck() {
        if (!config.isPluginEnabled() || !config.isLivestockDensityCheckEnabled()) {
            return;
        }
        
        if (isCleanupRunning.get()) {
            plugin.getLogger().info("[LivestockMonitor] 清理任务正在进行中，跳过本次检查");
            return;
        }
        
        if (!plugin.isEnabled()) {
            plugin.getLogger().fine("[LivestockMonitor] 监控检查跳过 - 插件已禁用");
            return;
        }
        
        try {
            // 检查是否需要基于性能进行智能清理
            SparkEntityMetrics currentMetrics = hotspotDetector.getCurrentSparkMetrics();
            if (currentMetrics != null && performanceAnalyzer.shouldPerformSmartCleanup(currentMetrics, lastPerformanceCheck)) {
                lastPerformanceCheck = System.currentTimeMillis();
                cleanupManager.performSmartCleanup(currentMetrics);
                return;
            }
            
            // 异步扫描畜牧业热点
            hotspotDetector.scanLivestockHotspotsAsync(new EntityHotspotDetector.LivestockScanCallback() {
                @Override
                public void onComplete(List<LivestockHotspotInfo> hotspots, LivestockStatistics statistics) {
                    try {
                        lastStatistics = statistics;
                        violationDetector.processHotspotResults(hotspots, statistics, cleanupManager);
                    } catch (Exception e) {
                        plugin.getLogger().severe("[LivestockMonitor] 处理热点扫描结果时发生异常: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                @Override
                public void onError(Exception error) {
                    plugin.getLogger().severe("[LivestockMonitor] 热点扫描失败: " + error.getMessage());
                    error.printStackTrace();
                }
            });
            
        } catch (Exception e) {
            plugin.getLogger().severe("[LivestockMonitor] 启动监控检查时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 重启监控
     */
    public void restartMonitoring() {
        plugin.getLogger().info("[LivestockMonitor] 重启畜牧业监控系统...");
        stopMonitoring();
        
        // 延迟启动以确保完全停止
        new BukkitRunnable() {
            @Override
            public void run() {
                startMonitoring();
            }
        }.runTaskLater(plugin, 20L); // 1秒延迟
    }
    
    /**
     * 获取状态信息
     */
    public String getStatusInfo() {
        StringBuilder status = new StringBuilder();
        status.append("=== 畜牧业监控状态 ===\n");
        status.append("监控任务: ").append(livestockMonitorTask != null ? "运行中" : "已停止").append("\n");
        status.append("智能清理: ").append(smartCleanupTask != null ? "运行中" : "已停止").append("\n");
        status.append("清理状态: ").append(isCleanupRunning.get() ? "进行中" : "空闲").append("\n");
        
        if (lastStatistics != null) {
            status.append("最后统计: ").append(lastStatistics.getTotalAnimals()).append(" 只动物\n");
        }
        
        status.append(cleanupManager.getStatusInfo());
        
        return status.toString();
    }
    
    // Getter方法
    public LivestockStatistics getLastStatistics() {
        return lastStatistics;
    }
    
    public SparkEntityMetrics getLastPerformanceMetrics() {
        try {
            return hotspotDetector.getCurrentSparkMetrics();
        } catch (Exception e) {
            plugin.getLogger().warning("[LivestockMonitor] 获取性能指标失败: " + e.getMessage());
            return null;
        }
    }
    
    public boolean isCleanupRunning() {
        return isCleanupRunning.get();
    }
    
    public void setCleanupRunning(boolean running) {
        isCleanupRunning.set(running);
    }
}