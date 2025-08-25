package com.xiaoxiao.arissweeping.handler;

import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.livestock.*;
import com.xiaoxiao.arissweeping.util.LivestockStatistics;
import com.xiaoxiao.arissweeping.util.SparkEntityMetrics;
import org.bukkit.plugin.Plugin;

/**
 * 畜牧业密度管理处理器 - 重构版本
 * 现在作为各个组件的协调器，提供统一的接口
 * 保持向后兼容性，同时将功能分解为多个专门的组件
 */
public class LivestockDensityHandler {
    private final Plugin plugin;
    private final ModConfig config;
    
    // 核心组件
    private final LivestockMonitor monitor;
    private final LivestockViolationDetector violationDetector;
    private final LivestockCleanupManager cleanupManager;
    private final LivestockPerformanceAnalyzer performanceAnalyzer;
    
    // 调度和服务组件
    private final LivestockScheduler scheduler;
    private final LivestockSmartCleanupService smartCleanupService;
    
    /**
     * 构造函数
     */
    public LivestockDensityHandler(Plugin plugin, ModConfig config) {
        this.plugin = plugin;
        this.config = config;
        
        try {
            // 初始化各个组件
            this.monitor = new LivestockMonitor(plugin, config);
            this.violationDetector = new LivestockViolationDetector(config);
            this.cleanupManager = new LivestockCleanupManager(plugin, config);
            this.performanceAnalyzer = new LivestockPerformanceAnalyzer(config);
            
            // 创建热点检测器
            com.xiaoxiao.arissweeping.util.EntityHotspotDetector hotspotDetector = 
                new com.xiaoxiao.arissweeping.util.EntityHotspotDetector((com.xiaoxiao.arissweeping.ArisSweeping) plugin);
            
            // 初始化调度和服务组件
            this.smartCleanupService = new LivestockSmartCleanupService(plugin, config, hotspotDetector, performanceAnalyzer, cleanupManager);
            this.scheduler = new LivestockScheduler(plugin, config, monitor, smartCleanupService);
            
            plugin.getLogger().info("[LivestockDensityHandler] 成功初始化重构版畜牧业密度处理器");
        } catch (Exception e) {
            plugin.getLogger().severe("[LivestockDensityHandler] 初始化畜牧业密度处理器失败: " + e.getMessage());
            throw new RuntimeException("无法初始化畜牧业密度处理器", e);
        }
    }
    
    // ==================== 公共接口方法 ====================
    
    /**
     * 启动智能畜牧业监控系统
     */
    public void startLivestockMonitoring() {
        scheduler.startScheduling();
    }
    
    /**
     * 停止智能畜牧业监控系统
     */
    public void stopLivestockMonitoring() {
        scheduler.stopScheduling();
    }
    
    /**
     * 重启畜牧业监控
     */
    public void restartLivestockMonitoring() {
        scheduler.restartScheduling();
    }
    
    /**
     * 执行畜牧业清理
     */
    public void performLivestockCleanup() {
        cleanupManager.performLivestockCleanup();
    }
    
    /**
     * 获取状态信息
     */
    public String getStatusInfo() {
        StringBuilder status = new StringBuilder();
        status.append("=== 畜牧业密度管理状态 ===\n");
        status.append(monitor.getStatusInfo());
        status.append("\n");
        status.append("调度器状态: ").append(scheduler.isScheduling() ? "运行中" : "已停止").append("\n");
        status.append("智能清理服务: ").append(config.isSmartCleanupEnabled() ? "启用" : "禁用").append("\n");
        status.append("\n");
        
        // 性能分析信息
        SparkEntityMetrics metrics = getLastPerformanceMetrics();
        if (metrics != null) {
            status.append("=== 性能状态 ===\n");
            status.append(performanceAnalyzer.getPerformanceReport(metrics));
        }
        
        return status.toString();
    }
    
    /**
     * 获取最后的统计信息
     */
    public LivestockStatistics getLastStatistics() {
        return monitor.getLastStatistics();
    }
    
    /**
     * 获取最后的性能指标
     */
    public SparkEntityMetrics getLastPerformanceMetrics() {
        return monitor.getLastPerformanceMetrics();
    }
    
    /**
     * 检查清理是否正在运行
     */
    public boolean isCleanupRunning() {
        return cleanupManager.isCleanupRunning();
    }
    
    // ==================== 向后兼容性方法 ====================
    
    /**
     * @deprecated 使用 startLivestockMonitoring() 替代
     */
    @Deprecated
    public void startDensityCheck() {
        startLivestockMonitoring();
    }
    
    /**
     * @deprecated 使用 stopLivestockMonitoring() 替代
     */
    @Deprecated
    public void stopDensityCheck() {
        stopLivestockMonitoring();
    }
    
    /**
     * @deprecated 使用 restartLivestockMonitoring() 替代
     */
    @Deprecated
    public void restartDensityCheck() {
        restartLivestockMonitoring();
    }
    
    // ==================== 组件访问方法 ====================
    
    /**
     * 获取监控器组件
     */
    public LivestockMonitor getMonitor() {
        return monitor;
    }
    
