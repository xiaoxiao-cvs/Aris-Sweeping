package com.xiaoxiao.arissweeping.util;

import com.xiaoxiao.arissweeping.ArisSweeping;
import me.lucko.spark.api.Spark;
import me.lucko.spark.api.SparkProvider;
import me.lucko.spark.api.statistic.StatisticWindow;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import me.lucko.spark.api.statistic.types.DoubleStatistic;
import me.lucko.spark.api.statistic.types.GenericStatistic;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 全新的实体热点检测器 - 完全基于Spark API的畜牧业管理系统
 * 提供异步扫描、缓存机制、智能实体权重计算
 * 专门针对畜牧业密度控制进行优化
 */
public class EntityHotspotDetector {
    private final ArisSweeping plugin;
    private final Spark spark;
    private final Map<String, LivestockHotspotInfo> livestockCache = new ConcurrentHashMap<>();
    private final Map<String, SparkEntityMetrics> sparkMetricsCache = new ConcurrentHashMap<>();
    private final Map<String, HotspotInfo> hotspotCache = new ConcurrentHashMap<>(); // 兼容性缓存
    private long lastScanTime = 0;
    private long lastSparkScanTime = 0;
    private static final long CACHE_DURATION = 30000; // 30秒缓存
    private static final long SPARK_CACHE_DURATION = 60000; // Spark数据1分钟缓存
    
    public EntityHotspotDetector(ArisSweeping plugin) {
        this.plugin = plugin;
        try {
            this.spark = SparkProvider.get();
            plugin.getLogger().info("[EntityHotspotDetector] 成功连接到Spark API - 启用畜牧业智能管理");
            initializeSparkMetrics();
        } catch (Exception e) {
            plugin.getLogger().severe("[EntityHotspotDetector] 无法连接到Spark API: " + e.getMessage());
            throw new RuntimeException("Spark API不可用，畜牧业管理系统无法启动", e);
        }
    }
    
    /**
     * 初始化Spark性能指标监控
     */
    private void initializeSparkMetrics() {
        try {
            // 获取TPS和MSPT数据用于智能清理决策
            DoubleStatistic<StatisticWindow.TicksPerSecond> tps = spark.tps();
            GenericStatistic<DoubleAverageInfo, StatisticWindow.MillisPerTick> mspt = spark.mspt();
            
            plugin.getLogger().info("[EntityHotspotDetector] Spark性能监控已初始化");
        } catch (Exception e) {
            plugin.getLogger().warning("[EntityHotspotDetector] 初始化Spark性能监控时发生异常: " + e.getMessage());
        }
    }
    
    /**
     * Spark性能指标缓存类
     */
    public static class SparkEntityMetrics {
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
        public long getTotalEntities() { return entityCount; } // 添加缺失的方法
        public double getTps() { return currentTps; } // 添加兼容方法
        public double getMspt() { return currentMspt; } // 添加兼容方法
        public double getMemoryUsage() { return memoryUsage; }
        public long getTimestamp() { return timestamp; }
        
        public boolean isPerformanceCritical() {
            return currentTps < 15.0 || currentMspt > 50.0;
        }
    }
    
    /**
     * 畜牧业热点信息类 - 专门针对动物密度管理
     */
    public static class LivestockHotspotInfo {
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
        public boolean exceedsLimit() { return exceedsLimit; }
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
        
        private static int countAnimals(Map<EntityType, Integer> entityCounts) {
            return entityCounts.entrySet().stream()
                .filter(entry -> isAnimalType(entry.getKey()))
                .mapToInt(Map.Entry::getValue)
                .sum();
        }
        
