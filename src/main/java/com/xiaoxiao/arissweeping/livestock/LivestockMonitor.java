package com.xiaoxiao.arissweeping.livestock;

import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.EntityHotspotDetector;
import com.xiaoxiao.arissweeping.util.LivestockHotspotInfo;
import com.xiaoxiao.arissweeping.util.LivestockStatistics;
import com.xiaoxiao.arissweeping.util.SparkEntityMetrics;
import com.xiaoxiao.arissweeping.util.CleanupStateManager;
import com.xiaoxiao.arissweeping.util.LoggerUtil;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * 畜牧业监控器 - 专注于核心监控检查逻辑
 * 调度功能已分离到LivestockScheduler
 */
public class LivestockMonitor {
    private final Plugin plugin;
    private final ModConfig config;
    private final EntityHotspotDetector hotspotDetector;
    private final LivestockViolationDetector violationDetector;
    private final LivestockCleanupManager cleanupManager;
    private final CleanupStateManager stateManager;
    
    private LivestockStatistics lastStatistics;
    
    public LivestockMonitor(Plugin plugin, ModConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.stateManager = CleanupStateManager.getInstance();
        
        this.hotspotDetector = new EntityHotspotDetector((com.xiaoxiao.arissweeping.ArisSweeping) plugin);
        this.violationDetector = new LivestockViolationDetector(config);
        this.cleanupManager = new LivestockCleanupManager(plugin, config);
        
        LoggerUtil.info("[LivestockMonitor] 成功初始化畜牧业监控器");
    }
    
    /**
     * 检查监控是否可用
     */
    public boolean isMonitoringAvailable() {
        return config.isPluginEnabled() && 
               config.isScheduledChecksEnabled() && 
               config.getMaxAnimalsPerChunk() > 0;
    }
    
    /**
     * 重置监控状态
     */
    public void resetMonitoringState() {
        cleanupManager.clearPendingCleanups();
        lastStatistics = null;
        LoggerUtil.info("[LivestockMonitor] 监控状态已重置");
    }
    

    
    /**
     * 执行监控检查
     */
    public void performMonitoringCheck() {
        if (!isMonitoringAvailable()) {
            return;
        }
        
        if (stateManager.isCleanupRunning(CleanupStateManager.CleanupType.LIVESTOCK)) {
            LoggerUtil.info("[LivestockMonitor] 清理任务正在进行中，跳过本次检查");
            return;
        }
        
        if (!plugin.isEnabled()) {
            LoggerUtil.fine("[LivestockMonitor] 监控检查跳过 - 插件已禁用");
            return;
        }
        
        try {
            // 移除智能清理逻辑，让畜牧业检查完全独立于主清理流程
            // 智能清理现在只通过独立的smartCleanupTask执行
            
            // 异步扫描畜牧业热点
            hotspotDetector.scanLivestockHotspotsAsync(new EntityHotspotDetector.LivestockScanCallback() {
                @Override
                public void onComplete(List<LivestockHotspotInfo> hotspots, LivestockStatistics statistics) {
                    lastStatistics = statistics;
                    violationDetector.processHotspotResults(hotspots, statistics, cleanupManager);
                }
                
                @Override
                public void onError(Exception error) {
                    LoggerUtil.severe("[LivestockMonitor] 热点扫描失败: " + error.getMessage());
                    error.printStackTrace();
                }
            });
            
        } catch (Exception e) {
            LoggerUtil.severe("[LivestockMonitor] 启动监控检查时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    

    
    /**
     * 获取状态信息
     */
    public String getStatusInfo() {
        StringBuilder status = new StringBuilder();
        status.append("=== 畜牧业监控状态 ===\n");
        status.append("监控可用: ").append(isMonitoringAvailable() ? "是" : "否").append("\n");
        status.append("清理状态: ").append(stateManager.isCleanupRunning(CleanupStateManager.CleanupType.LIVESTOCK) ? "进行中" : "空闲").append("\n");
        
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
            LoggerUtil.warning("[LivestockMonitor] 获取性能指标失败: " + e.getMessage());
            return null;
        }
    }
    
    public boolean isCleanupRunning() {
        return stateManager.isCleanupRunning(CleanupStateManager.CleanupType.LIVESTOCK);
    }
    
    public void setCleanupRunning(boolean running) {
        if (running) {
            stateManager.tryStartCleanup(CleanupStateManager.CleanupType.LIVESTOCK, "LIVESTOCK_MONITOR");
        } else {
            stateManager.completeCleanup(CleanupStateManager.CleanupType.LIVESTOCK);
        }
    }
}