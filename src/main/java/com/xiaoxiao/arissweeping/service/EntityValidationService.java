package com.xiaoxiao.arissweeping.service;

import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.ClusterDetector;
import com.xiaoxiao.arissweeping.util.EntityTypeUtils;
import com.xiaoxiao.arissweeping.util.LoggerUtil;
import org.bukkit.entity.*;

/**
 * 实体验证服务 - 负责判断实体是否应该被清理
 * 从EntityCleanupHandler中分离出来，遵循单一职责原则
 */
public class EntityValidationService {
    private final ModConfig config;
    
    public EntityValidationService(ModConfig config) {
        this.config = config;
    }
    
    /**
     * 判断实体是否应该被清理
     * @param entity 要检查的实体
     * @return true如果应该清理，false否则
     */
    public boolean shouldCleanupEntity(Entity entity) {
        try {
            if (entity == null || !entity.isValid()) {
                return false;
            }
            
            // 永远不清理玩家
            if (entity instanceof Player) {
                return false;
            }
            
            // 掉落物清理 - 参考EntityClearer，跳过所有检查直接清理
            if (entity instanceof Item item) {
                return shouldCleanupItem(item);
            }
            
            // 智能保护检查
            if (shouldProtectEntity(entity)) {
                return false;
            }
            
            // 使用统一的保护实体判断
            if (EntityTypeUtils.isProtectedEntity(entity)) {
                return false;
            }
            
            // 聚集清理检查
            if (!passesClusterCheck(entity)) {
                return false;
            }
            
            // 按实体类型进行清理判断
            return shouldCleanupByType(entity);
            
        } catch (Exception e) {
            LoggerUtil.severe("检查实体清理条件时发生严重错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 检查掉落物是否应该被清理
     */
    private boolean shouldCleanupItem(Item item) {
        try {
            if (!config.isCleanupItems()) {
                return false;
            }
            
            // 保护有自定义名称的物品（如玩家重命名的装备）
            if (item.getCustomName() != null) {
                return false;
            }
            
            // 直接清理掉落物，不进行聚集检测和智能保护检查
            return true;
        } catch (Exception e) {
            LoggerUtil.warning("检查掉落物清理条件时发生错误: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 智能保护检查
     */
    private boolean shouldProtectEntity(Entity entity) {
        try {
            return ClusterDetector.shouldProtectEntity(entity);
        } catch (Exception e) {
            LoggerUtil.warning("智能保护检查时发生错误: " + e.getMessage());
            return true; // 出错时保护实体
        }
    }
    
    /**
     * 聚集清理检查
     */
    private boolean passesClusterCheck(Entity entity) {
        try {
            if (config.isClusterCleanupEnabled()) {
                boolean isClustered = ClusterDetector.isEntityClustered(
                    entity, 
                    config.getClusterDetectionDistance(), 
                    config.getMinClusterSize(), 
                    config.isOnlyCountSameType()
                );
                
                // 如果不在聚集中，且启用了聚集清理，则不清理
                return isClustered;
            }
            return true; // 未启用聚集清理时通过检查
        } catch (Exception e) {
            LoggerUtil.warning("聚集清理检查时发生错误: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 按实体类型判断是否清理
     */
    private boolean shouldCleanupByType(Entity entity) {
        // 经验球清理
        if (EntityTypeUtils.isCleanableExperienceOrb(entity)) {
            return config.isCleanupExperienceOrbs();
        }
        
        // 箭矢清理（只清理普通箭矢，不清理三叉戟等特殊投射物）
        if (EntityTypeUtils.isCleanableArrow(entity)) {
            return config.isCleanupArrows();
        }
        
        // 掉落物（掉落方块）清理
        if (EntityTypeUtils.isCleanableFallingBlock(entity)) {
            return config.isCleanupFallingBlocks();
        }
        
        // 敌对生物清理
        if (entity instanceof Monster) {
            return shouldCleanupMonster((Monster) entity);
        }
        
        // 被动生物不清理（按用户要求）
        if (entity instanceof Animals) {
            return false;
        }
        
        return false;
    }
    
    /**
     * 检查敌对生物是否应该被清理
     */
    private boolean shouldCleanupMonster(Monster monster) {
        try {
            // 保护有名字的怪物
            if (monster.getCustomName() != null) {
                return false;
            }
            
            // 只有在配置开启敌对生物清理时才清理
            if (config.isCleanupHostileMobs()) {
                // 50%概率清理敌对生物，避免过度清理
                return Math.random() < 0.5;
            }
            return false;
        } catch (Exception e) {
            LoggerUtil.warning("检查敌对生物清理条件时发生错误: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查实体是否为有效的清理目标（基础验证）
     */
    public boolean isValidCleanupTarget(Entity entity) {
        return entity != null && entity.isValid() && !(entity instanceof Player);
    }
}