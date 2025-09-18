package com.arisweeping.cleaning.strategies;
import com.arisweeping.core.ArisLogger;

import com.arisweeping.data.ConfigData;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于距离的清理策略
 * 
 * 根据实体与玩家或指定点的距离来决定是否清理
 */
public class DistanceBasedStrategy implements CleaningStrategy {
    
    private final ConfigData configData;
    private boolean enabled = true;
    private double maxDistance = 100.0; // 默认最大距离
    
    public DistanceBasedStrategy(ConfigData configData) {
        this.configData = configData;
        // 可以从配置中读取最大距离设置
        // this.maxDistance = configData.getMaxCleaningDistance();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> applyStrategy(List<T> candidates) {
        if (!enabled || candidates.isEmpty()) {
            return candidates;
        }
        
        ArisLogger.debug("Applying distance-based strategy to {} candidates", candidates.size());
        
        // TODO: 实现基于距离的过滤逻辑
        // 需要：
        // 1. 获取玩家位置或参考点
        // 2. 计算每个实体与参考点的距离
        // 3. 过滤出超过最大距离的实体
        
        // 暂时的简单实现：根据距离阈值过滤
        return candidates.stream()
            .filter(this::isEntityFarEnough)
            .collect(Collectors.toList());
    }
    
    /**
     * 检查实体是否足够远（可以清理）
     */
    private <T> boolean isEntityFarEnough(T entity) {
        // TODO: 实现实际的距离计算
        // 需要获取实体位置和参考点位置
        
        // 暂时的占位符逻辑
        return Math.random() > 0.3; // 70%的实体被认为足够远
    }
    
    /**
     * 设置最大距离阈值
     */
    public void setMaxDistance(double maxDistance) {
        if (maxDistance > 0) {
            this.maxDistance = maxDistance;
            ArisLogger.info("Distance-based strategy max distance set to: {}", maxDistance);
        }
    }
    
    /**
     * 获取最大距离阈值
     */
    public double getMaxDistance() {
        return maxDistance;
    }
    
    @Override
    public String getStrategyName() {
        return "distance";
    }
    
    @Override
    public String getDescription() {
        return String.format("Removes entities that are more than %.1f blocks away from players", maxDistance);
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        ArisLogger.info("DistanceBasedStrategy enabled: {}", enabled);
    }
}