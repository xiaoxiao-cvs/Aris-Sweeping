package com.xiaoxiao.arissweeping.util;

import com.xiaoxiao.arissweeping.ArisSweeping;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.*;

/**
 * 热点扫描器 - 负责扫描世界中的实体热点
 * 遵循单一职责原则，只负责扫描逻辑
 */
public class HotspotScanner {
    private final ArisSweeping plugin;
    
    public HotspotScanner(ArisSweeping plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 扫描所有世界的畜牧业热点
     */
    public List<LivestockHotspotInfo> scanAllLivestockHotspots(SparkEntityMetrics sparkMetrics) {
        List<LivestockHotspotInfo> allHotspots = new ArrayList<>();
        
        for (World world : Bukkit.getWorlds()) {
            if (world == null || world.getEnvironment() != World.Environment.NORMAL) continue;
            
            List<LivestockHotspotInfo> worldHotspots = scanWorldLivestockHotspots(world, sparkMetrics);
            allHotspots.addAll(worldHotspots);
        }
        
        // 按清理优先级排序
        allHotspots.sort((a, b) -> Integer.compare(b.getCleanupPriority(), a.getCleanupPriority()));
        
        return allHotspots;
    }
    
    /**
     * 扫描单个世界的畜牧业热点
     */
    public List<LivestockHotspotInfo> scanWorldLivestockHotspots(World world, SparkEntityMetrics sparkMetrics) {
        List<LivestockHotspotInfo> hotspots = new ArrayList<>();
        Map<String, ChunkLivestockData> chunkData = new HashMap<>();
        
        // 收集所有区块的畜牧业数据
        for (Chunk chunk : world.getLoadedChunks()) {
            if (chunk == null) continue;
            
            ChunkLivestockData data = scanChunkLivestock(chunk);
            if (data.totalAnimals > 0 || data.totalEntities > plugin.getModConfig().getMaxAnimalsPerChunk()) {
                String chunkKey = chunk.getX() + "_" + chunk.getZ();
                chunkData.put(chunkKey, data);
            }
        }
        
        // 创建热点信息
        for (ChunkLivestockData data : chunkData.values()) {
            double livestockDensity = calculateLivestockDensityScore(data.totalAnimals, data.animalCounts);
            double performanceImpact = calculatePerformanceImpact(data.totalEntities, data.allEntityCounts, sparkMetrics);
            boolean exceedsLimit = data.totalAnimals > plugin.getModConfig().getMaxAnimalsPerChunk();
            
            // 记录有意义的畜牧业热点
            if (livestockDensity > 5.0 || exceedsLimit || 
                (sparkMetrics != null && sparkMetrics.isPerformanceCritical() && data.totalAnimals > 10)) {
                
                LivestockHotspotInfo hotspot = new LivestockHotspotInfo(
                    world.getName(),
                    data.chunkX,
                    data.chunkZ,
                    data.totalAnimals,
                    data.totalEntities,
                    data.animalCounts,
                    data.allEntityCounts,
                    livestockDensity,
                    performanceImpact,
                    exceedsLimit,
                    sparkMetrics
                );
                hotspots.add(hotspot);
            }
        }
        
        return hotspots;
    }
    
    /**
     * 扫描单个区块的畜牧业数据
     */
    private ChunkLivestockData scanChunkLivestock(Chunk chunk) {
        EntityCounter animalCounter = new EntityCounter();
        EntityCounter allEntityCounter = new EntityCounter();
        
        for (Entity entity : chunk.getEntities()) {
            if (entity != null) {
                EntityType type = entity.getType();
                allEntityCounter.increment(type);
                
                // 检查是否为动物
                if (EntityTypeUtils.isAnimal(entity) || isLivestockType(type)) {
                    animalCounter.increment(type);
                }
            }
        }
        
        return new ChunkLivestockData(chunk.getX(), chunk.getZ(), 
            animalCounter.getTotalCount(), allEntityCounter.getTotalCount(), 
            animalCounter.toMap(), allEntityCounter.toMap());
    }
    
    /**
     * 计算畜牧业密度评分
     */
    private double calculateLivestockDensityScore(int totalAnimals, Map<EntityType, Integer> animalCounts) {
        return calculateDensityScore(totalAnimals, animalCounts, true);
    }
    
    /**
     * 计算性能影响评分
     */
    private double calculatePerformanceImpact(int totalEntities, Map<EntityType, Integer> entityCounts, SparkEntityMetrics sparkMetrics) {
        double baseScore = calculateDensityScore(totalEntities, entityCounts, false);
        
        if (sparkMetrics != null && sparkMetrics.isPerformanceCritical()) {
            return baseScore * 1.5;
        }
        
        return baseScore;
    }
    
    /**
     * 统一的密度评分计算方法
     */
    private double calculateDensityScore(int totalEntities, Map<EntityType, Integer> entityCounts, boolean isLivestockMode) {
        double baseMultiplier = isLivestockMode ? 2.0 : 1.0;
        double score = totalEntities * baseMultiplier;
        
        // 根据实体类型加权
        for (Map.Entry<EntityType, Integer> entry : entityCounts.entrySet()) {
            EntityType type = entry.getKey();
            int count = entry.getValue();
            double weight = getUnifiedEntityWeight(type, isLivestockMode);
            score += count * weight;
        }
        
        return score;
    }
    
    /**
     * 获取统一的实体权重
     */
    private double getUnifiedEntityWeight(EntityType type, boolean isLivestockMode) {
        if (isLivestockMode) {
            return getLivestockWeight(type);
        }
        
        // 通用实体权重
        switch (type) {
            case VILLAGER:
            case IRON_GOLEM:
                return 3.0;
            case ZOMBIE:
            case SKELETON:
            case CREEPER:
                return 2.5;
            case ITEM_FRAME:
            case ARMOR_STAND:
                return 1.5;
            default:
                return 1.0;
        }
    }
    
    /**
     * 获取畜牧业实体权重
     */
    private double getLivestockWeight(EntityType type) {
        switch (type) {
            case COW:
            case PIG:
            case SHEEP:
                return 2.0;
            case CHICKEN:
            case RABBIT:
                return 1.5;
            case HORSE:
            case DONKEY:
            case MULE:
                return 3.0;
            default:
                return 1.0;
        }
    }
    
    /**
     * 检查是否为畜牧业类型
     */
    private boolean isLivestockType(EntityType type) {
        switch (type) {
            case COW:
            case PIG:
            case SHEEP:
            case CHICKEN:
            case RABBIT:
            case HORSE:
            case DONKEY:
            case MULE:
            case LLAMA:
            case GOAT:
            case AXOLOTL:
            case BEE:
            case CAT:
            case OCELOT:
            case WOLF:
            case PARROT:
            case TROPICAL_FISH:
            case COD:
            case SALMON:
            case PUFFERFISH:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * 区块畜牧业数据
     */
    public static class ChunkLivestockData {
        public final int chunkX;
        public final int chunkZ;
        public final int totalAnimals;
        public final int totalEntities;
        public final Map<EntityType, Integer> animalCounts;
        public final Map<EntityType, Integer> allEntityCounts;
        
        public ChunkLivestockData(int chunkX, int chunkZ, int totalAnimals, int totalEntities,
                                 Map<EntityType, Integer> animalCounts, Map<EntityType, Integer> allEntityCounts) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.totalAnimals = totalAnimals;
            this.totalEntities = totalEntities;
            this.animalCounts = animalCounts;
            this.allEntityCounts = allEntityCounts;
        }
    }
}