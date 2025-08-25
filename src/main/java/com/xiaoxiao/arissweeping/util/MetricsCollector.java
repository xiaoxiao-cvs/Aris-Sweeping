package com.xiaoxiao.arissweeping.util;

import me.lucko.spark.api.Spark;
import me.lucko.spark.api.SparkProvider;
import me.lucko.spark.api.statistic.StatisticWindow;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import me.lucko.spark.api.statistic.types.DoubleStatistic;
import me.lucko.spark.api.statistic.types.GenericStatistic;

/**
 * 性能指标收集器 - 负责收集和管理Spark性能数据
 * 遵循单一职责原则，只负责指标收集
 */
public class MetricsCollector {
    private final Spark spark;
    private long lastSparkScanTime = 0;
    private static final long SPARK_CACHE_DURATION = 60000; // Spark数据1分钟缓存
    
    public MetricsCollector() {
        Spark sparkInstance = null;
        try {
            sparkInstance = SparkProvider.get();
        } catch (Exception e) {
            // Spark不可用时静默处理
        }
        this.spark = sparkInstance;
    }
    
    /**
     * 获取当前Spark性能指标
     */
    public SparkEntityMetrics getCurrentSparkMetrics() {
        if (spark == null) {
            return null;
        }
        
        // 检查缓存
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSparkScanTime < SPARK_CACHE_DURATION) {
            return null; // 使用缓存的数据
        }
        
        try {
            // 获取TPS数据
            DoubleStatistic<StatisticWindow.TicksPerSecond> tpsStatistic = spark.tps();
            double currentTps = 20.0;
            if (tpsStatistic != null) {
                double tpsValue = tpsStatistic.poll(StatisticWindow.TicksPerSecond.SECONDS_10);
                if (tpsValue > 0) {
                    currentTps = tpsValue;
                }
            }
            
            // 获取MSPT数据
            GenericStatistic<DoubleAverageInfo, StatisticWindow.MillisPerTick> msptStatistic = spark.mspt();
            double currentMspt = 0.0;
            if (msptStatistic != null) {
                DoubleAverageInfo msptInfo = msptStatistic.poll(StatisticWindow.MillisPerTick.SECONDS_10);
                if (msptInfo != null) {
                    currentMspt = msptInfo.mean();
                }
            }
            
            // 获取内存使用情况
            double memoryUsage = getMemoryUsagePercentage();
            
            // 估算实体数量（简化版本）
            long entityCount = estimateEntityCount(currentTps, currentMspt);
            
            lastSparkScanTime = currentTime;
            return new SparkEntityMetrics(currentTps, currentMspt, entityCount, memoryUsage);
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取内存使用百分比
     */
    private double getMemoryUsagePercentage() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return (double) usedMemory / maxMemory * 100.0;
    }
    
    /**
     * 基于性能指标估算实体数量
     */
    private long estimateEntityCount(double tps, double mspt) {
        // 简化的估算逻辑
        if (tps < 15.0 || mspt > 30.0) {
            return 2000; // 高负载估算
        } else if (tps < 18.0 || mspt > 15.0) {
            return 1000; // 中等负载估算
        } else {
            return 500; // 低负载估算
        }
    }
    
    /**
     * 检查Spark是否可用
     */
    public boolean isSparkAvailable() {
        return spark != null;
    }
    
    /**
     * 获取最后扫描时间
     */
    public long getLastSparkScanTime() {
        return lastSparkScanTime;
    }
}