package com.xiaoxiao.arissweeping.chain;

import org.bukkit.entity.Entity;

/**
 * 抽象实体检查处理器
 * 提供责任链模式的基本实现
 */
public abstract class AbstractEntityCheckHandler implements EntityCheckHandler {
    
    protected EntityCheckHandler nextHandler;
    protected final String handlerName;
    protected final int priority;
    
    protected AbstractEntityCheckHandler(String handlerName, int priority) {
        this.handlerName = handlerName;
        this.priority = priority;
    }
    
    @Override
    public void setNextHandler(EntityCheckHandler nextHandler) {
        this.nextHandler = nextHandler;
    }
    
    @Override
    public EntityCheckHandler getNextHandler() {
        return nextHandler;
    }
    
    @Override
    public String getHandlerName() {
        return handlerName;
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
    
    @Override
    public final CheckResult checkEntity(Entity entity, EntityCheckContext context) {
        // 执行具体的检查逻辑
        CheckResult result = doCheck(entity, context);
        
        // 如果结果是继续检查，且有下一个处理器，则传递给下一个处理器
        if (result == CheckResult.CONTINUE_CHECK && nextHandler != null) {
            return nextHandler.checkEntity(entity, context);
        }
        
        return result;
    }
    
    /**
     * 具体的检查逻辑，由子类实现
     * @param entity 要检查的实体
     * @param context 检查上下文
     * @return 检查结果
     */
    protected abstract CheckResult doCheck(Entity entity, EntityCheckContext context);
    
    /**
     * 检查实体年龄是否超过阈值
     * @param entity 实体
     * @param ageThreshold 年龄阈值（tick）
     * @return 是否超过阈值
     */
    protected boolean isEntityOldEnough(Entity entity, int ageThreshold) {
        return entity.getTicksLived() >= ageThreshold;
    }
    
    /**
     * 检查实体是否有自定义名称
     * @param entity 实体
     * @return 是否有自定义名称
     */
    protected boolean hasCustomName(Entity entity) {
        return entity.getCustomName() != null && !entity.getCustomName().isEmpty();
    }
    
    /**
     * 检查实体是否持久化
     * @param entity 实体
     * @return 是否持久化
     */
    protected boolean isPersistent(Entity entity) {
        return entity.isPersistent();
    }
    
    @Override
    public String toString() {
        return String.format("%s(priority=%d)", handlerName, priority);
    }
}