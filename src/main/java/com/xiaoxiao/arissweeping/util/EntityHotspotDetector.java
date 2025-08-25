package com.xiaoxiao.arissweeping.util;

import com.xiaoxiao.arissweeping.ArisSweeping;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 重构后的实体热点检测器 - 遵循Linus的简化原则
 * 职责明确：协调各个组件，提供统一的API接口
 */
public class EntityHotspotDetector {
    private final ArisSweeping plugin;
    private final HotspotScanner scanner;
    private final MetricsCollector metricsCollector;
    private final StatisticsCalculator statisticsCalculator;
    private long lastScanTime = 0;
    
    public EntityHotspotDetector(ArisSweeping plugin) {
        this.plugin = plugin;
        this.scanner = new HotspotScanner(plugin);
        this.metricsCollector = new MetricsCollector();
        this.statisticsCalculator = new StatisticsCalculator();
    }
    
    /**
     * 异步扫描畜牧业热点
     */
    public void scanLivestockHotspotsAsync(LivestockScanCallback callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // 直接执行扫描，不使用缓存
                    SparkEntityMetrics sparkMetrics = metricsCollector.getCurrentSparkMetrics();
                    List<LivestockHotspotInfo> hotspots = scanner.scanAllLivestockHotspots(sparkMetrics);
                    
                    // 更新扫描时间
                    lastScanTime = System.currentTimeMillis();
                    
                    // 计算统计信息
                    LivestockStatistics statistics = statisticsCalculator.calculateLivestockStatistics(hotspots, sparkMetrics);
                    
                    callback.onComplete(hotspots, statistics);
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }
    
    /**
     * 兼容性方法：异步扫描热点
     */
    public void scanHotspotsAsync(HotspotScanCallback callback) {
        scanLivestockHotspotsAsync(new LivestockScanCallback() {
            @Override
            public void onComplete(List<LivestockHotspotInfo> hotspots, LivestockStatistics statistics) {
                // 转换为兼容格式
                List<HotspotInfo> compatibleHotspots = hotspots.stream()
                    .map(h -> new HotspotInfo(h.getWorldName(), h.getChunkX(), h.getChunkZ(), 
                                            h.getTotalEntities(), h.getAllEntityCounts(), h.getLivestockDensity()))
                    .collect(Collectors.toList());
                callback.onComplete(compatibleHotspots);
            }
            
            @Override
            public void onError(Exception error) {
                callback.onError(error);
            }
        });
    }
    
    /**
     * 获取当前Spark性能指标
     */
    public SparkEntityMetrics getCurrentSparkMetrics() {
        return metricsCollector.getCurrentSparkMetrics();
    }
    
    /**
     * 获取排名前N的畜牧业热点
     */
    public List<LivestockHotspotInfo> getTopLivestockHotspots(int limit) {
        // 直接扫描，不使用缓存
        SparkEntityMetrics sparkMetrics = metricsCollector.getCurrentSparkMetrics();
        List<LivestockHotspotInfo> hotspots = scanner.scanAllLivestockHotspots(sparkMetrics);
        return statisticsCalculator.getTopHotspots(hotspots, limit);
    }
    
    /**
     * 获取特定世界的畜牧业热点
     */
    public List<LivestockHotspotInfo> getWorldLivestockHotspots(String worldName) {
        // 直接扫描，不使用缓存
        SparkEntityMetrics sparkMetrics = metricsCollector.getCurrentSparkMetrics();
        List<LivestockHotspotInfo> hotspots = scanner.scanAllLivestockHotspots(sparkMetrics);
        return statisticsCalculator.getWorldHotspots(hotspots, worldName);
    }
    
    /**
     * 获取超过限制的畜牧业热点
     */
    public List<LivestockHotspotInfo> getExceedingLivestockHotspots() {
        // 直接扫描，不使用缓存
        SparkEntityMetrics sparkMetrics = metricsCollector.getCurrentSparkMetrics();
        List<LivestockHotspotInfo> hotspots = scanner.scanAllLivestockHotspots(sparkMetrics);
        return statisticsCalculator.getExceedingHotspots(hotspots);
    }
    