    /**
     * 获取违规检测器组件
     */
    public LivestockViolationDetector getViolationDetector() {
        return violationDetector;
    }
    
    /**
     * 获取清理管理器组件
     */
    public LivestockCleanupManager getCleanupManager() {
        return cleanupManager;
    }
    
    /**
     * 获取性能分析器组件
     */
    public LivestockPerformanceAnalyzer getPerformanceAnalyzer() {
        return performanceAnalyzer;
    }
    
    /**
     * 获取调度器组件
     */
    public LivestockScheduler getScheduler() {
        return scheduler;
    }
    
    /**
     * 获取智能清理服务组件
     */
    public LivestockSmartCleanupService getSmartCleanupService() {
        return smartCleanupService;
    }
    
    // ==================== 高级功能方法 ====================
    
    /**
     * 执行智能清理（基于性能指标）
     */
    public void performSmartCleanup(SparkEntityMetrics metrics) {
        cleanupManager.performSmartCleanup(metrics);
    }
    
    /**
     * 重置性能基线
     */
    public void resetPerformanceBaseline() {
        performanceAnalyzer.resetBaseline();
        plugin.getLogger().info("[LivestockDensityHandler] 性能基线已重置");
    }
    
    /**
     * 清理所有待处理的清理任务
     */
    public void clearPendingCleanups() {
        cleanupManager.clearPendingCleanups();
        plugin.getLogger().info("[LivestockDensityHandler] 已清理所有待处理的清理任务");
    }
    
    /**
     * 清理违规检测历史
     */
    public void clearViolationHistory() {
        violationDetector.clearWarningHistory();
        plugin.getLogger().info("[LivestockDensityHandler] 已清理违规检测历史");
    }
    
    /**
     * 获取详细的性能报告
     */
    public String getDetailedPerformanceReport() {
        SparkEntityMetrics metrics = getLastPerformanceMetrics();
        if (metrics == null) {
            return "无法获取性能指标";
        }
        
        return performanceAnalyzer.getPerformanceReport(metrics);
    }
    
    /**
     * 获取组件健康状态
     */
    public String getComponentHealthStatus() {
        StringBuilder health = new StringBuilder();
        health.append("=== 组件健康状态 ===\n");
        
        // 监控器状态
        health.append("监控器: ");
        try {
            LivestockStatistics stats = monitor.getLastStatistics();
            health.append(stats != null ? "正常" : "无数据").append("\n");
        } catch (Exception e) {
            health.append("异常 - ").append(e.getMessage()).append("\n");
        }
        
        // 违规检测器状态
        health.append("违规检测器: ");
        try {
            int historySize = violationDetector.getWarningHistorySize();
            health.append("正常 (历史记录: ").append(historySize).append(")").append("\n");
        } catch (Exception e) {
            health.append("异常 - ").append(e.getMessage()).append("\n");
        }
        
        // 清理管理器状态
        health.append("清理管理器: ");
        try {
            int pendingCount = cleanupManager.getPendingCleanupCount();
            health.append("正常 (待处理: ").append(pendingCount).append(")").append("\n");
        } catch (Exception e) {
            health.append("异常 - ").append(e.getMessage()).append("\n");
        }
        
        // 性能分析器状态
        health.append("性能分析器: ");
        try {
            SparkEntityMetrics baseline = performanceAnalyzer.getBaselineMetrics();
            health.append(baseline != null ? "正常 (有基线)" : "正常 (无基线)").append("\n");
        } catch (Exception e) {
            health.append("异常 - ").append(e.getMessage()).append("\n");
        }
        
        return health.toString();
    }
    
    /**
     * 执行系统自检
     */
    public boolean performSystemCheck() {
        try {
            plugin.getLogger().info("[LivestockDensityHandler] 开始系统自检...");
            
            // 检查配置
            if (!config.isPluginEnabled()) {
                plugin.getLogger().warning("[LivestockDensityHandler] 插件已禁用");
                return false;
            }
            
            // 检查各组件
            boolean allHealthy = true;
            
            // 检查监控器
        try {
            monitor.getLastStatistics();
        } catch (Exception e) {
            plugin.getLogger().warning("[LivestockDensityHandler] 监控器检查失败: " + e.getMessage());
            allHealthy = false;
        }
        
        // 检查调度器
        try {
            scheduler.isScheduling();
        } catch (Exception e) {
            plugin.getLogger().warning("[LivestockDensityHandler] 调度器检查失败: " + e.getMessage());
            allHealthy = false;
        }
        
        // 检查智能清理服务
        try {
            // 智能清理服务状态检查 - 检查服务是否正常工作
            smartCleanupService.getStatusInfo();
        } catch (Exception e) {
            plugin.getLogger().warning("[LivestockDensityHandler] 智能清理服务检查失败: " + e.getMessage());
            allHealthy = false;
        }
            
            if (allHealthy) {
                plugin.getLogger().info("[LivestockDensityHandler] 系统自检通过");
            } else {
                plugin.getLogger().warning("[LivestockDensityHandler] 系统自检发现问题");
            }
            
            return allHealthy;
            
        } catch (Exception e) {
            plugin.getLogger().severe("[LivestockDensityHandler] 系统自检时发生异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}