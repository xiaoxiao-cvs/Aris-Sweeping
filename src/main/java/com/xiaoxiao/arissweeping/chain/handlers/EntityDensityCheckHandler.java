package com.xiaoxiao.arissweeping.chain.handlers;

import com.xiaoxiao.arissweeping.chain.AbstractEntityCheckHandler;
import com.xiaoxiao.arissweeping.chain.EntityCheckContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;

/**
 * 实体密度检查处理器
 * 当区块中实体密度过高时，优先清理较老的实体
 */
public class EntityDensityCheckHandler extends AbstractEntityCheckHandler {
    
    public EntityDensityCheckHandler() {
        super("EntityDensityCheck", 50);
    }
    
    @Override
    protected CheckResult doCheck(Entity entity, EntityCheckContext context) {
        // 更新实体统计
        context.incrementEntitiesInChunk();
        
        // 检查是否超过密度阈值
        if (!context.isChunkOverloaded()) {
            return CheckResult.CONTINUE_CHECK;
        }
        
        // 区块过载，根据实体类型和年龄进行清理
        return handleOverloadedChunk(entity, context);
    }
    
    private CheckResult handleOverloadedChunk(Entity entity, EntityCheckContext context) {
        // 保护重要实体
        if (hasCustomName(entity) || isPersistent(entity)) {
            return CheckResult.SHOULD_KEEP;
        }
        
        // 根据实体类型设置不同的清理策略
        if (entity instanceof Item) {
            return handleItemDensity(entity, context);
        } else if (entity instanceof ExperienceOrb) {
            return handleExperienceOrbDensity(entity, context);
        } else if (entity instanceof Arrow) {
            return handleArrowDensity(entity, context);
        } else if (entity instanceof LivingEntity) {
            return handleMobDensity(entity, context);
        }
        
        // 其他实体类型，使用通用策略
        return handleGenericEntityDensity(entity, context);
    }
    
    private CheckResult handleItemDensity(Entity entity, EntityCheckContext context) {
        // 物品密度过高时，清理较老的物品
        if (context.hasExcessiveItems()) {
            int minAge = Math.max(context.getConfig().getItemMinAge() / 2, 50); // 降低年龄要求
            if (isEntityOldEnough(entity, minAge)) {
                return CheckResult.SHOULD_CLEAN;
            }
        }
        
        return CheckResult.CONTINUE_CHECK;
    }
    
    private CheckResult handleExperienceOrbDensity(Entity entity, EntityCheckContext context) {
        // 经验球密度过高时，清理较老的经验球
        int minAge = Math.max(context.getConfig().getExperienceOrbMinAge() / 2, 30);
        if (isEntityOldEnough(entity, minAge)) {
            return CheckResult.SHOULD_CLEAN;
        }
        
        return CheckResult.CONTINUE_CHECK;
    }
    
    private CheckResult handleArrowDensity(Entity entity, EntityCheckContext context) {
        // 箭矢密度过高时，清理较老的箭矢
        int minAge = Math.max(context.getConfig().getArrowMinAge() / 2, 20);
        if (isEntityOldEnough(entity, minAge)) {
            return CheckResult.SHOULD_CLEAN;
        }
        
        return CheckResult.CONTINUE_CHECK;
    }
    
    private CheckResult handleMobDensity(Entity entity, EntityCheckContext context) {
        LivingEntity mob = (LivingEntity) entity;
        
        // 生物密度过高时，只清理非持久化的生物
        if (!isPersistent(mob) && !hasCustomName(mob)) {
            // 检查生物年龄
            if (isEntityOldEnough(mob, 1200)) { // 1分钟
                context.incrementMobsInChunk();
                return CheckResult.SHOULD_CLEAN;
            }
        }
        
        return CheckResult.CONTINUE_CHECK;
    }
    
    private CheckResult handleGenericEntityDensity(Entity entity, EntityCheckContext context) {
        // 通用实体密度处理
        if (isEntityOldEnough(entity, 600)) { // 30秒
            return CheckResult.SHOULD_CLEAN;
        }
        
        return CheckResult.CONTINUE_CHECK;
    }
}