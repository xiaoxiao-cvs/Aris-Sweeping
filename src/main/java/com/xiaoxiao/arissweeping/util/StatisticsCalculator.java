package com.xiaoxiao.arissweeping.util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计计算器 - 负责热点数据的统计分析
 * 遵循单一职责原则，只负责统计计算
 */
public class StatisticsCalculator {
    
    /**
     * 计算畜牧业统计信息
     */
    public LivestockStatistics calculateLivestockStatistics(List<LivestockHotspotInfo> hotspots, SparkEntityMetrics sparkMetrics) {
        if (hotspots == null || hotspots.isEmpty()) {
            return new LivestockStatistics(0, 0, 0, 0, 0.0, sparkMetrics, Collections.emptyList());
        }
        
        int totalHotspots = hotspots.size();
        int exceedingHotspots = 0;
        int totalAnimals = 0;
        int totalEntities = 0;
        double totalDensity = 0.0;
        List<LivestockHotspotInfo> violatingChunks = new ArrayList<>();
        
        for (LivestockHotspotInfo hotspot : hotspots) {
            totalAnimals += hotspot.getTotalAnimals();
            totalEntities += hotspot.getTotalEntities();
            totalDensity += hotspot.getLivestockDensity();
            
            if (hotspot.isExceedsLimit()) {
                exceedingHotspots++;
                violatingChunks.add(hotspot);
            }
        }
        
        double averageDensity = totalHotspots > 0 ? totalDensity / totalHotspots : 0.0;
        
        return new LivestockStatistics(totalHotspots, exceedingHotspots, totalAnimals, 
                                     totalEntities, averageDensity, sparkMetrics, violatingChunks);
    }
    
    /**
     * 获取排名前N的热点
     */
    public List<LivestockHotspotInfo> getTopHotspots(List<LivestockHotspotInfo> hotspots, int limit) {
        if (hotspots == null || hotspots.isEmpty()) {
            return Collections.emptyList();
        }
        
        return hotspots.stream()
                .sorted((a, b) -> Double.compare(b.getLivestockDensity(), a.getLivestockDensity()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取特定世界的热点
     */
    public List<LivestockHotspotInfo> getWorldHotspots(List<LivestockHotspotInfo> hotspots, String worldName) {
        if (hotspots == null || hotspots.isEmpty()) {
            return Collections.emptyList();
        }
        
        return hotspots.stream()
                .filter(hotspot -> hotspot.getWorldName().equals(worldName))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取超过限制的热点
     */
    public List<LivestockHotspotInfo> getExceedingHotspots(List<LivestockHotspotInfo> hotspots) {
        if (hotspots == null || hotspots.isEmpty()) {
            return Collections.emptyList();
        }
        
        return hotspots.stream()
                .filter(LivestockHotspotInfo::isExceedsLimit)
                .collect(Collectors.toList());
    }
    
    /**
     * 计算世界级别的统计信息
     */
    public Map<String, WorldStatistics> calculateWorldStatistics(List<LivestockHotspotInfo> hotspots) {
        if (hotspots == null || hotspots.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<String, List<LivestockHotspotInfo>> worldGroups = hotspots.stream()
                .collect(Collectors.groupingBy(LivestockHotspotInfo::getWorldName));
        
        Map<String, WorldStatistics> worldStats = new HashMap<>();
        
        for (Map.Entry<String, List<LivestockHotspotInfo>> entry : worldGroups.entrySet()) {
            String worldName = entry.getKey();
            List<LivestockHotspotInfo> worldHotspots = entry.getValue();
            
            int totalAnimals = worldHotspots.stream().mapToInt(LivestockHotspotInfo::getTotalAnimals).sum();
            int totalEntities = worldHotspots.stream().mapToInt(LivestockHotspotInfo::getTotalEntities).sum();
            double averageDensity = worldHotspots.stream().mapToDouble(LivestockHotspotInfo::getLivestockDensity).average().orElse(0.0);
            int exceedingCount = (int) worldHotspots.stream().filter(LivestockHotspotInfo::isExceedsLimit).count();
            
            worldStats.put(worldName, new WorldStatistics(worldName, worldHotspots.size(), 
                    totalAnimals, totalEntities, averageDensity, exceedingCount));
        }
        
        return worldStats;
    }
    
    /**
     * 世界统计信息
     */
    public static class WorldStatistics {
        private final String worldName;
        private final int hotspotCount;
        private final int totalAnimals;
        private final int totalEntities;
        private final double averageDensity;
        private final int exceedingCount;
        
        public WorldStatistics(String worldName, int hotspotCount, int totalAnimals, 
                             int totalEntities, double averageDensity, int exceedingCount) {
            this.worldName = worldName;
            this.hotspotCount = hotspotCount;
            this.totalAnimals = totalAnimals;
            this.totalEntities = totalEntities;
            this.averageDensity = averageDensity;
            this.exceedingCount = exceedingCount;
        }
        
        public String getWorldName() { return worldName; }
        public int getHotspotCount() { return hotspotCount; }
        public int getTotalAnimals() { return totalAnimals; }
        public int getTotalEntities() { return totalEntities; }
        public double getAverageDensity() { return averageDensity; }
        public int getExceedingCount() { return exceedingCount; }
        
        public double getExceedingRate() {
            return hotspotCount > 0 ? (double) exceedingCount / hotspotCount * 100.0 : 0.0;
        }
    }
}