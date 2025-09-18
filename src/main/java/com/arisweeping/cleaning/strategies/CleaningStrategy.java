package com.arisweeping.cleaning.strategies;

import java.util.List;

/**
 * 清理策略接口
 * 
 * 定义实体清理的具体策略，如基于时间、距离或密度的清理
 */
public interface CleaningStrategy {
    
    /**
     * 应用清理策略
     * 
     * @param candidates 候选实体列表
     * @return 过滤后应被清理的实体列表
     */
    <T> List<T> applyStrategy(List<T> candidates);
    
    /**
     * 获取策略名称
     */
    String getStrategyName();
    
    /**
     * 获取策略描述
     */
    String getDescription();
    
    /**
     * 策略是否启用
     */
    boolean isEnabled();
    
    /**
     * 设置策略启用状态
     */
    void setEnabled(boolean enabled);
}