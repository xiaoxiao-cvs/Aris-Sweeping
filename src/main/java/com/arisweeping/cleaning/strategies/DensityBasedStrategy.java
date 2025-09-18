package com.arisweeping.cleaning.strategies;

import com.arisweeping.data.ConfigData;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于密度的清理策略
 * 
 * 根据实体在特定区域内的密度来决定是否清理
 */
public class DensityBasedStrategy implements CleaningStrategy {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final ConfigData configData;
    private boolean enabled = true;
    private double checkRadius = 16.0; // 默认检查半径
    private int densityThreshold = 10; // 默认密度阈值
    
    public DensityBasedStrategy(ConfigData configData) {
        this.configData = configData;
        // 从配置中读取密度设置
        this.checkRadius = configData.getAnimalDensityRadius();
        this.densityThreshold = configData.getAnimalDensityThreshold();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> applyStrategy(List<T> candidates) {
        if (!enabled || candidates.isEmpty()) {
            return candidates;
        }
        
        LOGGER.debug("Applying density-based strategy to {} candidates with radius {} and threshold {}", 
                    candidates.size(), checkRadius, densityThreshold);
        
        // 按位置分组，计算密度
        Map<String, List<T>> locationGroups = groupEntitiesByLocation(candidates);
        
        // 找出高密度区域的实体
        List<T> entitiesToRemove = new ArrayList<>();
        
        for (Map.Entry<String, List<T>> entry : locationGroups.entrySet()) {
            List<T> entitiesInArea = entry.getValue();
            
            if (entitiesInArea.size() > densityThreshold) {
                // 高密度区域，选择部分实体进行清理
                int entitiesToKeep = densityThreshold;
                int entitiesToRemoveCount = entitiesInArea.size() - entitiesToKeep;
                
                List<T> selectedForRemoval = selectEntitiesForRemoval(entitiesInArea, entitiesToRemoveCount);
                entitiesToRemove.addAll(selectedForRemoval);
            }
        }
        
        LOGGER.debug("Selected {} entities for removal based on density", entitiesToRemove.size());
        return entitiesToRemove;
    }
    
    /**
     * 按位置对实体分组
     */
    private <T> Map<String, List<T>> groupEntitiesByLocation(List<T> entities) {
        Map<String, List<T>> groups = new HashMap<>();
        
        for (T entity : entities) {
            // TODO: 实现实际的位置获取和分组逻辑
            // 需要：
            // 1. 获取实体的坐标
            // 2. 根据检查半径将坐标离散化为网格
            // 3. 将实体分组到对应的网格中
            
            // 暂时的占位符实现：随机分组
            String locationKey = "location_" + (Math.abs(entity.hashCode()) % 10);
            groups.computeIfAbsent(locationKey, k -> new ArrayList<>()).add(entity);
        }
        
        return groups;
    }
    
    /**
     * 从高密度区域选择要移除的实体
     */
    private <T> List<T> selectEntitiesForRemoval(List<T> entities, int count) {
        if (count <= 0 || entities.isEmpty()) {
            return Collections.emptyList();
        }
        
        // TODO: 实现智能选择逻辑
        // 可以考虑：
        // 1. 优先选择年龄较大的实体
        // 2. 优先选择非繁殖状态的实体
        // 3. 保留稀有或特殊的实体
        
        // 暂时的简单实现：随机选择
        List<T> shuffled = new ArrayList<>(entities);
        Collections.shuffle(shuffled);
        
        return shuffled.stream()
            .limit(Math.min(count, shuffled.size()))
            .collect(Collectors.toList());
    }
    
    /**
     * 计算指定位置的实体密度
     */
    public int calculateDensityAtLocation(List<?> entities, double x, double y, double z) {
        // TODO: 实现基于坐标的密度计算
        // 需要检查在 (x,y,z) 周围 checkRadius 范围内的实体数量
        
        // 暂时返回简单的估算
        return entities.size();
    }
    
    /**
     * 检查某个区域是否为高密度区域
     */
    public boolean isHighDensityArea(List<?> entitiesInArea) {
        return entitiesInArea.size() > densityThreshold;
    }
    
    /**
     * 设置检查半径
     */
    public void setCheckRadius(double radius) {
        if (radius > 0) {
            this.checkRadius = radius;
            LOGGER.info("Density-based strategy check radius set to: {}", radius);
        }
    }
    
    /**
     * 设置密度阈值
     */
    public void setDensityThreshold(int threshold) {
        if (threshold > 0) {
            this.densityThreshold = threshold;
            LOGGER.info("Density-based strategy threshold set to: {}", threshold);
        }
    }
    
    /**
     * 获取检查半径
     */
    public double getCheckRadius() {
        return checkRadius;
    }
    
    /**
     * 获取密度阈值
     */
    public int getDensityThreshold() {
        return densityThreshold;
    }
    
    @Override
    public String getStrategyName() {
        return "density";
    }
    
    @Override
    public String getDescription() {
        return String.format("Removes excess entities when more than %d are within %.1f blocks", 
                           densityThreshold, checkRadius);
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LOGGER.info("DensityBasedStrategy enabled: {}", enabled);
    }
    
    /**
     * 获取策略统计信息
     */
    @SuppressWarnings("unchecked")
    public DensityStrategyStatistics getStatistics(List<?> entities) {
        Map<String, List<Object>> locationGroups = groupEntitiesByLocation((List<Object>) entities);
        
        int totalGroups = locationGroups.size();
        int highDensityGroups = 0;
        int totalEntitiesInHighDensityAreas = 0;
        
        for (List<?> group : locationGroups.values()) {
            if (isHighDensityArea(group)) {
                highDensityGroups++;
                totalEntitiesInHighDensityAreas += group.size();
            }
        }
        
        return new DensityStrategyStatistics(
            totalGroups,
            highDensityGroups,
            totalEntitiesInHighDensityAreas,
            densityThreshold,
            checkRadius
        );
    }
    
    /**
     * 密度策略统计信息
     */
    public static class DensityStrategyStatistics {
        private final int totalGroups;
        private final int highDensityGroups;
        private final int entitiesInHighDensityAreas;
        private final int densityThreshold;
        private final double checkRadius;
        
        public DensityStrategyStatistics(int totalGroups, int highDensityGroups, 
                                       int entitiesInHighDensityAreas, int densityThreshold, double checkRadius) {
            this.totalGroups = totalGroups;
            this.highDensityGroups = highDensityGroups;
            this.entitiesInHighDensityAreas = entitiesInHighDensityAreas;
            this.densityThreshold = densityThreshold;
            this.checkRadius = checkRadius;
        }
        
        public int getTotalGroups() { return totalGroups; }
        public int getHighDensityGroups() { return highDensityGroups; }
        public int getEntitiesInHighDensityAreas() { return entitiesInHighDensityAreas; }
        public int getDensityThreshold() { return densityThreshold; }
        public double getCheckRadius() { return checkRadius; }
        
        @Override
        public String toString() {
            return String.format("DensityStatistics{groups=%d, highDensity=%d, entities=%d, threshold=%d, radius=%.1f}",
                totalGroups, highDensityGroups, entitiesInHighDensityAreas, densityThreshold, checkRadius);
        }
    }
}