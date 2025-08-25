package com.xiaoxiao.arissweeping.cleanup;

import com.xiaoxiao.arissweeping.chain.EntityCheckChain;
import com.xiaoxiao.arissweeping.chain.EntityCheckContext;
import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.CleanupStats;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.ArrayList;

/**
 * 基于责任链模式的实体过滤器
 * 使用责任链模式来判断实体是否应该被清理
 */
public class ChainBasedEntityFilter implements EntityFilter {
    
    private final EntityCheckChain checkChain;
    private final ModConfig config;
    
    public ChainBasedEntityFilter(ModConfig config) {
        this.config = config;
        this.checkChain = new EntityCheckChain(config);
    }
    
    @Override
    public boolean shouldCleanupEntity(Entity entity) {
        if (entity == null || !entity.isValid()) {
            return false;
        }
        
        // 永远不清理玩家
        if (entity instanceof Player) {
            return false;
        }
        
        // 创建检查上下文
        EntityCheckContext context = createContext(entity);
        
        // 使用责任链进行检查
        return checkChain.shouldCleanEntity(entity, context);
    }
    
    @Override
    public int cleanupEntities(List<Entity> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        
        int cleaned = 0;
        CleanupStats stats = new CleanupStats();
        
        // 按区块分组处理实体
        for (Entity entity : entities) {
            if (entity == null || !entity.isValid()) {
                continue;
            }
            
            // 创建上下文（包含区块信息）
            EntityCheckContext context = createContextWithStats(entity, stats);
            
            // 使用责任链检查
            if (checkChain.shouldCleanEntity(entity, context)) {
                entity.remove();
                cleaned++;
            }
        }
        
        return cleaned;
    }
    
    /**
     * 创建实体检查上下文
     * @param entity 实体
     * @return 检查上下文
     */
    private EntityCheckContext createContext(Entity entity) {
        World world = entity.getWorld();
        Chunk chunk = entity.getLocation().getChunk();
        
        EntityCheckContext context = new EntityCheckContext(config, world, chunk);
        
        // 设置附近玩家
        List<Player> nearbyPlayers = getNearbyPlayers(entity, 32.0);
        context.setNearbyPlayers(nearbyPlayers);
        
        // 设置当前时间
        context.setCurrentTime(System.currentTimeMillis());
        
        return context;
    }
    
    /**
     * 创建带统计信息的实体检查上下文
     * @param entity 实体
     * @param stats 统计信息
     * @return 检查上下文
     */
    private EntityCheckContext createContextWithStats(Entity entity, CleanupStats stats) {
        EntityCheckContext context = createContext(entity);
        context.setStats(stats);
        return context;
    }
    
    /**
     * 获取实体附近的玩家
     * @param entity 实体
     * @param radius 搜索半径
     * @return 附近的玩家列表
     */
    private List<Player> getNearbyPlayers(Entity entity, double radius) {
        List<Player> nearbyPlayers = new ArrayList<>();
        
        try {
            for (Entity nearbyEntity : entity.getNearbyEntities(radius, radius, radius)) {
                if (nearbyEntity instanceof Player) {
                    nearbyPlayers.add((Player) nearbyEntity);
                }
            }
        } catch (Exception e) {
            // 忽略获取附近实体时的异常
        }
        
        return nearbyPlayers;
    }
    
    /**
     * 重新构建责任链（当配置改变时调用）
     */
    public void rebuildChain() {
        checkChain.rebuild();
    }
    
    /**
     * 获取责任链信息
     * @return 处理器信息列表
     */
    public List<String> getChainInfo() {
        return checkChain.getHandlerInfo();
    }
    
    /**
     * 添加自定义处理器
     * @param handler 处理器
     */
    public void addCustomHandler(com.xiaoxiao.arissweeping.chain.EntityCheckHandler handler) {
        checkChain.addHandler(handler);
    }
    
    /**
     * 移除处理器
     * @param handlerName 处理器名称
     * @return 是否成功移除
     */
    public boolean removeHandler(String handlerName) {
        return checkChain.removeHandler(handlerName);
    }
}