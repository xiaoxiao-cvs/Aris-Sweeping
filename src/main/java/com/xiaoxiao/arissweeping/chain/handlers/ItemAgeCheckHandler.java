package com.xiaoxiao.arissweeping.chain.handlers;

import com.xiaoxiao.arissweeping.chain.AbstractEntityCheckHandler;
import com.xiaoxiao.arissweeping.chain.EntityCheckContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;

/**
 * 物品年龄检查处理器
 * 检查物品是否达到清理年龄阈值
 */
public class ItemAgeCheckHandler extends AbstractEntityCheckHandler {
    
    public ItemAgeCheckHandler() {
        super("ItemAgeCheck", 100);
    }
    
    @Override
    protected CheckResult doCheck(Entity entity, EntityCheckContext context) {
        // 只处理物品实体
        if (!(entity instanceof Item)) {
            return CheckResult.CONTINUE_CHECK;
        }
        
        Item item = (Item) entity;
        
        // 检查是否启用物品清理
        if (!context.getConfig().isCleanupItems()) {
            return CheckResult.SHOULD_KEEP;
        }
        
        // 检查物品年龄
        int minAge = context.getConfig().getItemMinAge();
        if (!isEntityOldEnough(item, minAge)) {
            return CheckResult.SHOULD_KEEP;
        }
        
        // 检查是否有自定义名称（通常表示重要物品）
        if (hasCustomName(item)) {
            return CheckResult.SHOULD_KEEP;
        }
        
        // 更新统计信息
        context.incrementItemsInChunk();
        
        // 物品符合清理条件
        return CheckResult.SHOULD_CLEAN;
    }
}