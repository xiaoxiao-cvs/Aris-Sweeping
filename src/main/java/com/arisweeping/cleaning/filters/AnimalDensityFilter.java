package com.arisweeping.cleaning.filters;

import com.arisweeping.data.ConfigData;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 动物密度过滤器
 * 
 * 根据动物密度配置过滤需要清理的动物实体
 */
public class AnimalDensityFilter {
    private final ConfigData configData;
    
    public AnimalDensityFilter(ConfigData configData) {
        this.configData = configData;
    }
    
    /**
     * 过滤动物实体列表
     * 
     * @param candidates 候选动物实体列表
     * @return 过滤后需要清理的动物实体列表
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> filter(List<T> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return candidates;
        }
        
        // 根据密度阈值进行过滤
        return filterByDensity(candidates);
    }
    
    /**
     * 基于密度阈值进行过滤
     */
    private <T> List<T> filterByDensity(List<T> candidates) {
        if (candidates.size() <= configData.getAnimalDensityThreshold()) {
            // 如果动物数量没有超过阈值，不清理任何动物
            return Collections.emptyList();
        }
        
        // 计算需要清理的动物数量
        int animalsToRemove = candidates.size() - configData.getAnimalDensityThreshold();
        
        // 根据优先级选择要清理的动物
        return selectAnimalsToRemove(candidates, animalsToRemove);
    }
    
    /**
     * 选择要移除的动物
     */
    private <T> List<T> selectAnimalsToRemove(List<T> candidates, int count) {
        // TODO: 实现具体的动物选择逻辑
        // 优先级规则：
        // 1. 非繁殖中的成年动物优先
        // 2. 年龄较大的动物优先
        // 3. 随机选择（如果其他条件相同）
        
        // 暂时的简单实现 - 随机选择
        List<T> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled);
        return shuffled.stream()
            .limit(Math.min(count, shuffled.size()))
            .collect(Collectors.toList());
    }
    
    /**
     * 检查动物是否应该被保护（不清理）
     */
    private <T> boolean shouldProtectAnimal(T animal) {
        // TODO: 实现具体的动物保护逻辑
        // 检查条件：
        // 1. 是否正在繁殖
        // 2. 是否是幼体
        // 3. 是否有特殊标记
        // 4. 是否是稀有品种
        
        if (configData.isProtectBreedingAnimals()) {
            // 暂时返回 false，实际需要检查动物的繁殖状态
            return false;
        }
        
        return false;
    }
    
    /**
     * 计算指定区域内的动物密度
     */
    public int calculateDensity(List<?> animals, double x, double y, double z) {
        double radius = configData.getAnimalDensityRadius();
        
        // TODO: 实现基于坐标的密度计算
        // 需要实际的动物位置信息
        
        return animals.size(); // 暂时返回总数
    }
    
    /**
     * 检查动物类型是否应该被清理
     */
    public boolean isAnimalTypeCleanable(String animalType) {
        // TODO: 实现基于配置的动物类型检查
        // 可能需要支持动物类型白名单/黑名单
        return true;
    }
    
    /**
     * 获取过滤器状态
     */
    public FilterStatus getStatus() {
        return new FilterStatus(
            "AnimalDensityFilter", 
            configData.isAnimalCleaningEnabled(),
            String.format("Removes excess animals when density exceeds %d within %g blocks", 
                configData.getAnimalDensityThreshold(), 
                configData.getAnimalDensityRadius())
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