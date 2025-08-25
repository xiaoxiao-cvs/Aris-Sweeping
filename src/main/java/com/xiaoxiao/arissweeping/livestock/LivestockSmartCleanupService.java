package com.xiaoxiao.arissweeping.livestock;

import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.EntityHotspotDetector;
import com.xiaoxiao.arissweeping.util.LoggerUtil;
import com.xiaoxiao.arissweeping.util.SparkEntityMetrics;
import org.bukkit.plugin.Plugin;

/**
 * 畜牧业智能清理服务 - 专门负责智能清理逻辑
 * 从LivestockMonitor中分离出来，遵循单一职责原则
 */
public class LivestockSmartCleanupService {
    private final Plugin plugin;
    private final ModConfig config;
    private final EntityHotspotDetector hotspotDetector;
    private final LivestockPerformanceAnalyzer performanceAnalyzer;
    private final LivestockCleanupManager cleanupManager;
    
    private long lastPerformanceCheck = 0;
    
    public LivestockSmartCleanupService(Plugin plugin, ModConfig config, 
                                       EntityHotspotDetector hotspotDetector,
                                       LivestockPerformanceAnalyzer performanceAnalyzer,
                                       LivestockCleanupManager cleanupManager) {
        this.plugin = plugin;
        this.config = config;
        this.hotspotDetector = hotspotDetector;
        this.performanceAnalyzer = performanceAnalyzer;
        this.cleanupManager = cleanupManager;
    }
    
    /**
     * 执行智能清理检查
     */
    public void performSmartCleanupCheck() {
        if (!config.isSmartCleanupEnabled()) {
            return;
        }
        
        // 检查主清理是否正在运行，如果是则跳过畜牧业智能清理
        if (plugin instanceof com.xiaoxiao.arissweeping.ArisSweeping) {
            com.xiaoxiao.arissweeping.ArisSweeping arisSweeping = (com.xiaoxiao.arissweeping.ArisSweeping) plugin;
            if (arisSweeping.isMainCleanupRunning()) {
                LoggerUtil.info("[LivestockSmartCleanup] 主清理正在运行，跳过畜牧业智能清理");
                return;
            }
        }
        
        try {
            SparkEntityMetrics metrics = hotspotDetector.getCurrentSparkMetrics();
            if (metrics != null && performanceAnalyzer.shouldPerformSmartCleanup(metrics, lastPerformanceCheck)) {
                lastPerformanceCheck = System.currentTimeMillis();
                cleanupManager.performSmartCleanup(metrics);
                LoggerUtil.info("[LivestockSmartCleanup] 基于性能指标执行智能清理");
            }
        } catch (Exception e) {
            LoggerUtil.severe("[LivestockSmartCleanup] 智能清理检查失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 重置性能检查时间
     */
    public void resetPerformanceCheck() {
        lastPerformanceCheck = 0;
    }
    
    /**
     * 获取最后一次性能检查时间
     */
    public long getLastPerformanceCheck() {
        return lastPerformanceCheck;
    }
    
    /**
     * 获取智能清理状态信息
     */
    public String getStatusInfo() {
        StringBuilder status = new StringBuilder();
        status.append("=== 智能清理服务状态 ===\n");
        status.append("智能清理: ").append(config.isSmartCleanupEnabled() ? "启用" : "禁用").append("\n");
        if (lastPerformanceCheck > 0) {
            long timeSinceLastCheck = System.currentTimeMillis() - lastPerformanceCheck;
            status.append("上次检查: ").append(timeSinceLastCheck / 1000).append("秒前\n");
        } else {
            status.append("上次检查: 从未执行\n");
        }
        return status.toString();
    }
}