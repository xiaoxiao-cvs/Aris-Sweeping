package com.xiaoxiao.arissweeping.chain;

import org.bukkit.entity.Entity;

/**
 * 实体检查处理器接口
 * 使用责任链模式处理实体检查逻辑
 */
public interface EntityCheckHandler {
    
    /**
     * 检查结果枚举
     */
    enum CheckResult {
        SHOULD_CLEAN,    // 应该清理
        SHOULD_KEEP,     // 应该保留
        CONTINUE_CHECK   // 继续检查下一个处理器
    }
    
    /**
     * 检查实体是否应该被清理
     * @param entity 要检查的实体
     * @param context 检查上下文
     * @return 检查结果
     */
    CheckResult checkEntity(Entity entity, EntityCheckContext context);
    
    /**
     * 设置下一个处理器
     * @param nextHandler 下一个处理器
     */
    void setNextHandler(EntityCheckHandler nextHandler);
    
    /**
     * 获取下一个处理器
     * @return 下一个处理器
     */
    EntityCheckHandler getNextHandler();
    
    /**
     * 获取处理器名称
     * @return 处理器名称
     */
    String getHandlerName();
    
    /**
     * 获取处理器优先级
     * @return 优先级（数值越小优先级越高）
     */
    int getPriority();
}