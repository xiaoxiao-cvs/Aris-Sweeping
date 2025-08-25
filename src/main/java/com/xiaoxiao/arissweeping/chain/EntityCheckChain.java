package com.xiaoxiao.arissweeping.chain;

import com.xiaoxiao.arissweeping.chain.handlers.*;
import com.xiaoxiao.arissweeping.config.ModConfig;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 实体检查责任链管理器
 * 负责组织和管理所有的实体检查处理器
 */
public class EntityCheckChain {
    
    private EntityCheckHandler firstHandler;
    private final List<EntityCheckHandler> handlers;
    private final ModConfig config;
    
    public EntityCheckChain(ModConfig config) {
        this.config = config;
        this.handlers = new ArrayList<>();
        initializeHandlers();
        buildChain();
    }
    
    /**
     * 初始化所有处理器
     */
    private void initializeHandlers() {
        // 添加所有处理器（按逻辑顺序）
        handlers.add(new PlayerProximityCheckHandler());     // 玩家附近保护
        handlers.add(new ItemAgeCheckHandler());             // 物品年龄检查
        handlers.add(new ExperienceOrbAgeCheckHandler());    // 经验球年龄检查
        handlers.add(new ArrowAgeCheckHandler());            // 箭矢年龄检查
        handlers.add(new EntityDensityCheckHandler());       // 密度检查（最后执行）
        
        // 可以根据配置动态添加更多处理器
        // if (config.isAdvancedCleanupEnabled()) {
        //     handlers.add(new AdvancedCleanupHandler());
        // }
    }
    
    /**
     * 构建责任链
     */
    private void buildChain() {
        if (handlers.isEmpty()) {
            return;
        }
        
        // 按优先级排序（数值越小优先级越高）
        handlers.sort(Comparator.comparingInt(EntityCheckHandler::getPriority));
        
        // 构建链式结构
        for (int i = 0; i < handlers.size() - 1; i++) {
            handlers.get(i).setNextHandler(handlers.get(i + 1));
        }
        
        firstHandler = handlers.get(0);
    }
    
    /**
     * 检查实体是否应该被清理
     * @param entity 要检查的实体
     * @param context 检查上下文
     * @return 是否应该清理
     */
    public boolean shouldCleanEntity(Entity entity, EntityCheckContext context) {
        if (firstHandler == null) {
            return false;
        }
        
        EntityCheckHandler.CheckResult result = firstHandler.checkEntity(entity, context);
        return result == EntityCheckHandler.CheckResult.SHOULD_CLEAN;
    }
    
    /**
     * 添加自定义处理器
     * @param handler 处理器
     */
    public void addHandler(EntityCheckHandler handler) {
        handlers.add(handler);
        buildChain(); // 重新构建链
    }
    
    /**
     * 移除处理器
     * @param handlerName 处理器名称
     * @return 是否成功移除
     */
    public boolean removeHandler(String handlerName) {
        boolean removed = handlers.removeIf(handler -> handler.getHandlerName().equals(handlerName));
        if (removed) {
            buildChain(); // 重新构建链
        }
        return removed;
    }
    
    /**
     * 获取所有处理器的信息
     * @return 处理器信息列表
     */
    public List<String> getHandlerInfo() {
        List<String> info = new ArrayList<>();
        for (EntityCheckHandler handler : handlers) {
            info.add(handler.toString());
        }
        return info;
    }
    
    /**
     * 重新构建责任链（当配置改变时调用）
     */
    public void rebuild() {
        handlers.clear();
        initializeHandlers();
        buildChain();
    }
    
    /**
     * 获取处理器数量
     * @return 处理器数量
     */
    public int getHandlerCount() {
        return handlers.size();
    }
    
    /**
     * 检查责任链是否为空
     * @return 是否为空
     */
    public boolean isEmpty() {
        return handlers.isEmpty();
    }
}