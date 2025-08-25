package com.xiaoxiao.arissweeping.performance;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 性能指标数据类
 * 用于收集和存储各种性能指标
 */
public class PerformanceMetrics {
    
    // 基本统计信息
    private final AtomicLong totalEntitiesProcessed = new AtomicLong(0);
    private final AtomicLong totalEntitiesRemoved = new AtomicLong(0);
    private final AtomicLong totalChunksProcessed = new AtomicLong(0);
    private final AtomicLong totalWorldsProcessed = new AtomicLong(0);
    
    // 执行时间统计
    private final LongAdder totalExecutionTime = new LongAdder();
    private final AtomicLong minExecutionTime = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxExecutionTime = new AtomicLong(0);
    private final AtomicLong executionCount = new AtomicLong(0);
    
    // 内存使用统计
    private volatile long peakMemoryUsage = 0;
    private volatile long currentMemoryUsage = 0;
    private final LongAdder totalMemoryAllocated = new LongAdder();
    
    // 错误统计
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final Map<String, AtomicLong> errorsByType = new ConcurrentHashMap<>();
    
    // 策略执行统计
    private final Map<String, StrategyMetrics> strategyMetrics = new ConcurrentHashMap<>();
    
    // 时间戳
    private final long startTime = System.currentTimeMillis();
    private volatile long lastUpdateTime = System.currentTimeMillis();
    
    /**
     * 策略执行指标
     */
    public static class StrategyMetrics {
        private final AtomicLong executionCount = new AtomicLong(0);
        private final AtomicLong entitiesProcessed = new AtomicLong(0);
        private final AtomicLong entitiesRemoved = new AtomicLong(0);
        private final LongAdder totalExecutionTime = new LongAdder();
        private final AtomicLong minExecutionTime = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxExecutionTime = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failureCount = new AtomicLong(0);
        
        public void recordExecution(long executionTime, long entitiesProcessed, long entitiesRemoved, boolean success) {
            this.executionCount.incrementAndGet();
            this.entitiesProcessed.addAndGet(entitiesProcessed);
            this.entitiesRemoved.addAndGet(entitiesRemoved);
            this.totalExecutionTime.add(executionTime);
            
            // 更新最小和最大执行时间
            updateMinTime(this.minExecutionTime, executionTime);
            updateMaxTime(this.maxExecutionTime, executionTime);
            
            if (success) {
                this.successCount.incrementAndGet();
            } else {
                this.failureCount.incrementAndGet();
            }
        }
        
        // Getters
        public long getExecutionCount() { return executionCount.get(); }
        public long getEntitiesProcessed() { return entitiesProcessed.get(); }
        public long getEntitiesRemoved() { return entitiesRemoved.get(); }
        public long getTotalExecutionTime() { return totalExecutionTime.sum(); }
        public long getMinExecutionTime() { 
            long min = minExecutionTime.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }
        public long getMaxExecutionTime() { return maxExecutionTime.get(); }
        public long getSuccessCount() { return successCount.get(); }
        public long getFailureCount() { return failureCount.get(); }
        
        public double getAverageExecutionTime() {
            long count = executionCount.get();
            return count > 0 ? (double) getTotalExecutionTime() / count : 0.0;
        }
        
        public double getSuccessRate() {
            long total = getExecutionCount();
            return total > 0 ? (double) getSuccessCount() / total * 100.0 : 0.0;
        }
        
        public double getEntitiesPerSecond() {
            long totalTime = getTotalExecutionTime();
            return totalTime > 0 ? (double) getEntitiesProcessed() / (totalTime / 1000.0) : 0.0;
        }
    }
    
