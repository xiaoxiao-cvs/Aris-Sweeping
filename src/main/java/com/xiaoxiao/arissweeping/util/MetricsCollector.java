package com.xiaoxiao.arissweeping.util;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;

/**
 * 性能指标收集器 - 负责收集和管理性能数据
 * 遵循单一职责原则，只负责指标收集
 * 支持有无Spark插件的环境
 */
public class MetricsCollector {
    private Object spark;
    private boolean sparkAvailable = false;
    private long lastSparkScanTime = 0;
    private static final long SPARK_CACHE_DURATION = 60000; // 数据1分钟缓存
    
    public MetricsCollector() {
        try {
            // 使用反射动态加载Spark API，避免直接依赖
            Class<?> sparkProviderClass = Class.forName("me.lucko.spark.api.SparkProvider");
            this.spark = sparkProviderClass.getMethod("get").invoke(null);
            this.sparkAvailable = true;
        } catch (Exception e) {
            // Spark不可用时静默处理，使用内置性能监控
            this.spark = null;
            this.sparkAvailable = false;
        }
    }
    
    /**
     * 获取当前性能指标
     */
    public SparkEntityMetrics getCurrentSparkMetrics() {
        // 检查缓存
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSparkScanTime < SPARK_CACHE_DURATION) {
            return null; // 使用缓存的数据
        }
        
        try {
            double currentTps = 20.0;
            double currentMspt = 0.0;
            
            if (sparkAvailable && spark != null) {
                // 使用Spark API获取精确数据
                currentTps = getSparkTps();
                currentMspt = getSparkMspt();
            } else {
                  // 没有Spark时使用估算值
                  currentTps = estimateTpsFromServerLoad();
                  currentMspt = calculateMsptFromTps(currentTps);
              }
            
            // 获取内存使用情况
            double memoryUsage = getMemoryUsagePercentage();
            
            // 获取实际实体数量
            long entityCount = getTotalEntityCount();
            
            lastSparkScanTime = currentTime;
            return new SparkEntityMetrics(currentTps, currentMspt, entityCount, memoryUsage);
            
        } catch (Exception e) {
            // 发生异常时返回默认值
            return new SparkEntityMetrics(20.0, 0.0, getTotalEntityCount(), getMemoryUsagePercentage());
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
     * 使用反射获取Spark TPS数据
     */
    private double getSparkTps() {
        try {
            Object tpsStatistic = spark.getClass().getMethod("tps").invoke(spark);
            if (tpsStatistic != null) {
                Class<?> windowClass = Class.forName("me.lucko.spark.api.statistic.StatisticWindow$TicksPerSecond");
                Object seconds10 = windowClass.getField("SECONDS_10").get(null);
                Object result = tpsStatistic.getClass().getMethod("poll", windowClass).invoke(tpsStatistic, seconds10);
                if (result instanceof Double) {
                    double tpsValue = (Double) result;
                    return tpsValue > 0 ? tpsValue : 20.0;
                }
            }
        } catch (Exception e) {
            // 反射失败时返回默认值
        }
        return 20.0;
    }
    
    /**
     * 使用反射获取Spark MSPT数据
     */
    private double getSparkMspt() {
        try {
            Object msptStatistic = spark.getClass().getMethod("mspt").invoke(spark);
            if (msptStatistic != null) {
                Class<?> windowClass = Class.forName("me.lucko.spark.api.statistic.StatisticWindow$MillisPerTick");
                Object seconds10 = windowClass.getField("SECONDS_10").get(null);
                Object result = msptStatistic.getClass().getMethod("poll", windowClass).invoke(msptStatistic, seconds10);
                if (result != null) {
                    Object meanValue = result.getClass().getMethod("mean").invoke(result);
                    if (meanValue instanceof Double) {
                        return (Double) meanValue;
                    }
                }
            }
        } catch (Exception e) {
            // 反射失败时返回默认值
        }
        return 0.0;
    }
    
    /**
     * 根据TPS计算MSPT
     */
    private double calculateMsptFromTps(double tps) {
        if (tps <= 0) return 50.0;
        return Math.max(0, 50.0 - (tps * 2.5));
    }
    
    /**
     * 根据服务器负载估算TPS
     */
    private double estimateTpsFromServerLoad() {
        try {
            // 基于内存使用率和实体数量估算TPS
            double memoryUsage = getMemoryUsagePercentage();
            long entityCount = getTotalEntityCount();
            
            // 简单的估算逻辑
            double baseTps = 20.0;
            
            // 内存使用率影响
            if (memoryUsage > 80) {
                baseTps -= 5.0;
            } else if (memoryUsage > 60) {
                baseTps -= 2.0;
            }
            
            // 实体数量影响
            if (entityCount > 2000) {
                baseTps -= 3.0;
            } else if (entityCount > 1000) {
                baseTps -= 1.5;
            }
            
            return Math.max(5.0, baseTps); // 最低5 TPS
        } catch (Exception e) {
            return 20.0; // 默认值
        }
    }
    
    /**
     * 获取所有世界的实体总数
     */
    private long getTotalEntityCount() {
        long totalEntities = 0;
        for (World world : Bukkit.getWorlds()) {
            totalEntities += world.getEntities().size();
        }
        return totalEntities;
    }
    
    /**
     * 检查Spark是否可用
     */
    public boolean isSparkAvailable() {
        return sparkAvailable;
    }
    
    /**
     * 获取最后扫描时间
     */
    public long getLastSparkScanTime() {
        return lastSparkScanTime;
    }
}