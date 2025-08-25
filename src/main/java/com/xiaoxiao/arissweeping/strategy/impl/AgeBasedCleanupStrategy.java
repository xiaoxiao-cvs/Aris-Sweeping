package com.xiaoxiao.arissweeping.strategy.impl;

import com.xiaoxiao.arissweeping.strategy.AbstractCleanupStrategy;
import org.bukkit.entity.*;
import org.bukkit.World;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于年龄的清理策略
 * 根据实体存在时间进行清理
 */
public class AgeBasedCleanupStrategy extends AbstractCleanupStrategy {
    
    private static final String CONFIG_ITEM_MAX_AGE = "item_max_age";
    private static final String CONFIG_XP_MAX_AGE = "xp_max_age";
    private static final String CONFIG_ARROW_MAX_AGE = "arrow_max_age";
    private static final String CONFIG_MOB_MAX_AGE = "mob_max_age";
    private static final String CONFIG_PROTECT_NAMED = "protect_named";
    private static final String CONFIG_PROTECT_PERSISTENT = "protect_persistent";
    
    @Override
    public String getName() {
        return "AgeBasedCleanup";
    }
    
    @Override
    public String getDescription() {
        return "根据实体存在时间进行清理，超过指定年龄的实体将被移除";
    }
    
    @Override
    public int getPriority() {
        return 100; // 中等优先级
    }
    
    @Override
    protected void initializeDefaultConfiguration() {
        setConfigValue(CONFIG_ITEM_MAX_AGE, 300); // 5分钟
        setConfigValue(CONFIG_XP_MAX_AGE, 180); // 3分钟
        setConfigValue(CONFIG_ARROW_MAX_AGE, 120); // 2分钟
        setConfigValue(CONFIG_MOB_MAX_AGE, 600); // 10分钟
        setConfigValue(CONFIG_PROTECT_NAMED, true);
        setConfigValue(CONFIG_PROTECT_PERSISTENT, true);
    }
    
    @Override
    public boolean isApplicable(World world, List<Entity> entities) {
        if (world == null || entities == null || entities.isEmpty()) {
            return false;
        }
        
        // 检查是否有可清理的实体类型
        return entities.stream().anyMatch(entity -> 
            entity instanceof Item ||
            entity instanceof ExperienceOrb ||
            entity instanceof Arrow ||
            entity instanceof LivingEntity
        );
    }
    
    @Override
    protected CleanupResult doExecute(World world, List<Entity> entities) {
        long startTime = System.currentTimeMillis();
        
        List<Entity> toRemove = new ArrayList<>(); 
        Map<String, Integer> typeStats = new HashMap<>();
        
        int itemMaxAge = getConfigValue(CONFIG_ITEM_MAX_AGE, 300);
        int xpMaxAge = getConfigValue(CONFIG_XP_MAX_AGE, 180);
        int arrowMaxAge = getConfigValue(CONFIG_ARROW_MAX_AGE, 120);
        int mobMaxAge = getConfigValue(CONFIG_MOB_MAX_AGE, 600);
        boolean protectNamed = getConfigValue(CONFIG_PROTECT_NAMED, true);
        boolean protectPersistent = getConfigValue(CONFIG_PROTECT_PERSISTENT, true);
        
        for (Entity entity : entities) {
            if (entity == null || !entity.isValid()) {
                continue;
            }
            
            // 保护有自定义名称的实体
            if (protectNamed && entity.getCustomName() != null) {
                continue;
            }
            
            // 保护持久化实体
            if (protectPersistent && entity.isPersistent()) {
                continue;
            }
            
            boolean shouldRemove = false;
            String entityType = entity.getType().name();
            int entityAge = entity.getTicksLived() / 20; // 转换为秒
            
            if (entity instanceof Item) {
                shouldRemove = entityAge > itemMaxAge;
            } else if (entity instanceof ExperienceOrb) {
                shouldRemove = entityAge > xpMaxAge;
            } else if (entity instanceof Arrow) {
                Arrow arrow = (Arrow) entity;
                // 只清理已经落地的箭
                shouldRemove = entityAge > arrowMaxAge && arrow.isInBlock();
            } else if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) entity;
                // 不清理玩家和有AI的生物
                if (!(entity instanceof Player) && !living.hasAI()) {
                    shouldRemove = entityAge > mobMaxAge;
                }
            }
            
