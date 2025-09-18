package com.arisweeping.cleaning.filters;

import com.arisweeping.data.ConfigData;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 物品实体过滤器
 * 
 * 根据配置和规则过滤需要清理的物品实体
 */
public class ItemEntityFilter {
    private final ConfigData configData;
    
    public ItemEntityFilter(ConfigData configData) {
        this.configData = configData;
    }
    
    /**
     * 过滤物品实体列表
     * 
     * @param candidates 候选物品实体列表
     * @return 过滤后需要清理的物品实体列表
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> filter(List<T> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return candidates;
        }
        
        return candidates.stream()
            .filter(this::shouldKeepItem)
            .collect(Collectors.toList());
    }
    
    /**
     * 检查物品是否应该保留（不清理）
     */
    private <T> boolean shouldKeepItem(T item) {
        // TODO: 实现具体的物品过滤逻辑
        // 这里需要检查：
        // 1. 物品是否在白名单中
        // 2. 物品是否在黑名单中
        // 3. 物品的存活时间
        // 4. 物品的价值（如果有配置）
        // 5. 物品的稀有度
        
        // 暂时的简单逻辑 - 保留一半物品用于测试
        return Math.random() > 0.5;
    }
    
    /**
     * 检查物品类型是否应该被清理
     */
    public boolean isItemTypeCleanable(String itemType) {
        // TODO: 实现基于配置的物品类型检查
        return true;
    }
    
    /**
     * 获取过滤器状态
     */
    public FilterStatus getStatus() {
        return new FilterStatus(
            "ItemEntityFilter", 
            configData.isItemCleaningEnabled(),
            "Filters items based on type, age, and configured rules"
        );
    }
    
    /**
     * 过滤器状态信息
     */
    public static class FilterStatus {
        private final String name;
        private final boolean enabled;
        private final String description;
        
        public FilterStatus(String name, boolean enabled, String description) {
            this.name = name;
            this.enabled = enabled;
            this.description = description;
        }
        
        public String getName() { return name; }
        public boolean isEnabled() { return enabled; }
        public String getDescription() { return description; }
        
        @Override
        public String toString() {
            return String.format("FilterStatus{name='%s', enabled=%s, description='%s'}", 
                name, enabled, description);
        }
    }
}