package com.xiaoxiao.arissweeping.strategy.impl;

import com.xiaoxiao.arissweeping.strategy.AbstractCleanupStrategy;
import org.bukkit.entity.*;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于密度的清理策略
 * 当区块中实体密度过高时进行清理
 */
public class DensityBasedCleanupStrategy extends AbstractCleanupStrategy {
    
    private static final String CONFIG_MAX_ENTITIES_PER_CHUNK = "max_entities_per_chunk";
    private static final String CONFIG_MAX_ITEMS_PER_CHUNK = "max_items_per_chunk";
    private static final String CONFIG_MAX_MOBS_PER_CHUNK = "max_mobs_per_chunk";
    private static final String CONFIG_MAX_XP_PER_CHUNK = "max_xp_per_chunk";
    private static final String CONFIG_CLEANUP_PERCENTAGE = "cleanup_percentage";
    private static final String CONFIG_PRIORITIZE_OLDER = "prioritize_older";
    private static final String CONFIG_PROTECT_NAMED = "protect_named";
    
    @Override
    public String getName() {
        return "DensityBasedCleanup";
    }
    
    @Override
    public String getDescription() {
        return "当区块中实体密度过高时进行清理，优先清理较老的实体";
    }
    
    @Override
    public int getPriority() {
        return 50; // 高优先级
    }
    
    @Override
    protected void initializeDefaultConfiguration() {
        setConfigValue(CONFIG_MAX_ENTITIES_PER_CHUNK, 100);
        setConfigValue(CONFIG_MAX_ITEMS_PER_CHUNK, 50);
        setConfigValue(CONFIG_MAX_MOBS_PER_CHUNK, 30);
        setConfigValue(CONFIG_MAX_XP_PER_CHUNK, 20);
        setConfigValue(CONFIG_CLEANUP_PERCENTAGE, 0.3); // 清理30%
        setConfigValue(CONFIG_PRIORITIZE_OLDER, true);
        setConfigValue(CONFIG_PROTECT_NAMED, true);
    }
    
    @Override
    public boolean isApplicable(World world, List<Entity> entities) {
        if (world == null || entities == null || entities.isEmpty()) {
            return false;
        }
        
        // 检查是否有密度过高的区块
        Map<String, List<Entity>> entitiesByChunk = groupEntitiesByChunk(entities);
        
        int maxEntitiesPerChunk = getConfigValue(CONFIG_MAX_ENTITIES_PER_CHUNK, 100);
        
        return entitiesByChunk.values().stream()
                .anyMatch(chunkEntities -> chunkEntities.size() > maxEntitiesPerChunk);
    }
    
