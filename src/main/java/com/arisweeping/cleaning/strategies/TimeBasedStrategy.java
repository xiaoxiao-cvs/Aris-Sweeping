package com.arisweeping.cleaning.strategies;

import com.arisweeping.data.ConfigData;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于时间的清理策略
 * 
 * 根据实体存在的时间来决定是否清理
 */
public class TimeBasedStrategy implements CleaningStrategy {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final ConfigData configData;
    private boolean enabled = true;
    
    public TimeBasedStrategy(ConfigData configData) {
        this.configData = configData;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> applyStrategy(List<T> candidates) {
        if (!enabled || candidates.isEmpty()) {
            return candidates;
        }
        
        LOGGER.debug("Applying time-based strategy to {} candidates", candidates.size());
        
        // 这里需要实际的实体时间检查逻辑
        // 为了编译通过，暂时返回所有候选者
        // TODO: 实现基于时间的过滤逻辑
        
        return candidates.stream()
            .limit(Math.max(1, candidates.size() / 2)) // 临时逻辑：保留一半
            .collect(Collectors.toList());
    }
    
    @Override
    public String getStrategyName() {
        return "time";
    }
    
    @Override
    public String getDescription() {
        return "Removes entities based on how long they have existed";
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LOGGER.info("TimeBasedStrategy enabled: {}", enabled);
    }
}