    /**
     * 获取畜牧业统计信息
     */
    public LivestockStatistics getLivestockStatistics() {
        // 直接扫描，不使用缓存
        SparkEntityMetrics sparkMetrics = metricsCollector.getCurrentSparkMetrics();
        List<LivestockHotspotInfo> hotspots = scanner.scanAllLivestockHotspots(sparkMetrics);
        return statisticsCalculator.calculateLivestockStatistics(hotspots, sparkMetrics);
    }
    
    /**
     * 兼容性方法：获取排名前N的热点
     */
    public List<HotspotInfo> getTopHotspots(String worldName, int limit) {
        List<LivestockHotspotInfo> worldHotspots = getWorldLivestockHotspots(worldName);
        return worldHotspots.stream()
            .limit(limit)
            .map(h -> new HotspotInfo(h.getWorldName(), h.getChunkX(), h.getChunkZ(), 
                                    h.getTotalEntities(), h.getAllEntityCounts(), h.getLivestockDensity()))
            .collect(Collectors.toList());
    }
    
    /**
     * 兼容性方法：获取排名前N的热点
     */
    public List<HotspotInfo> getTopHotspots(int limit) {
        List<LivestockHotspotInfo> topHotspots = getTopLivestockHotspots(limit);
        return topHotspots.stream()
            .map(h -> new HotspotInfo(h.getWorldName(), h.getChunkX(), h.getChunkZ(), 
                                    h.getTotalEntities(), h.getAllEntityCounts(), h.getLivestockDensity()))
            .collect(Collectors.toList());
    }
    
    /**
     * 清空缓存（已简化，无实际操作）
     */
    public void clearCache() {
        // 缓存已移除，此方法保留用于兼容性
    }
    
    /**
     * 检查是否正在扫描
     */
    public boolean isScanning() {
        return false; // 简化实现，不需要复杂的状态跟踪
    }
    
    /**
     * 获取最后扫描时间
     */
    public long getLastScanTime() {
        return lastScanTime;
    }
    
    /**
     * 获取最后Spark扫描时间
     */
    public long getLastSparkScanTime() {
        return metricsCollector.getLastSparkScanTime();
    }
    
    /**
     * 兼容性：保留原有HotspotInfo接口
     */
    public static class HotspotInfo extends LivestockHotspotInfo {
        public HotspotInfo(String worldName, int chunkX, int chunkZ, int totalEntities, 
                          Map<EntityType, Integer> entityCounts, double density) {
            super(worldName, chunkX, chunkZ, 
                  countAnimals(entityCounts), totalEntities,
                  filterAnimals(entityCounts), entityCounts,
                  density, density, 
                  totalEntities > 50, // 默认阈值
                  null);
        }
        
        public int getTotalEntities() { return super.getTotalEntities(); }
        public Map<EntityType, Integer> getEntityCounts() { return super.getAllEntityCounts(); }
        public double getDensity() { return super.getLivestockDensity(); }
        public double getDensityScore() { return getLivestockDensity(); }
        
        private static int countAnimals(Map<EntityType, Integer> entityCounts) {
            return entityCounts.entrySet().stream()
                .filter(entry -> isLivestockTypeStatic(entry.getKey()))
                .mapToInt(Map.Entry::getValue)
                .sum();
        }
        
        private static Map<EntityType, Integer> filterAnimals(Map<EntityType, Integer> entityCounts) {
            return entityCounts.entrySet().stream()
                .filter(entry -> isLivestockTypeStatic(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        
        private static boolean isLivestockTypeStatic(EntityType type) {
            switch (type) {
                case COW:
                case PIG:
                case SHEEP:
                case CHICKEN:
                case HORSE:
                case DONKEY:
                case MULE:
                case LLAMA:
                case RABBIT:
                case WOLF:
                case CAT:
                case OCELOT:
                case PARROT:
                case TURTLE:
                case PANDA:
                case FOX:
                case BEE:
                case GOAT:
                case AXOLOTL:
                    return true;
                default:
                    return false;
            }
        }
    }
    
    /**
     * 畜牧业扫描回调接口
     */
    public interface LivestockScanCallback {
        void onComplete(List<LivestockHotspotInfo> hotspots, LivestockStatistics statistics);
        void onError(Exception error);
    }
    
    /**
     * 热点扫描回调接口
     */
    public interface HotspotScanCallback {
        void onComplete(List<HotspotInfo> hotspots);
        void onError(Exception error);
    }
}