    @Override
    protected CleanupResult doExecute(World world, List<Entity> entities) {
        long startTime = System.currentTimeMillis();
        
        List<Entity> toRemove = new ArrayList<>();
        Map<String, Integer> typeStats = new HashMap<>();
        
        // 获取配置
        int maxEntitiesPerChunk = getConfigValue(CONFIG_MAX_ENTITIES_PER_CHUNK, 100);
        int maxItemsPerChunk = getConfigValue(CONFIG_MAX_ITEMS_PER_CHUNK, 50);
        int maxMobsPerChunk = getConfigValue(CONFIG_MAX_MOBS_PER_CHUNK, 30);
        int maxXpPerChunk = getConfigValue(CONFIG_MAX_XP_PER_CHUNK, 20);
        double cleanupPercentage = getConfigValue(CONFIG_CLEANUP_PERCENTAGE, 0.3);
        boolean prioritizeOlder = getConfigValue(CONFIG_PRIORITIZE_OLDER, true);
        boolean protectNamed = getConfigValue(CONFIG_PROTECT_NAMED, true);
        
        // 按区块分组实体
        Map<String, List<Entity>> entitiesByChunk = groupEntitiesByChunk(entities);
        
        for (Map.Entry<String, List<Entity>> entry : entitiesByChunk.entrySet()) {
            String chunkKey = entry.getKey();
            List<Entity> chunkEntities = entry.getValue();
            
            // 检查总体密度
            if (chunkEntities.size() > maxEntitiesPerChunk) {
                List<Entity> candidates = selectCleanupCandidates(
                    chunkEntities, cleanupPercentage, prioritizeOlder, protectNamed
                );
                toRemove.addAll(candidates);
            }
            
            // 按类型检查密度
            Map<Class<?>, List<Entity>> entitiesByType = groupEntitiesByType(chunkEntities);
            
            // 检查物品密度
            List<Entity> items = entitiesByType.getOrDefault(Item.class, Collections.emptyList());
            if (items.size() > maxItemsPerChunk) {
                List<Entity> itemsToRemove = selectCleanupCandidates(
                    items, cleanupPercentage, prioritizeOlder, protectNamed
                );
                toRemove.addAll(itemsToRemove);
            }
            
            // 检查生物密度
            List<Entity> mobs = entitiesByType.entrySet().stream()
                    .filter(e -> LivingEntity.class.isAssignableFrom(e.getKey()) && 
                               !Player.class.isAssignableFrom(e.getKey()))
                    .flatMap(e -> e.getValue().stream())
                    .collect(Collectors.toList());
            if (mobs.size() > maxMobsPerChunk) {
                List<Entity> mobsToRemove = selectCleanupCandidates(
                    mobs, cleanupPercentage, prioritizeOlder, protectNamed
                );
                toRemove.addAll(mobsToRemove);
            }
            
            // 检查经验球密度
            List<Entity> xpOrbs = entitiesByType.getOrDefault(ExperienceOrb.class, Collections.emptyList());
            if (xpOrbs.size() > maxXpPerChunk) {
                List<Entity> xpToRemove = selectCleanupCandidates(
                    xpOrbs, cleanupPercentage, prioritizeOlder, protectNamed
                );
                toRemove.addAll(xpToRemove);
            }
        }
        
        // 统计类型
        for (Entity entity : toRemove) {
            if (entity != null) {
                typeStats.merge(entity.getType().name(), 1, Integer::sum);
            }
        }
        
        // 执行移除
        int removedCount = batchRemoveEntities(toRemove);
        long executionTime = System.currentTimeMillis() - startTime;
        
        boolean success = removedCount >= 0;
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
    
    /**
     * 按区块分组实体
     */
    private Map<String, List<Entity>> groupEntitiesByChunk(List<Entity> entities) {
        Map<String, List<Entity>> grouped = new HashMap<>();
        
        for (Entity entity : entities) {
            if (entity != null && entity.isValid()) {
                Location loc = entity.getLocation();
                String chunkKey = getChunkKey(loc.getChunk());
                grouped.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(entity);
            }
        }
        
        return grouped;
    }
    
    /**
     * 按类型分组实体
     */
    private Map<Class<?>, List<Entity>> groupEntitiesByType(List<Entity> entities) {
        Map<Class<?>, List<Entity>> grouped = new HashMap<>();
        
        for (Entity entity : entities) {
            if (entity != null && entity.isValid()) {
                Class<?> entityClass = getEntityClass(entity);
                grouped.computeIfAbsent(entityClass, k -> new ArrayList<>()).add(entity);
            }
        }
        
        return grouped;
    }
    
    /**
     * 获取实体的主要类别
     */
    private Class<?> getEntityClass(Entity entity) {
        if (entity instanceof Item) {
            return Item.class;
        } else if (entity instanceof ExperienceOrb) {
            return ExperienceOrb.class;
        } else if (entity instanceof Player) {
            return Player.class;
        } else if (entity instanceof LivingEntity) {
            return LivingEntity.class;
        } else {
            return Entity.class;
        }
    }
    
    /**
     * 选择清理候选实体
     */
    private List<Entity> selectCleanupCandidates(List<Entity> entities, double percentage, 
                                                boolean prioritizeOlder, boolean protectNamed) {
        List<Entity> candidates = new ArrayList<>();
        
        // 过滤受保护的实体
        List<Entity> cleanableEntities = entities.stream()
                .filter(entity -> entity != null && entity.isValid())
                .filter(entity -> !protectNamed || entity.getCustomName() == null)
                .filter(entity -> !(entity instanceof Player))
                .filter(entity -> !entity.isPersistent())
                .collect(Collectors.toList());
        
        if (cleanableEntities.isEmpty()) {
            return candidates;
        }
        
        // 排序：优先清理较老的实体
        if (prioritizeOlder) {
            cleanableEntities.sort((e1, e2) -> Integer.compare(e2.getTicksLived(), e1.getTicksLived()));
        }
        
        // 计算要清理的数量
        int toRemoveCount = Math.max(1, (int) (cleanableEntities.size() * percentage));
        
        // 选择要清理的实体
        for (int i = 0; i < Math.min(toRemoveCount, cleanableEntities.size()); i++) {
            candidates.add(cleanableEntities.get(i));
        }
        
        return candidates;
    }
    
    /**
     * 获取区块键
     */
    private String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + "," + chunk.getZ();
    }
    