            if (shouldRemove) {
                toRemove.add(entity);
                typeStats.merge(entityType, 1, Integer::sum);
            }
        }
        
        // 执行移除
        int removedCount = batchRemoveEntities(toRemove);
        long executionTime = System.currentTimeMillis() - startTime;
        
        boolean success = removedCount >= 0; // 只要没有异常就算成功
        String errorMessage = success ? null : "部分实体移除失败";
        
        return new CleanupResult(
            entities.size(),
            removedCount,
            executionTime,
            success,
            errorMessage,
            typeStats
        );
    }
    
    @Override
    protected boolean doValidateConfiguration(Map<String, Object> config) {
        try {
            // 验证年龄配置
            if (config.containsKey(CONFIG_ITEM_MAX_AGE)) {
                int itemAge = (Integer) config.get(CONFIG_ITEM_MAX_AGE);
                if (itemAge < 0 || itemAge > 3600) { // 最大1小时
                    return false;
                }
            }
            
            if (config.containsKey(CONFIG_XP_MAX_AGE)) {
                int xpAge = (Integer) config.get(CONFIG_XP_MAX_AGE);
                if (xpAge < 0 || xpAge > 1800) { // 最大30分钟
                    return false;
                }
            }
            
            if (config.containsKey(CONFIG_ARROW_MAX_AGE)) {
                int arrowAge = (Integer) config.get(CONFIG_ARROW_MAX_AGE);
                if (arrowAge < 0 || arrowAge > 1800) { // 最大30分钟
                    return false;
                }
            }
            
            if (config.containsKey(CONFIG_MOB_MAX_AGE)) {
                int mobAge = (Integer) config.get(CONFIG_MOB_MAX_AGE);
                if (mobAge < 0 || mobAge > 7200) { // 最大2小时
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            logger.warning("配置验证异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取实体年龄（秒）
     */
    public int getEntityAge(Entity entity) {
        return entity != null ? entity.getTicksLived() / 20 : 0;
    }
    
    /**
     * 检查实体是否应该被保护
     */
    public boolean isEntityProtected(Entity entity) {
        if (entity == null) {
            return true;
        }
        
        boolean protectNamed = getConfigValue(CONFIG_PROTECT_NAMED, true);
        boolean protectPersistent = getConfigValue(CONFIG_PROTECT_PERSISTENT, true);
        
        if (protectNamed && entity.getCustomName() != null) {
            return true;
        }
        
        if (protectPersistent && entity.isPersistent()) {
            return true;
        }
        
        // 玩家总是被保护
        if (entity instanceof Player) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取实体的最大年龄限制
     */
    public int getMaxAgeForEntity(Entity entity) {
        if (entity instanceof Item) {
            return getConfigValue(CONFIG_ITEM_MAX_AGE, 300);
        } else if (entity instanceof ExperienceOrb) {
            return getConfigValue(CONFIG_XP_MAX_AGE, 180);
        } else if (entity instanceof Arrow) {
            return getConfigValue(CONFIG_ARROW_MAX_AGE, 120);
        } else if (entity instanceof LivingEntity) {
            return getConfigValue(CONFIG_MOB_MAX_AGE, 600);
        }
        
        return Integer.MAX_VALUE; // 其他实体不限制年龄
    }
    
    /**
     * 获取按类型分组的过期实体
     */
    public Map<String, List<Entity>> getExpiredEntitiesByType(List<Entity> entities) {
        Map<String, List<Entity>> expiredByType = new HashMap<>();
        
        for (Entity entity : entities) {
            if (entity == null || !entity.isValid() || isEntityProtected(entity)) {
                continue;
            }
            
            int entityAge = getEntityAge(entity);
            int maxAge = getMaxAgeForEntity(entity);
            
            if (entityAge > maxAge) {
                String type = entity.getType().name();
                expiredByType.computeIfAbsent(type, k -> new ArrayList<>()).add(entity);
            }
        }
        
        return expiredByType;
    }
    
    @Override
    protected void onConfigurationUpdated() {
        logger.info(String.format("年龄清理策略配置已更新: 物品=%ds, 经验=%ds, 箭=%ds, 生物=%ds",
            getConfigValue(CONFIG_ITEM_MAX_AGE, 300),
            getConfigValue(CONFIG_XP_MAX_AGE, 180),
            getConfigValue(CONFIG_ARROW_MAX_AGE, 120),
            getConfigValue(CONFIG_MOB_MAX_AGE, 600)
        ));
    }
}