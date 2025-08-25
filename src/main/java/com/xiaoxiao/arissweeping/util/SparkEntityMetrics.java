package com.xiaoxiao.arissweeping.util;

/**
 * Spark性能指标数据类
 * 简化的性能指标封装，避免过度复杂化
 */
public class SparkEntityMetrics {
    private final double currentTps;
    private final double currentMspt;
    private final long entityCount;
    private final double memoryUsage;
    private final long timestamp;
    
    public SparkEntityMetrics(double currentTps, double currentMspt, long entityCount, double memoryUsage) {
        this.currentTps = currentTps;
        this.currentMspt = currentMspt;
        this.entityCount = entityCount;
        this.memoryUsage = memoryUsage;
        this.timestamp = System.currentTimeMillis();
    }
    
    public double getCurrentTps() { return currentTps; }
    public double getCurrentMspt() { return currentMspt; }
    public long getEntityCount() { return entityCount; }
    public long getTotalEntities() { return entityCount; } // 兼容方法
    public double getTps() { return currentTps; } // 兼容方法
    public double getMspt() { return currentMspt; } // 兼容方法
    public double getMemoryUsage() { return memoryUsage; }
    public long getTimestamp() { return timestamp; }
    
    public boolean isPerformanceCritical() {
        return currentTps < 15.0 || currentMspt > 50.0;
    }
}