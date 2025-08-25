package com.xiaoxiao.arissweeping.chain.handlers;

import com.xiaoxiao.arissweeping.chain.AbstractEntityCheckHandler;
import com.xiaoxiao.arissweeping.chain.EntityCheckContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.AbstractArrow;

/**
 * 箭矢年龄检查处理器
 * 检查箭矢是否达到清理年龄阈值
 */
public class ArrowAgeCheckHandler extends AbstractEntityCheckHandler {
    
    public ArrowAgeCheckHandler() {
        super("ArrowAgeCheck", 40);
    }
    
    @Override
    protected CheckResult doCheck(Entity entity, EntityCheckContext context) {
        // 只处理箭矢类实体
        if (!(entity instanceof AbstractArrow)) {
            return CheckResult.CONTINUE_CHECK;
        }
        
        // 检查配置是否启用箭矢清理
        if (!context.getConfig().isCleanupArrows()) {
            return CheckResult.SHOULD_KEEP;
        }
        
        AbstractArrow arrow = (AbstractArrow) entity;
        
        // 保护有自定义名称的箭矢
        if (hasCustomName(arrow)) {
            return CheckResult.SHOULD_KEEP;
        }
        
        // 检查年龄
        int minAge = context.getConfig().getArrowMinAge();
        if (!isEntityOldEnough(arrow, minAge)) {
            return CheckResult.SHOULD_KEEP;
        }
        
        // 检查箭矢是否在地面上（不在飞行中）
        if (arrow.isInBlock() || arrow.getVelocity().lengthSquared() < 0.01) {
            // 更新统计
            context.getStats().incrementArrowsCleaned();
            return CheckResult.SHOULD_CLEAN;
        }
        
        // 飞行中的箭矢暂时保留
        return CheckResult.SHOULD_KEEP;
    }
}