package com.xiaoxiao.arissweeping.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 实体聚集检测器，用于检测和处理实体聚集问题
 */
public class ClusterDetector {
    
    /**
     * 检测实体是否处于聚集状态
     * 
     * @param entity 要检测的实体
     * @param nearbyDistance 检测范围
     * @param minClusterSize 最小聚集数量
     * @param onlyCountSameType 是否只计算相同类型的实体
     * @return 是否处于聚集状态
     */
    public static boolean isEntityClustered(Entity entity, double nearbyDistance, int minClusterSize, boolean onlyCountSameType) {
        List<Entity> nearbyEntities = entity.getNearbyEntities(nearbyDistance, nearbyDistance, nearbyDistance);
        
        if (onlyCountSameType) {
            // 只计算相同类型的实体
            EntityType entityType = entity.getType();
            long sameTypeCount = nearbyEntities.stream()
                .filter(e -> e.getType() == entityType)
                .count();
            return sameTypeCount >= minClusterSize;
        } else {
            // 计算所有类型的实体
            return nearbyEntities.size() >= minClusterSize;
        }
    }
    
    /**
     * 检测实体是否处于聚集状态（带类型过滤）
     * 
     * @param entity 要检测的实体
     * @param nearbyDistance 检测范围
     * @param minClusterSize 最小聚集数量
     * @param allowedTypes 允许计算的实体类型列表
     * @return 是否处于聚集状态
     */
    public static boolean isEntityClustered(Entity entity, double nearbyDistance, int minClusterSize, List<EntityType> allowedTypes) {
        List<Entity> nearbyEntities = entity.getNearbyEntities(nearbyDistance, nearbyDistance, nearbyDistance);
        
        long validCount = nearbyEntities.stream()
            .filter(e -> allowedTypes.contains(e.getType()))
            .count();
            
        return validCount >= minClusterSize;
    }
    
    /**
     * 获取聚集区域内的所有实体
     * 
     * @param centerEntity 中心实体
     * @param nearbyDistance 检测范围
     * @return 聚集区域内的实体列表
     */
    public static List<Entity> getClusteredEntities(Entity centerEntity, double nearbyDistance) {
        List<Entity> clusteredEntities = new ArrayList<>();
        clusteredEntities.add(centerEntity); // 包含中心实体
        clusteredEntities.addAll(centerEntity.getNearbyEntities(nearbyDistance, nearbyDistance, nearbyDistance));
        return clusteredEntities;
    }
    
    /**
     * 按类型分组聚集实体
     * 
     * @param centerEntity 中心实体
     * @param nearbyDistance 检测范围
     * @return 按类型分组的实体映射
     */
    public static Map<EntityType, List<Entity>> getClusteredEntitiesByType(Entity centerEntity, double nearbyDistance) {
        Map<EntityType, List<Entity>> entityMap = new HashMap<>();
        List<Entity> allEntities = getClusteredEntities(centerEntity, nearbyDistance);
        
        for (Entity entity : allEntities) {
            entityMap.computeIfAbsent(entity.getType(), k -> new ArrayList<>()).add(entity);
        }
        
        return entityMap;
    }
    
    /**
     * 计算实体密度
     * 
     * @param centerLocation 中心位置
     * @param entities 实体列表
     * @param radius 半径
     * @return 实体密度（实体数量/区域面积）
     */
    public static double calculateEntityDensity(Location centerLocation, List<Entity> entities, double radius) {
        long entitiesInRadius = entities.stream()
            .filter(entity -> entity.getLocation().distance(centerLocation) <= radius)
            .count();
            
        double area = Math.PI * radius * radius; // 圆形区域面积
        return entitiesInRadius / area;
    }
    
    /**
     * 检测区块内的实体聚集情况
     * 
     * @param entities 区块内的实体列表
     * @param maxDensity 最大允许密度
     * @return 是否存在过度聚集
     */
    public static boolean isChunkOvercrowded(List<Entity> entities, double maxDensity) {
        if (entities.isEmpty()) {
            return false;
        }
        
        // 区块大小为16x16，面积为256
        double chunkArea = 256.0;
        double currentDensity = entities.size() / chunkArea;
        
        return currentDensity > maxDensity;
    }
    
    /**
     * 获取聚集清理的候选实体
     * 
     * @param entities 所有实体列表
     * @param nearbyDistance 聚集检测距离
     * @param minClusterSize 最小聚集数量
     * @param preserveRatio 保留比例（0.0-1.0）
     * @return 应该被清理的实体列表
     */
    public static List<Entity> getClusterCleanupCandidates(List<Entity> entities, double nearbyDistance, 
                                                           int minClusterSize, double preserveRatio) {
        List<Entity> candidates = new ArrayList<>();
        Map<EntityType, List<Entity>> typeGroups = new HashMap<>();
        
        // 按类型分组
        for (Entity entity : entities) {
            typeGroups.computeIfAbsent(entity.getType(), k -> new ArrayList<>()).add(entity);
        }
        
        // 对每种类型的实体进行聚集检测
        for (Map.Entry<EntityType, List<Entity>> entry : typeGroups.entrySet()) {
            List<Entity> typeEntities = entry.getValue();
            
            for (Entity entity : typeEntities) {
                if (isEntityClustered(entity, nearbyDistance, minClusterSize, true)) {
                    // 获取聚集区域内的同类型实体
                    List<Entity> clusteredSameType = entity.getNearbyEntities(nearbyDistance, nearbyDistance, nearbyDistance)
                        .stream()
                        .filter(e -> e.getType() == entity.getType())
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                    
                    clusteredSameType.add(entity); // 包含中心实体
                    
                    // 计算需要保留的数量
                    int preserveCount = Math.max(1, (int) (clusteredSameType.size() * preserveRatio));
                    int removeCount = clusteredSameType.size() - preserveCount;
                    
                    // 添加到清理候选列表（移除最老的实体）
                    clusteredSameType.sort((e1, e2) -> Integer.compare(e2.getTicksLived(), e1.getTicksLived()));
                    for (int i = 0; i < removeCount && i < clusteredSameType.size(); i++) {
                        Entity candidate = clusteredSameType.get(i);
                        if (!candidates.contains(candidate)) {
                            candidates.add(candidate);
                        }
                    }
                }
            }
        }
        
        return candidates;
    }
    
    /**
     * 检测实体是否应该被保护（不被清理）
     * 
     * @param entity 实体
     * @return 是否应该被保护
     */
    public static boolean shouldProtectEntity(Entity entity) {
        // 保护有自定义名称的实体
        if (entity.getCustomName() != null) {
            return true;
        }
        
        // 保护被玩家骑乘的实体
        if (!entity.getPassengers().isEmpty()) {
            return entity.getPassengers().stream()
                .anyMatch(passenger -> passenger instanceof org.bukkit.entity.Player);
        }
        
        // 保护拴绳实体
        if (entity instanceof org.bukkit.entity.LivingEntity livingEntity) {
            return livingEntity.isLeashed();
        }
        
        return false;
    }
}