package com.xiaoxiao.arissweeping.util;

import org.bukkit.entity.EntityType;

import java.util.*;

/**
 * 优化的实体计数器，避免Map<EntityType, Integer>的装箱拆箱开销
 * 使用数组存储常见实体类型的计数，提高性能
 */
public class EntityCounter {
    // 常见实体类型的索引映射
    private static final Map<EntityType, Integer> TYPE_INDEX_MAP = new HashMap<>();
    private static final EntityType[] INDEX_TYPE_MAP;
    private static final int COMMON_TYPES_COUNT;
    
    static {
        // 定义常见的实体类型
        EntityType[] commonTypes = {
            EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.CHICKEN,
            EntityType.HORSE, EntityType.DONKEY, EntityType.MULE, EntityType.LLAMA,
            EntityType.RABBIT, EntityType.WOLF, EntityType.CAT, EntityType.PARROT,
            EntityType.ITEM, EntityType.EXPERIENCE_ORB, EntityType.ARROW,
            EntityType.FALLING_BLOCK, EntityType.ZOMBIE, EntityType.SKELETON,
            EntityType.CREEPER, EntityType.SPIDER, EntityType.ENDERMAN,
            EntityType.ARMOR_STAND, EntityType.ITEM_FRAME, EntityType.PAINTING,
            EntityType.MINECART, EntityType.BOAT
        };
        
        COMMON_TYPES_COUNT = commonTypes.length;
        INDEX_TYPE_MAP = commonTypes;
        
        for (int i = 0; i < commonTypes.length; i++) {
            TYPE_INDEX_MAP.put(commonTypes[i], i);
        }
    }
    
    // 使用数组存储常见类型的计数，避免装箱
    private final int[] commonTypeCounts = new int[COMMON_TYPES_COUNT];
    // 使用Map存储不常见类型的计数
    private final Map<EntityType, Integer> uncommonTypeCounts = new HashMap<>();
    private int totalCount = 0;
    
    /**
     * 增加指定实体类型的计数
     */
    public void increment(EntityType type) {
        if (type == null) return;
        
        Integer index = TYPE_INDEX_MAP.get(type);
        if (index != null) {
            commonTypeCounts[index]++;
        } else {
            uncommonTypeCounts.put(type, uncommonTypeCounts.getOrDefault(type, 0) + 1);
        }
        totalCount++;
    }
    
    /**
     * 增加指定实体类型的计数（指定数量）
     */
    public void add(EntityType type, int count) {
        if (type == null || count <= 0) return;
        
        Integer index = TYPE_INDEX_MAP.get(type);
        if (index != null) {
            commonTypeCounts[index] += count;
        } else {
            uncommonTypeCounts.put(type, uncommonTypeCounts.getOrDefault(type, 0) + count);
        }
        totalCount += count;
    }
    
    /**
     * 获取指定实体类型的计数
     */
    public int getCount(EntityType type) {
        if (type == null) return 0;
        
        Integer index = TYPE_INDEX_MAP.get(type);
        if (index != null) {
            return commonTypeCounts[index];
        } else {
            return uncommonTypeCounts.getOrDefault(type, 0);
        }
    }
    
    /**
     * 获取总计数
     */
    public int getTotalCount() {
        return totalCount;
    }
    
    /**
     * 检查是否为空
     */
    public boolean isEmpty() {
        return totalCount == 0;
    }
    
    /**
     * 清空所有计数
     */
    public void clear() {
        Arrays.fill(commonTypeCounts, 0);
        uncommonTypeCounts.clear();
        totalCount = 0;
    }
    
    /**
     * 获取所有非零计数的实体类型
     */
    public Set<EntityType> getTypes() {
        Set<EntityType> types = new HashSet<>();
        
        // 添加常见类型
        for (int i = 0; i < COMMON_TYPES_COUNT; i++) {
            if (commonTypeCounts[i] > 0) {
                types.add(INDEX_TYPE_MAP[i]);
            }
        }
        
        // 添加不常见类型
        types.addAll(uncommonTypeCounts.keySet());
        
        return types;
    }
    
    /**
     * 转换为传统的Map格式（用于兼容性）
     */
    public Map<EntityType, Integer> toMap() {
        Map<EntityType, Integer> result = new HashMap<>();
        
        // 添加常见类型
        for (int i = 0; i < COMMON_TYPES_COUNT; i++) {
            if (commonTypeCounts[i] > 0) {
                result.put(INDEX_TYPE_MAP[i], commonTypeCounts[i]);
            }
        }
        
        // 添加不常见类型
        result.putAll(uncommonTypeCounts);
        
        return result;
    }
    
    /**
     * 从Map创建EntityCounter
     */
    public static EntityCounter fromMap(Map<EntityType, Integer> map) {
        EntityCounter counter = new EntityCounter();
        if (map != null) {
            for (Map.Entry<EntityType, Integer> entry : map.entrySet()) {
                counter.add(entry.getKey(), entry.getValue());
            }
        }
        return counter;
    }
    
    /**
     * 合并另一个计数器的数据
     */
    public void merge(EntityCounter other) {
        if (other == null) return;
        
        // 合并常见类型
        for (int i = 0; i < COMMON_TYPES_COUNT; i++) {
            commonTypeCounts[i] += other.commonTypeCounts[i];
        }
        
        // 合并不常见类型
        for (Map.Entry<EntityType, Integer> entry : other.uncommonTypeCounts.entrySet()) {
            uncommonTypeCounts.put(entry.getKey(), 
                uncommonTypeCounts.getOrDefault(entry.getKey(), 0) + entry.getValue());
        }
        
        totalCount += other.totalCount;
    }
    
    /**
     * 获取动物类型的总计数
     */
    public int getAnimalCount() {
        int count = 0;
        for (EntityType type : getTypes()) {
            if (EntityTypeUtils.isAnimal(null) && type.getEntityClass() != null) {
                // 简化判断，基于EntityType判断
                if (isAnimalType(type)) {
                    count += getCount(type);
                }
            }
        }
        return count;
    }
    
    private boolean isAnimalType(EntityType type) {
        return type == EntityType.COW || type == EntityType.PIG || 
               type == EntityType.SHEEP || type == EntityType.CHICKEN ||
               type == EntityType.HORSE || type == EntityType.DONKEY ||
               type == EntityType.MULE || type == EntityType.LLAMA ||
               type == EntityType.RABBIT || type == EntityType.WOLF ||
               type == EntityType.CAT || type == EntityType.PARROT;
    }
    
    @Override
    public String toString() {
        return "EntityCounter{totalCount=" + totalCount + ", types=" + getTypes().size() + "}";
    }
}