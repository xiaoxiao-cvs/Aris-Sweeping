package com.xiaoxiao.arissweeping.integration;

import org.bukkit.entity.Entity;
import org.bukkit.World;

/**
 * 实体清理处理器接口
 */
public interface EntityCleanupHandler {
    
    /**
     * 检查实体是否应该被清理
     * @param entity 要检查的实体
     * @param world 实体所在的世界
     * @return 如果实体应该被清理则返回true
     */
    boolean shouldCleanup(Entity entity, World world);
    
    /**
     * 执行实体清理
     * @param entity 要清理的实体
     * @return 如果清理成功则返回true
     */
    boolean cleanup(Entity entity);
    
    /**
     * 获取处理器名称
     * @return 处理器名称
     */
    String getName();
    
    /**
     * 获取处理器优先级
     * @return 优先级值，数值越小优先级越高
     */
    int getPriority();
}