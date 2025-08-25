package com.xiaoxiao.arissweeping.util;

import java.util.*;

/**
 * 畜牧业统计信息类
 * 简化的统计数据结构
 */
public class LivestockStatistics {
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
    
    // Getters
    public int getTotalHotspots() { return totalHotspots; }
    public int getExceedingHotspots() { return exceedingHotspots; }
    public int getTotalAnimals() { return totalAnimals; }
    public int getTotalEntities() { return totalEntities; }
    public double getAverageDensity() { return averageDensity; }
    public SparkEntityMetrics getSparkMetrics() { return sparkMetrics; }
    public long getTimestamp() { return timestamp; }
    
    public double getExceedingRate() {
        return totalHotspots > 0 ? (double) exceedingHotspots / totalHotspots * 100.0 : 0.0;
    }

    public boolean isPerformanceCritical() {
        return sparkMetrics != null && sparkMetrics.isPerformanceCritical();
    }

    public List<LivestockHotspotInfo> getViolatingChunks() {
        return new ArrayList<>(violatingChunks);
    }

    public double getAveragePerformanceImpact() {
        if (violatingChunks.isEmpty()) {
            return 0.0;
        }
        
        return violatingChunks.stream()
                .mapToDouble(LivestockHotspotInfo::getPerformanceImpact)
                .average()
                .orElse(0.0);
    }
}