    @Override
    protected boolean doValidateConfiguration(Map<String, Object> config) {
        try {
            // 验证密度限制
            if (config.containsKey(CONFIG_MAX_ENTITIES_PER_CHUNK)) {
                int maxEntities = (Integer) config.get(CONFIG_MAX_ENTITIES_PER_CHUNK);
                if (maxEntities < 1 || maxEntities > 1000) {
                    return false;
                }
            }
            
            if (config.containsKey(CONFIG_MAX_ITEMS_PER_CHUNK)) {
                int maxItems = (Integer) config.get(CONFIG_MAX_ITEMS_PER_CHUNK);
                if (maxItems < 1 || maxItems > 500) {
                    return false;
                }
            }
            
            if (config.containsKey(CONFIG_MAX_MOBS_PER_CHUNK)) {
                int maxMobs = (Integer) config.get(CONFIG_MAX_MOBS_PER_CHUNK);
                if (maxMobs < 1 || maxMobs > 200) {
                    return false;
                }
            }
            
            if (config.containsKey(CONFIG_CLEANUP_PERCENTAGE)) {
                double percentage = (Double) config.get(CONFIG_CLEANUP_PERCENTAGE);
                if (percentage < 0.1 || percentage > 1.0) {
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
     * 获取区块密度信息
     */
    public Map<String, ChunkDensityInfo> getChunkDensityInfo(List<Entity> entities) {
        Map<String, ChunkDensityInfo> densityInfo = new HashMap<>();
        Map<String, List<Entity>> entitiesByChunk = groupEntitiesByChunk(entities);
        
        for (Map.Entry<String, List<Entity>> entry : entitiesByChunk.entrySet()) {
            String chunkKey = entry.getKey();
            List<Entity> chunkEntities = entry.getValue();
            
            ChunkDensityInfo info = new ChunkDensityInfo(chunkKey, chunkEntities);
            densityInfo.put(chunkKey, info);
        }
        
        return densityInfo;
    }
    
    /**
     * 区块密度信息类
     */
    public static class ChunkDensityInfo {
        private final String chunkKey;
        private final int totalEntities;
        private final int itemCount;
        private final int mobCount;
        private final int xpCount;
        private final int otherCount;
        
        public ChunkDensityInfo(String chunkKey, List<Entity> entities) {
            this.chunkKey = chunkKey;
            this.totalEntities = entities.size();
            
            int items = 0, mobs = 0, xp = 0, others = 0;
            
            for (Entity entity : entities) {
                if (entity instanceof Item) {
                    items++;
                } else if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                    mobs++;
                } else if (entity instanceof ExperienceOrb) {
                    xp++;
                } else {
                    others++;
                }
            }
            
            this.itemCount = items;
            this.mobCount = mobs;
            this.xpCount = xp;
            this.otherCount = others;
        }
        
        // Getters
        public String getChunkKey() { return chunkKey; }
        public int getTotalEntities() { return totalEntities; }
        public int getItemCount() { return itemCount; }
        public int getMobCount() { return mobCount; }
        public int getXpCount() { return xpCount; }
        public int getOtherCount() { return otherCount; }
        
        public boolean isOverloaded(int maxTotal, int maxItems, int maxMobs, int maxXp) {
            return totalEntities > maxTotal || 
                   itemCount > maxItems || 
                   mobCount > maxMobs || 
                   xpCount > maxXp;
        }
        
        @Override
        public String toString() {
            return String.format("ChunkDensity{%s: total=%d, items=%d, mobs=%d, xp=%d, others=%d}",
                chunkKey, totalEntities, itemCount, mobCount, xpCount, otherCount);
        }
    }
    
    @Override
    protected void onConfigurationUpdated() {
        logger.info(String.format("密度清理策略配置已更新: 总实体=%d, 物品=%d, 生物=%d, 经验=%d, 清理比例=%.1f%%",
            getConfigValue(CONFIG_MAX_ENTITIES_PER_CHUNK, 100),
            getConfigValue(CONFIG_MAX_ITEMS_PER_CHUNK, 50),
            getConfigValue(CONFIG_MAX_MOBS_PER_CHUNK, 30),
            getConfigValue(CONFIG_MAX_XP_PER_CHUNK, 20),
            getConfigValue(CONFIG_CLEANUP_PERCENTAGE, 0.3) * 100
        ));
    }
}