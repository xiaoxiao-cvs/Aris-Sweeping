package com.xiaoxiao.arissweeping.livestock;

import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.EntityHotspotDetector.SparkEntityMetrics;

/**
 * 畜牧业性能分析器 - 负责分析服务器性能并决定清理策略
 */
public class LivestockPerformanceAnalyzer {
    private final ModConfig config;
    private SparkEntityMetrics baselineMetrics;
    private long baselineTime = 0;
    
    // 性能阈值常量
    private static final double DEFAULT_TPS_THRESHOLD = 18.0;
    private static final double DEFAULT_MSPT_THRESHOLD = 55.0;
    private static final int DEFAULT_ENTITY_THRESHOLD = 1000;
    private static final long PERFORMANCE_CHECK_INTERVAL = 30000; // 30秒
    private static final long BASELINE_UPDATE_INTERVAL = 300000; // 5分钟
    
    public LivestockPerformanceAnalyzer(ModConfig config) {
        this.config = config;
    }
    
    /**
     * 判断是否需要执行智能清理
     */
    public boolean shouldPerformSmartCleanup(SparkEntityMetrics metrics, long lastPerformanceCheck) {
        long currentTime = System.currentTimeMillis();
        
        // 避免频繁检查
        if (currentTime - lastPerformanceCheck < PERFORMANCE_CHECK_INTERVAL) {
            return false;
        }
        
        // 更新基线指标
        updateBaselineIfNeeded(metrics, currentTime);
        
        // 基于配置的阈值判断
        if (exceedsConfiguredThresholds(metrics)) {
            return true;
        }
        
        // 基于基线的性能下降判断
        if (baselineMetrics != null && hasSignificantPerformanceDrop(metrics)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查是否超过配置的阈值
     */
    private boolean exceedsConfiguredThresholds(SparkEntityMetrics metrics) {
        double tpsThreshold = config.getDouble("livestock.smart-cleanup.tps-threshold", DEFAULT_TPS_THRESHOLD);
        double msptThreshold = config.getDouble("livestock.smart-cleanup.mspt-threshold", DEFAULT_MSPT_THRESHOLD);
        int entityThreshold = config.getInt("livestock.smart-cleanup.entity-threshold", DEFAULT_ENTITY_THRESHOLD);
        
        return metrics.getTps() < tpsThreshold || 
               metrics.getMspt() > msptThreshold || 
               metrics.getTotalEntities() > entityThreshold;
    }
    
    /**
     * 检查是否有显著的性能下降
     */
    private boolean hasSignificantPerformanceDrop(SparkEntityMetrics current) {
        if (baselineMetrics == null) {
            return false;
        }
        
        // TPS下降超过20%
        double tpsDrop = (baselineMetrics.getTps() - current.getTps()) / baselineMetrics.getTps();
        if (tpsDrop > 0.2) {
            return true;
        }
        
        // MSPT增加超过50%
        double msptIncrease = (current.getMspt() - baselineMetrics.getMspt()) / baselineMetrics.getMspt();
        if (msptIncrease > 0.5) {
            return true;
        }
        
        // 实体数量增加超过30%
        double entityIncrease = (double)(current.getTotalEntities() - baselineMetrics.getTotalEntities()) / baselineMetrics.getTotalEntities();
        if (entityIncrease > 0.3) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 更新基线指标（如果需要）
     */
    private void updateBaselineIfNeeded(SparkEntityMetrics metrics, long currentTime) {
        // 如果没有基线或基线过期，则更新
        if (baselineMetrics == null || currentTime - baselineTime > BASELINE_UPDATE_INTERVAL) {
            // 只有在性能良好时才更新基线
            if (isGoodPerformance(metrics)) {
                baselineMetrics = metrics;
                baselineTime = currentTime;
                
                if (config.isDebugMode()) {
                    config.getPlugin().getLogger().info(String.format(
                        "[LivestockPerformanceAnalyzer] 更新性能基线 - TPS: %.2f, MSPT: %.2f, 实体: %d",
                        metrics.getTps(), metrics.getMspt(), metrics.getTotalEntities()
                    ));
                }
            }
        }
    }
    
    /**
     * 判断当前性能是否良好
     */
    private boolean isGoodPerformance(SparkEntityMetrics metrics) {
        return metrics.getTps() > 19.0 && metrics.getMspt() < 45.0;
    }
    
    /**
     * 判断是否需要紧急清理
     */
    public boolean shouldPerformEmergencyCleanup(SparkEntityMetrics current) {
        if (baselineMetrics == null) {
            return false;
        }
        
        // 紧急情况的阈值更严格
        double emergencyTpsThreshold = config.getDouble("livestock.emergency-cleanup.tps-threshold", 15.0);
        double emergencyMsptThreshold = config.getDouble("livestock.emergency-cleanup.mspt-threshold", 80.0);
        
        // TPS严重下降或MSPT严重增加
        if (current.getTps() < emergencyTpsThreshold || current.getMspt() > emergencyMsptThreshold) {
            return true;
        }
        
        // 与基线相比的严重下降
        double tpsDrop = (baselineMetrics.getTps() - current.getTps()) / baselineMetrics.getTps();
        double msptIncrease = (current.getMspt() - baselineMetrics.getMspt()) / baselineMetrics.getMspt();
        
        return tpsDrop > 0.4 || msptIncrease > 1.0; // 40%的TPS下降或100%的MSPT增加
    }
    
    /**
     * 计算清理优先级
     */
    public int calculateCleanupPriority(SparkEntityMetrics metrics) {
        int priority = 1; // 基础优先级
        
        // 基于TPS调整优先级
        if (metrics.getTps() < 15.0) {
            priority += 3;
        } else if (metrics.getTps() < 18.0) {
            priority += 2;
        } else if (metrics.getTps() < 19.5) {
            priority += 1;
        }
        
        // 基于MSPT调整优先级
        if (metrics.getMspt() > 80.0) {
            priority += 3;
        } else if (metrics.getMspt() > 60.0) {
            priority += 2;
        } else if (metrics.getMspt() > 50.0) {
            priority += 1;
        }
        
        // 基于实体数量调整优先级
        if (metrics.getTotalEntities() > 2000) {
            priority += 2;
        } else if (metrics.getTotalEntities() > 1500) {
            priority += 1;
        }
        
        return Math.min(priority, 10); // 最大优先级为10
    }
    
    /**
     * 获取性能状态描述
     */
    public String getPerformanceStatus(SparkEntityMetrics metrics) {
        if (metrics == null) {
            return "未知";
        }
        
        if (shouldPerformEmergencyCleanup(metrics)) {
            return "严重";
        } else if (shouldPerformSmartCleanup(metrics, 0)) {
            return "警告";
        } else if (isGoodPerformance(metrics)) {
            return "良好";
        } else {
            return "正常";
        }
    }
    
    /**
     * 获取性能分析报告
     */
    public String getPerformanceReport(SparkEntityMetrics current) {
        StringBuilder report = new StringBuilder();
        report.append("=== 性能分析报告 ===\n");
        
        if (current != null) {
            report.append(String.format("当前TPS: %.2f\n", current.getTps()));
            report.append(String.format("当前MSPT: %.2f\n", current.getMspt()));
            report.append(String.format("实体数量: %d\n", current.getTotalEntities()));
            report.append(String.format("性能状态: %s\n", getPerformanceStatus(current)));
            report.append(String.format("清理优先级: %d\n", calculateCleanupPriority(current)));
        }
        
        if (baselineMetrics != null) {
            report.append("\n=== 基线对比 ===\n");
            report.append(String.format("基线TPS: %.2f\n", baselineMetrics.getTps()));
            report.append(String.format("基线MSPT: %.2f\n", baselineMetrics.getMspt()));
            report.append(String.format("基线实体: %d\n", baselineMetrics.getTotalEntities()));
            
            if (current != null) {
                double tpsChange = ((current.getTps() - baselineMetrics.getTps()) / baselineMetrics.getTps()) * 100;
                double msptChange = ((current.getMspt() - baselineMetrics.getMspt()) / baselineMetrics.getMspt()) * 100;
                long entityChange = current.getTotalEntities() - baselineMetrics.getTotalEntities();
                
                report.append(String.format("TPS变化: %.1f%%\n", tpsChange));
                report.append(String.format("MSPT变化: %.1f%%\n", msptChange));
                report.append(String.format("实体变化: %+d\n", (int)entityChange));
            }
        }
        
        return report.toString();
    }
    
    /**
     * 重置基线指标
     */
    public void resetBaseline() {
        baselineMetrics = null;
        baselineTime = 0;
    }
    
    /**
     * 获取基线指标
     */
    public SparkEntityMetrics getBaselineMetrics() {
        return baselineMetrics;
    }
}