        private static Map<EntityType, Integer> filterAnimals(Map<EntityType, Integer> entityCounts) {
            return entityCounts.entrySet().stream()
                .filter(entry -> isAnimalType(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        
        private static boolean isAnimalType(EntityType type) {
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
        
        public double getDensityScore() {
            return getLivestockDensity();
        }
    }
    
    /**
     * 异步扫描畜牧业热点 - 使用Spark API增强
     * @param callback 扫描完成后的回调
     */
    public void scanLivestockHotspotsAsync(LivestockScanCallback callback) {
        // 检查缓存
        if (System.currentTimeMillis() - lastScanTime < CACHE_DURATION && !livestockCache.isEmpty()) {
            LivestockStatistics statistics = getLivestockStatistics();
            callback.onComplete(new ArrayList<>(livestockCache.values()), statistics);
            return;
        }
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // 获取Spark性能指标
                    SparkEntityMetrics sparkMetrics = getCurrentSparkMetrics();
                    List<LivestockHotspotInfo> hotspots = scanAllLivestockHotspots(sparkMetrics);
                    
                    // 生成统计信息
                    LivestockStatistics statistics = getLivestockStatistics();
                    
                    // 回到主线程执行回调
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            callback.onComplete(hotspots, statistics);
                        }
                    }.runTask(plugin);
                    
                } catch (Exception e) {
                    plugin.getLogger().warning("[EntityHotspotDetector] 扫描畜牧业热点时发生异常: " + e.getMessage());
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            callback.onError(e);
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskAsynchronously(plugin);
    }
    
    /**
     * 兼容性方法：异步扫描所有世界的实体热点
     * @param callback 扫描完成后的回调
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
        try {
            // 检查Spark指标缓存
            if (System.currentTimeMillis() - lastSparkScanTime < SPARK_CACHE_DURATION && sparkMetricsCache != null) {
                return sparkMetricsCache.get("global");
            }
            
            DoubleStatistic<StatisticWindow.TicksPerSecond> tps = spark.tps();
            GenericStatistic<DoubleAverageInfo, StatisticWindow.MillisPerTick> mspt = spark.mspt();
            
            // 添加null检查防止NullPointerException
            double currentTps = (tps != null) ? tps.poll(StatisticWindow.TicksPerSecond.SECONDS_5) : 20.0;
            double currentMspt = 0.0;
            if (mspt != null) {
                DoubleAverageInfo msptInfo = mspt.poll(StatisticWindow.MillisPerTick.SECONDS_10);
                currentMspt = msptInfo != null ? msptInfo.mean() : 0.0;
            }
            
            // 获取实体总数
            long totalEntities = Bukkit.getWorlds().stream()
                .mapToLong(world -> world.getEntities().size())
                .sum();
            
            // 获取内存使用情况
            Runtime runtime = Runtime.getRuntime();
            double memoryUsage = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory() * 100;
            
            SparkEntityMetrics metrics = new SparkEntityMetrics(currentTps, currentMspt, totalEntities, memoryUsage);
            
            // 更新缓存
            sparkMetricsCache.put("global", metrics);
            lastSparkScanTime = System.currentTimeMillis();
            
            return metrics;
            
        } catch (Exception e) {
            plugin.getLogger().warning("[EntityHotspotDetector] 获取Spark指标时发生异常: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 扫描所有世界的畜牧业热点
     */
    private List<LivestockHotspotInfo> scanAllLivestockHotspots(SparkEntityMetrics sparkMetrics) {
        List<LivestockHotspotInfo> allHotspots = new ArrayList<>();
        livestockCache.clear();
        
        for (World world : Bukkit.getWorlds()) {
            if (world == null || world.getEnvironment() != World.Environment.NORMAL) continue;
            
            try {
                List<LivestockHotspotInfo> worldHotspots = scanWorldLivestockHotspots(world, sparkMetrics);
                allHotspots.addAll(worldHotspots);
                
                // 更新缓存
                for (LivestockHotspotInfo hotspot : worldHotspots) {
                    String key = hotspot.getWorldName() + "_" + hotspot.getChunkX() + "_" + hotspot.getChunkZ();
                    livestockCache.put(key, hotspot);
                }
                
            } catch (Exception e) {
                plugin.getLogger().warning("[EntityHotspotDetector] 扫描世界 " + world.getName() + " 畜牧业热点时发生异常: " + e.getMessage());
            }
        }
        
        // 按清理优先级排序
        allHotspots.sort((a, b) -> Integer.compare(b.getCleanupPriority(), a.getCleanupPriority()));
        
        lastScanTime = System.currentTimeMillis();
        return allHotspots;
    }
    
    /**
     * 兼容性方法：扫描所有世界的实体热点
     */
    private List<HotspotInfo> scanAllWorlds() {
        SparkEntityMetrics sparkMetrics = getCurrentSparkMetrics();
        List<LivestockHotspotInfo> livestockHotspots = scanAllLivestockHotspots(sparkMetrics);
        
        // 转换为兼容格式
        return livestockHotspots.stream()
            .map(h -> new HotspotInfo(h.getWorldName(), h.getChunkX(), h.getChunkZ(), 
                                    h.getTotalEntities(), h.getAllEntityCounts(), h.getLivestockDensity()))
            .collect(Collectors.toList());
    }
    
    /**
     * 扫描单个世界的畜牧业热点
     */
    private List<LivestockHotspotInfo> scanWorldLivestockHotspots(World world, SparkEntityMetrics sparkMetrics) {
        List<LivestockHotspotInfo> hotspots = new ArrayList<>();
        Map<String, ChunkLivestockData> chunkData = new HashMap<>();
        
        // 收集所有区块的畜牧业数据
        for (Chunk chunk : world.getLoadedChunks()) {
            if (chunk == null) continue;
            
            Map<EntityType, Integer> animalCounts = new HashMap<>();
            Map<EntityType, Integer> allEntityCounts = new HashMap<>();
            int totalAnimals = 0;
            int totalEntities = 0;
            
            for (Entity entity : chunk.getEntities()) {
                if (entity != null) {
                    EntityType type = entity.getType();
                    allEntityCounts.put(type, allEntityCounts.getOrDefault(type, 0) + 1);
                    totalEntities++;
                    
                    // 检查是否为动物
                    if (entity instanceof Animals || isLivestockType(type)) {
                        animalCounts.put(type, animalCounts.getOrDefault(type, 0) + 1);
                        totalAnimals++;
                    }
                }
            }
            
            if (totalAnimals > 0 || totalEntities > plugin.getModConfig().getMaxAnimalsPerChunk()) {
                String chunkKey = chunk.getX() + "_" + chunk.getZ();
                chunkData.put(chunkKey, new ChunkLivestockData(chunk.getX(), chunk.getZ(), 
                    totalAnimals, totalEntities, animalCounts, allEntityCounts));
            }
        }
        
        // 计算畜牧业密度评分并创建热点信息
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
     * 兼容性方法：扫描单个世界的实体热点
     */
    private List<HotspotInfo> scanWorldHotspots(World world) {
        SparkEntityMetrics sparkMetrics = getCurrentSparkMetrics();
        List<LivestockHotspotInfo> livestockHotspots = scanWorldLivestockHotspots(world, sparkMetrics);
        
        // 转换为兼容格式
        return livestockHotspots.stream()
            .map(h -> new HotspotInfo(h.getWorldName(), h.getChunkX(), h.getChunkZ(), 
                                    h.getTotalEntities(), h.getAllEntityCounts(), h.getLivestockDensity()))
            .collect(Collectors.toList());
    }
    
    /**
     * 计算实体密度评分
     * 考虑实体总数、类型多样性、特殊实体权重等因素
     */
    private double calculateDensityScore(int totalEntities, Map<EntityType, Integer> entityCounts) {
        double score = totalEntities; // 基础分数
        
        // 类型多样性加分
        double diversityBonus = Math.sqrt(entityCounts.size()) * 2;
        score += diversityBonus;
        
        // 特殊实体权重
        for (Map.Entry<EntityType, Integer> entry : entityCounts.entrySet()) {
            EntityType type = entry.getKey();
            int count = entry.getValue();
            
            // 给予不同实体类型不同的权重
            double weight = getEntityWeight(type);
            score += count * weight;
        }
        
        return score;
    }
    
    /**
     * 获取实体类型的权重
     */
    private double getEntityWeight(EntityType type) {
        switch (type) {
            // 高影响实体
            case WITHER:
            case ENDER_DRAGON:
                return 10.0;
            case CREEPER:
            case PRIMED_TNT:
                return 5.0;
            
            // 中等影响实体
            case ZOMBIE:
            case SKELETON:
            case SPIDER:
                return 2.0;
            
            // 动物类
            case COW:
            case PIG:
            case SHEEP:
            case CHICKEN:
                return 1.5;
            
            // 掉落物和经验球
            case DROPPED_ITEM:
                return 0.8;
            case EXPERIENCE_ORB:
                return 0.5;
            
            // 其他实体
            default:
                return 1.0;
        }
    }
    
    /**
     * 获取前N个畜牧业热点
     */
    public List<LivestockHotspotInfo> getTopLivestockHotspots(int limit) {
        List<LivestockHotspotInfo> allHotspots = new ArrayList<>(livestockCache.values());
        allHotspots.sort((a, b) -> Integer.compare(b.getCleanupPriority(), a.getCleanupPriority()));
        
        return allHotspots.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取指定世界的畜牧业热点
     */
    public List<LivestockHotspotInfo> getWorldLivestockHotspots(String worldName) {
        return livestockCache.values().stream()
            .filter(hotspot -> hotspot.getWorldName().equals(worldName))
            .sorted((a, b) -> Integer.compare(b.getCleanupPriority(), a.getCleanupPriority()))
            .collect(Collectors.toList());
    }
    
    /**
     * 获取超标的畜牧业热点
     */
    public List<LivestockHotspotInfo> getExceedingLivestockHotspots() {
        return livestockCache.values().stream()
            .filter(LivestockHotspotInfo::exceedsLimit)
            .sorted((a, b) -> Integer.compare(b.getCleanupPriority(), a.getCleanupPriority()))
            .collect(Collectors.toList());
    }
    
    /**
     * 获取畜牧业统计信息
     */
    public LivestockStatistics getLivestockStatistics() {
        int totalHotspots = livestockCache.size();
        int exceedingHotspots = (int) livestockCache.values().stream()
            .filter(LivestockHotspotInfo::exceedsLimit)
            .count();
        
        int totalAnimals = livestockCache.values().stream()
            .mapToInt(LivestockHotspotInfo::getTotalAnimals)
            .sum();
        
        int totalEntities = livestockCache.values().stream()
            .mapToInt(LivestockHotspotInfo::getTotalEntities)
            .sum();
        
        double avgDensity = livestockCache.values().stream()
            .mapToDouble(LivestockHotspotInfo::getLivestockDensity)
            .average()
            .orElse(0.0);
        
        SparkEntityMetrics sparkMetrics = sparkMetricsCache.get("global");
        
        // 获取违规的热点区块列表
        List<LivestockHotspotInfo> violatingChunks = livestockCache.values().stream()
            .filter(LivestockHotspotInfo::exceedsLimit)
            .collect(Collectors.toList());
        
        return new LivestockStatistics(totalHotspots, exceedingHotspots, totalAnimals, 
            totalEntities, avgDensity, sparkMetrics, violatingChunks);
    }
    
    /**
     * 获取指定世界的前N个热点
     */
    public List<HotspotInfo> getTopHotspots(String worldName, int limit) {
        return hotspotCache.values().stream()
            .filter(hotspot -> hotspot.getWorldName().equals(worldName))
            .sorted((a, b) -> Double.compare(b.getDensity(), a.getDensity()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取所有世界的前N个热点
     */
    public List<HotspotInfo> getTopHotspots(int limit) {
        return hotspotCache.values().stream()
            .sorted((a, b) -> Double.compare(b.getDensity(), a.getDensity()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * 清除缓存
     */
    public void clearCache() {
        livestockCache.clear();
        hotspotCache.clear();
        sparkMetricsCache.clear();
        lastScanTime = 0;
        lastSparkScanTime = 0;
    }
    
    /**
     * 检查是否正在扫描
     */
    public boolean isScanning() {
        return false; // 异步扫描，无需状态跟踪
    }
    
    /**
     * 获取上次扫描时间
     */
    public long getLastScanTime() {
        return lastScanTime;
    }
    
    /**
     * 获取上次Spark扫描时间
     */
    public long getLastSparkScanTime() {
        return lastSparkScanTime;
    }
    
    /**
     * 畜牧业统计信息类
     */
    public static class LivestockStatistics {
        private final int totalHotspots;
        private final int exceedingHotspots;
        private final int totalAnimals;
        private final int totalEntities;
        private final double averageDensity;
        private final SparkEntityMetrics sparkMetrics;
        private final long timestamp;
        private final List<LivestockHotspotInfo> violatingChunks;
        
        public LivestockStatistics(int totalHotspots, int exceedingHotspots, int totalAnimals,
                                  int totalEntities, double averageDensity, SparkEntityMetrics sparkMetrics,
                                  List<LivestockHotspotInfo> violatingChunks) {
            this.totalHotspots = totalHotspots;
            this.exceedingHotspots = exceedingHotspots;
            this.totalAnimals = totalAnimals;
            this.totalEntities = totalEntities;
            this.averageDensity = averageDensity;
            this.sparkMetrics = sparkMetrics;
            this.timestamp = System.currentTimeMillis();
            this.violatingChunks = new ArrayList<>(violatingChunks != null ? violatingChunks : Collections.emptyList());
        }
        
        public int getTotalHotspots() { return totalHotspots; }
        public int getExceedingHotspots() { return exceedingHotspots; }
        public int getTotalAnimals() { return totalAnimals; }
        public int getTotalEntities() { return totalEntities; }
        public double getAverageDensity() { return averageDensity; }
        public SparkEntityMetrics getSparkMetrics() { return sparkMetrics; }
        public long getTimestamp() { return timestamp; }
        
        public double getExceedingRate() {
            return totalHotspots > 0 ? (double) exceedingHotspots / totalHotspots * 100 : 0.0;
        }
        
        public boolean isPerformanceCritical() {
            return sparkMetrics != null && sparkMetrics.isPerformanceCritical();
        }
        
        public List<LivestockHotspotInfo> getViolatingChunks() {
            return new ArrayList<>(violatingChunks);
        }
        
        public double getAveragePerformanceImpact() {
            // 计算平均性能影响
            if (sparkMetrics != null) {
                // 基于TPS和MSPT计算性能影响
                double tpsImpact = Math.max(0, (20.0 - sparkMetrics.getCurrentTps()) / 20.0 * 100);
                double msptImpact = Math.max(0, (sparkMetrics.getCurrentMspt() - 50.0) / 50.0 * 100);
                return (tpsImpact + msptImpact) / 2.0;
            }
            return 0.0;
        }
    }
    
    /**
     * 区块实体数据内部类
     */
    private static class ChunkEntityData {
        final int chunkX;
        final int chunkZ;
        final int totalEntities;
        final Map<EntityType, Integer> entityCounts;
        
        ChunkEntityData(int chunkX, int chunkZ, int totalEntities, Map<EntityType, Integer> entityCounts) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.totalEntities = totalEntities;
            this.entityCounts = entityCounts;
        }
    }
    
    /**
     * 检查是否为畜牧业实体类型
     */
    private boolean isLivestockType(EntityType type) {
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
    
    /**
     * 计算畜牧业密度评分
     */
    private double calculateLivestockDensityScore(int totalAnimals, Map<EntityType, Integer> animalCounts) {
        double score = totalAnimals * 2.0; // 动物基础权重更高
        
        // 动物种类多样性加分
        double diversityBonus = Math.sqrt(animalCounts.size()) * 3;
        score += diversityBonus;
        
        // 特定动物类型权重
        for (Map.Entry<EntityType, Integer> entry : animalCounts.entrySet()) {
            EntityType type = entry.getKey();
            int count = entry.getValue();
            
            double weight = getLivestockWeight(type);
            score += count * weight;
        }
        
        return score;
    }
    
    /**
     * 计算性能影响评分
     */
    private double calculatePerformanceImpact(int totalEntities, Map<EntityType, Integer> entityCounts, SparkEntityMetrics sparkMetrics) {
        double impact = totalEntities;
        
        // Spark性能指标影响
        if (sparkMetrics != null) {
            if (sparkMetrics.getCurrentTps() < 15.0) {
                impact *= 1.5; // TPS低时增加影响权重
            }
            if (sparkMetrics.getCurrentMspt() > 50.0) {
                impact *= 1.3; // MSPT高时增加影响权重
            }
        }
        
        // 高影响实体类型加权
        for (Map.Entry<EntityType, Integer> entry : entityCounts.entrySet()) {
            EntityType type = entry.getKey();
            int count = entry.getValue();
            
            if (isHighImpactEntity(type)) {
                impact += count * 5.0;
            }
        }
        
        return impact;
    }
    
    /**
     * 获取畜牧业实体权重
     */
    private double getLivestockWeight(EntityType type) {
        switch (type) {
            case COW:
            case HORSE:
                return 2.0; // 大型动物权重更高
            case PIG:
            case SHEEP:
            case LLAMA:
                return 1.5;
            case CHICKEN:
            case RABBIT:
            case BEE:
                return 1.0;
            case WOLF:
            case CAT:
                return 0.8; // 宠物权重较低
            default:
                return 1.2;
        }
    }
    
    /**
     * 检查是否为高影响实体
     */
    private boolean isHighImpactEntity(EntityType type) {
        switch (type) {
            case WITHER:
            case ENDER_DRAGON:
            case CREEPER:
            case PRIMED_TNT:
            case FIREWORK:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * 区块畜牧业数据内部类
     */
    private static class ChunkLivestockData {
        final int chunkX;
        final int chunkZ;
        final int totalAnimals;
        final int totalEntities;
        final Map<EntityType, Integer> animalCounts;
        final Map<EntityType, Integer> allEntityCounts;
        
        ChunkLivestockData(int chunkX, int chunkZ, int totalAnimals, int totalEntities,
                          Map<EntityType, Integer> animalCounts, Map<EntityType, Integer> allEntityCounts) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.totalAnimals = totalAnimals;
            this.totalEntities = totalEntities;
            this.animalCounts = animalCounts;
            this.allEntityCounts = allEntityCounts;
        }
    }
    
    /**
     * 畜牧业热点扫描回调接口
     */
    public interface LivestockScanCallback {
        void onComplete(List<LivestockHotspotInfo> hotspots, LivestockStatistics statistics);
        void onError(Exception error);
    }
    
    /**
     * 热点扫描回调接口（兼容性）
     */
    public interface HotspotScanCallback {
        void onComplete(List<HotspotInfo> hotspots);
        void onError(Exception error);
    }
}