    /**
     * 记录清理执行
     */
    public void recordCleanupExecution(long executionTime, long entitiesProcessed, long entitiesRemoved) {
        this.totalEntitiesProcessed.addAndGet(entitiesProcessed);
        this.totalEntitiesRemoved.addAndGet(entitiesRemoved);
        this.totalExecutionTime.add(executionTime);
        this.executionCount.incrementAndGet();
        
        updateMinTime(this.minExecutionTime, executionTime);
        updateMaxTime(this.maxExecutionTime, executionTime);
        
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * 记录策略执行
     */
    public void recordStrategyExecution(String strategyName, long executionTime, 
                                      long entitiesProcessed, long entitiesRemoved, boolean success) {
        strategyMetrics.computeIfAbsent(strategyName, k -> new StrategyMetrics())
            .recordExecution(executionTime, entitiesProcessed, entitiesRemoved, success);
        
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * 记录块处理
     */
    public void recordChunkProcessed() {
        this.totalChunksProcessed.incrementAndGet();
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * 记录世界处理
     */
    public void recordWorldProcessed() {
        this.totalWorldsProcessed.incrementAndGet();
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * 记录内存使用
     */
    public void recordMemoryUsage(long currentUsage, long allocated) {
        this.currentMemoryUsage = currentUsage;
        this.totalMemoryAllocated.add(allocated);
        
        if (currentUsage > this.peakMemoryUsage) {
            this.peakMemoryUsage = currentUsage;
        }
        
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * 记录错误
     */
    public void recordError(String errorType) {
        this.totalErrors.incrementAndGet();
        this.errorsByType.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * 获取运行时间（毫秒）
     */
    public long getUptime() {
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * 获取平均执行时间
     */
    public double getAverageExecutionTime() {
        long count = executionCount.get();
        return count > 0 ? (double) totalExecutionTime.sum() / count : 0.0;
    }
    
    /**
     * 获取每秒处理的实体数
     */
    public double getEntitiesPerSecond() {
        long uptime = getUptime();
        return uptime > 0 ? (double) totalEntitiesProcessed.get() / (uptime / 1000.0) : 0.0;
    }
    
    /**
     * 获取移除率（移除的实体数/处理的实体数）
     */
    public double getRemovalRate() {
        long processed = totalEntitiesProcessed.get();
        return processed > 0 ? (double) totalEntitiesRemoved.get() / processed * 100.0 : 0.0;
    }
    
    /**
     * 获取错误率
     */
    public double getErrorRate() {
        long total = executionCount.get();
        return total > 0 ? (double) totalErrors.get() / total * 100.0 : 0.0;
    }
    
    /**
     * 重置所有指标
     */
    public void reset() {
        totalEntitiesProcessed.set(0);
        totalEntitiesRemoved.set(0);
        totalChunksProcessed.set(0);
        totalWorldsProcessed.set(0);
        
        totalExecutionTime.reset();
        minExecutionTime.set(Long.MAX_VALUE);
        maxExecutionTime.set(0);
        executionCount.set(0);
        
        peakMemoryUsage = 0;
        currentMemoryUsage = 0;
        totalMemoryAllocated.reset();
        
        totalErrors.set(0);
        errorsByType.clear();
        strategyMetrics.clear();
        
        lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * 获取指标快照
     */
    public MetricsSnapshot getSnapshot() {
        return new MetricsSnapshot(
            totalEntitiesProcessed.get(),
            totalEntitiesRemoved.get(),
            totalChunksProcessed.get(),
            totalWorldsProcessed.get(),
            totalExecutionTime.sum(),
            getMinExecutionTime(),
            maxExecutionTime.get(),
            executionCount.get(),
            peakMemoryUsage,
            currentMemoryUsage,
            totalMemoryAllocated.sum(),
            totalErrors.get(),
            new HashMap<>(errorsByType),
            new HashMap<>(strategyMetrics),
            startTime,
            lastUpdateTime
        );
    }
    
    // 辅助方法
    private static void updateMinTime(AtomicLong minTime, long newTime) {
        minTime.updateAndGet(current -> Math.min(current, newTime));
    }
    
    private static void updateMaxTime(AtomicLong maxTime, long newTime) {
        maxTime.updateAndGet(current -> Math.max(current, newTime));
    }
    
    // Getters
    public long getTotalEntitiesProcessed() { return totalEntitiesProcessed.get(); }
    public long getTotalEntitiesRemoved() { return totalEntitiesRemoved.get(); }
    public long getTotalChunksProcessed() { return totalChunksProcessed.get(); }
    public long getTotalWorldsProcessed() { return totalWorldsProcessed.get(); }
    public long getTotalExecutionTime() { return totalExecutionTime.sum(); }
    public long getMinExecutionTime() { 
        long min = minExecutionTime.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }
    public long getMaxExecutionTime() { return maxExecutionTime.get(); }
    public long getExecutionCount() { return executionCount.get(); }
    public long getPeakMemoryUsage() { return peakMemoryUsage; }
    public long getCurrentMemoryUsage() { return currentMemoryUsage; }
    public long getTotalMemoryAllocated() { return totalMemoryAllocated.sum(); }
    public long getTotalErrors() { return totalErrors.get(); }
    public Map<String, AtomicLong> getErrorsByType() { return new HashMap<>(errorsByType); }
    public Map<String, StrategyMetrics> getStrategyMetrics() { return new HashMap<>(strategyMetrics); }
    public long getStartTime() { return startTime; }
    public long getLastUpdateTime() { return lastUpdateTime; }
    
    /**
     * 指标快照类
     * 用于获取某一时刻的指标状态
     */
    public static class MetricsSnapshot {
        private final long totalEntitiesProcessed;
        private final long totalEntitiesRemoved;
        private final long totalChunksProcessed;
        private final long totalWorldsProcessed;
        private final long totalExecutionTime;
        private final long minExecutionTime;
        private final long maxExecutionTime;
        private final long executionCount;
        private final long peakMemoryUsage;
        private final long currentMemoryUsage;
        private final long totalMemoryAllocated;
        private final long totalErrors;
        private final Map<String, AtomicLong> errorsByType;
        private final Map<String, StrategyMetrics> strategyMetrics;
        private final long startTime;
        private final long snapshotTime;
        
        public MetricsSnapshot(long totalEntitiesProcessed, long totalEntitiesRemoved,
                             long totalChunksProcessed, long totalWorldsProcessed,
                             long totalExecutionTime, long minExecutionTime, long maxExecutionTime,
                             long executionCount, long peakMemoryUsage, long currentMemoryUsage,
                             long totalMemoryAllocated, long totalErrors,
                             Map<String, AtomicLong> errorsByType, Map<String, StrategyMetrics> strategyMetrics,
                             long startTime, long snapshotTime) {
            this.totalEntitiesProcessed = totalEntitiesProcessed;
            this.totalEntitiesRemoved = totalEntitiesRemoved;
            this.totalChunksProcessed = totalChunksProcessed;
            this.totalWorldsProcessed = totalWorldsProcessed;
            this.totalExecutionTime = totalExecutionTime;
            this.minExecutionTime = minExecutionTime;
            this.maxExecutionTime = maxExecutionTime;
            this.executionCount = executionCount;
            this.peakMemoryUsage = peakMemoryUsage;
            this.currentMemoryUsage = currentMemoryUsage;
            this.totalMemoryAllocated = totalMemoryAllocated;
            this.totalErrors = totalErrors;
            this.errorsByType = errorsByType;
            this.strategyMetrics = strategyMetrics;
            this.startTime = startTime;
            this.snapshotTime = snapshotTime;
        }
        
        // Getters
        public long getTotalEntitiesProcessed() { return totalEntitiesProcessed; }
        public long getTotalEntitiesRemoved() { return totalEntitiesRemoved; }
        public long getTotalChunksProcessed() { return totalChunksProcessed; }
        public long getTotalWorldsProcessed() { return totalWorldsProcessed; }
        public long getTotalExecutionTime() { return totalExecutionTime; }
        public long getMinExecutionTime() { return minExecutionTime; }
        public long getMaxExecutionTime() { return maxExecutionTime; }
        public long getExecutionCount() { return executionCount; }
        public long getPeakMemoryUsage() { return peakMemoryUsage; }
        public long getCurrentMemoryUsage() { return currentMemoryUsage; }
        public long getTotalMemoryAllocated() { return totalMemoryAllocated; }
        public long getTotalErrors() { return totalErrors; }
        public Map<String, AtomicLong> getErrorsByType() { return errorsByType; }
        public Map<String, StrategyMetrics> getStrategyMetrics() { return strategyMetrics; }
        public long getStartTime() { return startTime; }
        public long getSnapshotTime() { return snapshotTime; }
        
        public long getUptime() {
            return snapshotTime - startTime;
        }
        
        public double getAverageExecutionTime() {
            return executionCount > 0 ? (double) totalExecutionTime / executionCount : 0.0;
        }
        
        public double getEntitiesPerSecond() {
            long uptime = getUptime();
            return uptime > 0 ? (double) totalEntitiesProcessed / (uptime / 1000.0) : 0.0;
        }
        
        public double getRemovalRate() {
            return totalEntitiesProcessed > 0 ? (double) totalEntitiesRemoved / totalEntitiesProcessed * 100.0 : 0.0;
        }
        
        public double getErrorRate() {
            return executionCount > 0 ? (double) totalErrors / executionCount * 100.0 : 0.0;
        }
    }
}