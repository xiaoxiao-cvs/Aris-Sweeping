package com.xiaoxiao.arissweeping.cleanup;

import org.bukkit.entity.Entity;
import java.util.List;

/**
 * 实体过滤接口
 * 负责判断哪些实体应该被清理
 */
public interface EntityFilter {
    
    /**
     * 判断实体是否应该被清理
     * @param entity 要检查的实体
     * @return 如果应该清理返回true，否则返回false
     */
    boolean shouldCleanupEntity(Entity entity);
    
    /**
     * 批量清理实体
     * @param entities 要清理的实体列表
     * @return 实际清理的实体数量
     */
    int cleanupEntities(List<Entity> entities);
}