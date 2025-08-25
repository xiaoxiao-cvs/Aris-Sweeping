package com.xiaoxiao.arissweeping.chain.handlers;

import com.xiaoxiao.arissweeping.chain.AbstractEntityCheckHandler;
import com.xiaoxiao.arissweeping.chain.EntityCheckContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.Location;

/**
 * 玩家附近检查处理器
 * 检查实体是否在玩家附近，避免清理玩家附近的重要实体
 */
public class PlayerProximityCheckHandler extends AbstractEntityCheckHandler {
    
    private static final double PROTECTION_RADIUS = 16.0; // 保护半径（方块）
    private static final double PROTECTION_RADIUS_SQUARED = PROTECTION_RADIUS * PROTECTION_RADIUS;
    
    public PlayerProximityCheckHandler() {
        super("PlayerProximityCheck", 10); // 高优先级
    }
    
    @Override
    protected CheckResult doCheck(Entity entity, EntityCheckContext context) {
        // 如果没有附近的玩家，继续检查
        if (!context.hasNearbyPlayers()) {
            return CheckResult.CONTINUE_CHECK;
        }
        
        Location entityLocation = entity.getLocation();
        
        // 检查是否在任何玩家的保护范围内
        for (Player player : context.getNearbyPlayers()) {
            Location playerLocation = player.getLocation();
            
            // 只检查同一世界的玩家
            if (!playerLocation.getWorld().equals(entityLocation.getWorld())) {
                continue;
            }
            
            // 计算距离的平方（避免开方运算）
            double distanceSquared = playerLocation.distanceSquared(entityLocation);
            
            if (distanceSquared <= PROTECTION_RADIUS_SQUARED) {
                // 在玩家保护范围内，根据实体类型决定是否保留
                return shouldProtectFromPlayer(entity, player, Math.sqrt(distanceSquared));
            }
        }
        
        // 不在任何玩家的保护范围内，继续检查
        return CheckResult.CONTINUE_CHECK;
    }
    
    /**
     * 判断是否应该保护实体免受玩家附近的清理
     * @param entity 实体
     * @param player 附近的玩家
     * @param distance 距离
     * @return 检查结果
     */
    private CheckResult shouldProtectFromPlayer(Entity entity, Player player, double distance) {
        // 非常近的距离（5格内）- 强制保护
        if (distance <= 5.0) {
            return CheckResult.SHOULD_KEEP;
        }
        
        // 中等距离（5-10格）- 根据实体类型决定
        if (distance <= 10.0) {
            // 保护有价值的实体
            if (hasCustomName(entity) || isPersistent(entity)) {
                return CheckResult.SHOULD_KEEP;
            }
        }
        
        // 较远距离（10-16格）- 只保护特殊实体
        if (hasCustomName(entity) && isPersistent(entity)) {
            return CheckResult.SHOULD_KEEP;
        }
        
        // 继续检查其他条件
        return CheckResult.CONTINUE_CHECK;
    }
}