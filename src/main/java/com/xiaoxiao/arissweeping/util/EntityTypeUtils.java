package com.xiaoxiao.arissweeping.util;

import org.bukkit.entity.*;

/**
 * 实体类型判断工具类
 * 统一管理所有实体类型相关的判断逻辑
 */
public class EntityTypeUtils {
    
    /**
     * 判断实体是否为特殊保护实体（不应被清理）
     * @param entity 实体
     * @return 是否为特殊保护实体
     */
    public static boolean isProtectedEntity(Entity entity) {
        if (entity == null || !entity.isValid()) {
            return true; // 无效实体视为受保护
        }
        
        // 有自定义名称的实体
        if (entity.getCustomName() != null && !entity.getCustomName().isEmpty()) {
            return true;
        }
        
        // 特殊功能实体
        if (entity instanceof Minecart ||
            entity instanceof Boat ||
            entity instanceof ArmorStand ||
            entity instanceof ItemFrame ||
            entity instanceof Painting ||
            entity instanceof LeashHitch) {
            return true;
        }
        
        // 被玩家骑乘的实体
        if (!entity.getPassengers().isEmpty()) {
            return entity.getPassengers().stream()
                .anyMatch(passenger -> passenger instanceof Player);
        }
        
        // 拴绳实体
        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity.isLeashed();
        }
        
        return false;
    }
    
    /**
     * 判断实体是否为可清理的物品类实体
     * @param entity 实体
     * @return 是否为可清理的物品类实体
     */
    public static boolean isCleanableItem(Entity entity) {
        return entity instanceof Item;
    }
    
    /**
     * 判断实体是否为可清理的经验球
     * @param entity 实体
     * @return 是否为可清理的经验球
     */
    public static boolean isCleanableExperienceOrb(Entity entity) {
        return entity instanceof ExperienceOrb;
    }
    
    /**
     * 判断实体是否为可清理的箭矢
     * @param entity 实体
     * @return 是否为可清理的箭矢
     */
    public static boolean isCleanableArrow(Entity entity) {
        return entity instanceof Arrow;
    }
    
    /**
     * 判断实体是否为可清理的掉落方块
     * @param entity 实体
     * @return 是否为可清理的掉落方块
     */
    public static boolean isCleanableFallingBlock(Entity entity) {
        return entity instanceof FallingBlock;
    }
    
    /**
     * 判断实体是否为生物（怪物或动物）
     * @param entity 实体
     * @return 是否为生物
     */
    public static boolean isMob(Entity entity) {
        return entity instanceof Monster || entity instanceof Animals;
    }
    
    /**
     * 判断实体是否为动物
     * @param entity 实体
     * @return 是否为动物
     */
    public static boolean isAnimal(Entity entity) {
        return entity instanceof Animals;
    }
    
    /**
     * 判断实体是否为怪物
     * @param entity 实体
     * @return 是否为怪物
     */
    public static boolean isMonster(Entity entity) {
        return entity instanceof Monster;
    }
    
    /**
     * 获取实体的清理类型分类
     * @param entity 实体
     * @return 清理类型
     */
    public static CleanupEntityType getCleanupType(Entity entity) {
        if (entity == null) {
            return CleanupEntityType.UNKNOWN;
        }
        
        if (isCleanableItem(entity)) {
            return CleanupEntityType.ITEM;
        }
        
        if (isCleanableExperienceOrb(entity)) {
            return CleanupEntityType.EXPERIENCE_ORB;
        }
        
        if (isCleanableArrow(entity)) {
            return CleanupEntityType.ARROW;
        }
        
        if (isCleanableFallingBlock(entity)) {
            return CleanupEntityType.FALLING_BLOCK;
        }
        
        if (isMob(entity)) {
            return CleanupEntityType.MOB;
        }
        
        return CleanupEntityType.OTHER;
    }
    
    /**
     * 清理实体类型枚举
     */
    public enum CleanupEntityType {
        ITEM,
        EXPERIENCE_ORB,
        ARROW,
        FALLING_BLOCK,
        MOB,
        OTHER,
        UNKNOWN
    }
}