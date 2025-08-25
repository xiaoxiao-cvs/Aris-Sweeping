package com.xiaoxiao.arissweeping.chain.handlers;

import com.xiaoxiao.arissweeping.chain.AbstractEntityCheckHandler;
import com.xiaoxiao.arissweeping.chain.EntityCheckContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;

/**
 * 经验球年龄检查处理器
 * 检查经验球是否达到清理年龄阈值
 */
public class ExperienceOrbAgeCheckHandler extends AbstractEntityCheckHandler {
    
    public ExperienceOrbAgeCheckHandler() {
        super("ExperienceOrbAgeCheck", 30);
    }
    
    @Override
    protected CheckResult doCheck(Entity entity, EntityCheckContext context) {
        // 只处理经验球
        if (!(entity instanceof ExperienceOrb)) {
            return CheckResult.CONTINUE_CHECK;
        }
        
        // 检查配置是否启用经验球清理
        if (!context.getConfig().isCleanupExperienceOrbs()) {
            return CheckResult.SHOULD_KEEP;
        }
        
        ExperienceOrb orb = (ExperienceOrb) entity;
        
        // 检查年龄
        int minAge = context.getConfig().getExperienceOrbMinAge();
        if (!isEntityOldEnough(orb, minAge)) {
            return CheckResult.SHOULD_KEEP;
        }
        
        // 更新统计
        context.getStats().incrementExperienceOrbsCleaned();
        
        return CheckResult.SHOULD_CLEAN;
    }
}