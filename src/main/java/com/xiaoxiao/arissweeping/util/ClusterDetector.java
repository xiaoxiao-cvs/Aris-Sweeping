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
        try {
            if (entity == null || !entity.isValid()) {
                return false;
            }
            
            List<Entity> nearbyEntities = entity.getNearbyEntities(nearbyDistance, nearbyDistance, nearbyDistance);
            
            if (onlyCountSameType) {
                // 只计算相同类型的实体
                EntityType entityType = entity.getType();
                long sameTypeCount = nearbyEntities.stream()
                    .filter(e -> e != null && e.isValid() && e.getType() == entityType)
                    .count();
                return sameTypeCount >= minClusterSize;
            } else {
                // 计算所有类型的实体
                long validCount = nearbyEntities.stream()
                    .filter(e -> e != null && e.isValid())
                    .count();
                return validCount >= minClusterSize;
            }
        } catch (Exception e) {
            // 静默处理异常，返回false以避免影响正常流程
            return false;
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
        try {
            if (entity == null || !entity.isValid() || allowedTypes == null || allowedTypes.isEmpty()) {
                return false;
            }
            
            List<Entity> nearbyEntities = entity.getNearbyEntities(nearbyDistance, nearbyDistance, nearbyDistance);
            
            long validCount = nearbyEntities.stream()
                .filter(e -> e != null && e.isValid() && allowedTypes.contains(e.getType()))
                .count();
                
            return validCount >= minClusterSize;
        } catch (Exception e) {
            // 静默处理异常，返回false以避免影响正常流程
            return false;
        }
    }
    
    /**
     * 获取聚集区域内的所有实体
     * 
     * @param centerEntity 中心实体
     * @param nearbyDistance 检测范围
     * @return 聚集区域内的实体列表
     */
    public static List<Entity> getClusteredEntities(Entity centerEntity, double nearbyDistance) {
        try {
            List<Entity> clusteredEntities = new ArrayList<>();
            
            if (centerEntity == null || !centerEntity.isValid()) {
                return clusteredEntities;
            }
            
            clusteredEntities.add(centerEntity); // 包含中心实体
            List<Entity> nearbyEntities = centerEntity.getNearbyEntities(nearbyDistance, nearbyDistance, nearbyDistance);
            
            // 过滤有效实体
            for (Entity entity : nearbyEntities) {
                if (entity != null && entity.isValid()) {
                    clusteredEntities.add(entity);
                }
            }
            
            return clusteredEntities;
        } catch (Exception e) {
            // 返回空列表以避免影响正常流程
            return new ArrayList<>();
        }
    }
    
    /**
     * 按类型分组聚集实体
     * 
     * @param centerEntity 中心实体
     * @param nearbyDistance 检测范围
     * @return 按类型分组的实体映射
     */
    public static Map<EntityType, List<Entity>> getClusteredEntitiesByType(Entity centerEntity, double nearbyDistance) {
        try {
            Map<EntityType, List<Entity>> entityMap = new HashMap<>();
            
            if (centerEntity == null || !centerEntity.isValid()) {
                return entityMap;
            }
            
            List<Entity> allEntities = getClusteredEntities(centerEntity, nearbyDistance);
            
            for (Entity entity : allEntities) {
                try {
                    if (entity != null && entity.isValid()) {
                        entityMap.computeIfAbsent(entity.getType(), k -> new ArrayList<>()).add(entity);
                    }
                } catch (Exception e) {
                    // 跳过有问题的实体，继续处理其他实体
                    continue;
                }
            }
            
            return entityMap;
        } catch (Exception e) {
            // 返回空映射以避免影响正常流程
            return new HashMap<>();
        }
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
        try {
            if (centerLocation == null || entities == null || radius <= 0) {
                return 0.0;
            }
            
            long entitiesInRadius = entities.stream()
                .filter(entity -> {
                    try {
                        return entity != null && entity.isValid() && 
                               entity.getLocation() != null && 
                               entity.getLocation().distance(centerLocation) <= radius;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();
                
            double area = Math.PI * radius * radius; // 圆形区域面积
            return area > 0 ? entitiesInRadius / area : 0.0;
        } catch (Exception e) {
            // 返回0密度以避免影响正常流程
            return 0.0;
        }
    }
    
    /**
     * 检测区块内的实体聚集情况
     * 
     * @param entities 区块内的实体列表
     * @param maxDensity 最大允许密度
     * @return 是否存在过度聚集
     */
    public static boolean isChunkOvercrowded(List<Entity> entities, double maxDensity) {
        try {
            if (entities == null || entities.isEmpty() || maxDensity <= 0) {
                return false;
            }
            
            // 过滤有效实体
            long validEntityCount = entities.stream()
                .filter(entity -> entity != null && entity.isValid())
                .count();
            
            if (validEntityCount == 0) {
                return false;
            }
            
            // 区块大小为16x16，面积为256
            double chunkArea = 256.0;
            double currentDensity = validEntityCount / chunkArea;
            
            return currentDensity > maxDensity;
        } catch (Exception e) {
            // 返回false以避免影响正常流程
            return false;
        }
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
        try {
            List<Entity> candidates = new ArrayList<>();
            
            if (entities == null || entities.isEmpty() || nearbyDistance <= 0 || minClusterSize <= 0 || preserveRatio < 0 || preserveRatio > 1) {
                return candidates;
            }
            
            Map<EntityType, List<Entity>> typeGroups = new HashMap<>();
            
            // 按类型分组，过滤有效实体
            for (Entity entity : entities) {
                try {
                    if (entity != null && entity.isValid()) {
                        typeGroups.computeIfAbsent(entity.getType(), k -> new ArrayList<>()).add(entity);
                    }
                } catch (Exception e) {
                    // 跳过有问题的实体
                    continue;
                }
            }
            
            // 对每种类型的实体进行聚集检测
            for (Map.Entry<EntityType, List<Entity>> entry : typeGroups.entrySet()) {
                try {
                    List<Entity> typeEntities = entry.getValue();
                    
                    for (Entity entity : typeEntities) {
                        try {
                            if (entity == null || !entity.isValid()) {
                                continue;
                            }
                            
                            if (isEntityClustered(entity, nearbyDistance, minClusterSize, true)) {
                                // 获取聚集区域内的同类型实体
                                List<Entity> clusteredSameType = entity.getNearbyEntities(nearbyDistance, nearbyDistance, nearbyDistance)
                                    .stream()
                                    .filter(e -> e != null && e.isValid() && e.getType() == entity.getType())
                                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                                
                                clusteredSameType.add(entity); // 包含中心实体
                                
                                // 计算需要保留的数量
                                int preserveCount = Math.max(1, (int) (clusteredSameType.size() * preserveRatio));
                                int removeCount = clusteredSameType.size() - preserveCount;
                                
                                if (removeCount > 0) {
                                    // 添加到清理候选列表（移除最老的实体）
                                    try {
                                        clusteredSameType.sort((e1, e2) -> {
                                            try {
                                                return Integer.compare(e2.getTicksLived(), e1.getTicksLived());
                                            } catch (Exception e) {
                                                return 0;
                                            }
                                        });
                                        
                                        for (int i = 0; i < removeCount && i < clusteredSameType.size(); i++) {
                                            Entity candidate = clusteredSameType.get(i);
                                            if (candidate != null && candidate.isValid() && !candidates.contains(candidate)) {
                                                candidates.add(candidate);
                                            }
                                        }
                                    } catch (Exception e) {
                                        // 排序或添加失败，跳过这个聚集
                                        continue;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // 跳过有问题的实体
                            continue;
                        }
                    }
                } catch (Exception e) {
                    // 跳过有问题的类型组
                    continue;
                }
            }
            
            return candidates;
        } catch (Exception e) {
            // 返回空列表以避免影响正常流程
            return new ArrayList<>();
        }
    }
    
    /**
     * 检测实体是否应该被保护（不被清理）
     * 
     * @param entity 实体
     * @return 是否应该被保护
     */
    public static boolean shouldProtectEntity(Entity entity) {
        try {
            if (entity == null || !entity.isValid()) {
                return false;
            }
            
            // 保护有自定义名称的实体
            try {
                if (entity.getCustomName() != null) {
                    return true;
                }
            } catch (Exception e) {
                // 获取自定义名称失败，继续其他检查
            }
            
            // 保护被玩家骑乘的实体
            try {
                if (!entity.getPassengers().isEmpty()) {
                    return entity.getPassengers().stream()
                        .anyMatch(passenger -> {
                            try {
                                return passenger instanceof org.bukkit.entity.Player;
                            } catch (Exception e) {
                                return false;
                            }
                        });
                }
            } catch (Exception e) {
                // 获取乘客信息失败，继续其他检查
            }
            
            // 保护拴绳实体
            try {
                if (entity instanceof org.bukkit.entity.LivingEntity livingEntity) {
                    return livingEntity.isLeashed();
                }
            } catch (Exception e) {
                // 检查拴绳状态失败，继续其他检查
            }
            
            return false;
        } catch (Exception e) {
            // 默认保护有问题的实体
            return true;
        }
    }
}