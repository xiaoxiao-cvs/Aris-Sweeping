package com.xiaoxiao.arissweeping.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;

import java.util.*;

/**
 * 畜牧业热点信息类 - 专门针对动物密度管理
 * 简化的数据结构，避免过度复杂化
 */
public class LivestockHotspotInfo {
    private final String worldName;
    private final int chunkX;
    private final int chunkZ;
    private final int totalAnimals;
    private final int totalEntities;
    private final Map<EntityType, Integer> animalCounts;
    private final Map<EntityType, Integer> allEntityCounts;
    private final double livestockDensity; // 畜牧业密度评分
    private final double performanceImpact; // 性能影响评分
    private final boolean exceedsLimit; // 是否超过配置限制
    private final long scanTime;
    private final SparkEntityMetrics sparkMetrics;
    
    public LivestockHotspotInfo(String worldName, int chunkX, int chunkZ, 
                               int totalAnimals, int totalEntities,
                               Map<EntityType, Integer> animalCounts,
                               Map<EntityType, Integer> allEntityCounts,
                               double livestockDensity, double performanceImpact,
                               boolean exceedsLimit, SparkEntityMetrics sparkMetrics) {
        this.worldName = worldName;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.totalAnimals = totalAnimals;
        this.totalEntities = totalEntities;
        this.animalCounts = new HashMap<>(animalCounts);
        this.allEntityCounts = new HashMap<>(allEntityCounts);
        this.livestockDensity = livestockDensity;
        this.performanceImpact = performanceImpact;
        this.exceedsLimit = exceedsLimit;
        this.scanTime = System.currentTimeMillis();
        this.sparkMetrics = sparkMetrics;
    }
    
    /**
     * 使用EntityCounter构造的便利方法
     */
    public LivestockHotspotInfo(String worldName, int chunkX, int chunkZ,
                               EntityCounter animalCounter, EntityCounter allEntityCounter,
                               double livestockDensity, double performanceImpact,
                               boolean exceedsLimit, SparkEntityMetrics sparkMetrics) {
        this(worldName, chunkX, chunkZ,
             animalCounter.getTotalCount(), allEntityCounter.getTotalCount(),
             animalCounter.toMap(), allEntityCounter.toMap(),
             livestockDensity, performanceImpact, exceedsLimit, sparkMetrics);
    }
    
    // Getters
    public String getWorldName() { return worldName; }
    public int getChunkX() { return chunkX; }
    public int getChunkZ() { return chunkZ; }
    public int getTotalAnimals() { return totalAnimals; }
    public int getTotalEntities() { return totalEntities; }
    public Map<EntityType, Integer> getAnimalCounts() { return new HashMap<>(animalCounts); }
    public Map<EntityType, Integer> getAllEntityCounts() { return new HashMap<>(allEntityCounts); }
    public double getLivestockDensity() { return livestockDensity; }
    public double getPerformanceImpact() { return performanceImpact; }
    public boolean isExceedsLimit() { return exceedsLimit; }
    public boolean exceedsLimit() { return exceedsLimit; } // 兼容方法
    public long getScanTime() { return scanTime; }
    public SparkEntityMetrics getSparkMetrics() { return sparkMetrics; }
    
    public String getCoordinates() {
        return String.format("(%d, %d)", chunkX, chunkZ);
    }
    
    public Location getCenterLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            return new Location(world, chunkX * 16 + 8, 64, chunkZ * 16 + 8);
        }
        return null;
    }
    
    /**
     * 获取清理优先级（数值越高优先级越高）
     */
    public int getCleanupPriority() {
        int priority = 0;
        
        // 基础优先级：超标程度
        if (exceedsLimit) {
            priority += 100;
        }
        
        // 性能影响加权
        if (sparkMetrics != null && sparkMetrics.isPerformanceCritical()) {
            priority += 50;
        }
        
        // 密度评分加权
        priority += (int) (livestockDensity * 10);
        
        // 动物数量加权
        priority += totalAnimals;
        
        return priority;
    }
}