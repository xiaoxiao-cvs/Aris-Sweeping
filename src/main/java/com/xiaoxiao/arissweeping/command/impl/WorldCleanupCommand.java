package com.xiaoxiao.arissweeping.command.impl;

import com.xiaoxiao.arissweeping.command.Command;
import com.xiaoxiao.arissweeping.strategy.StrategyManager;
import com.xiaoxiao.arissweeping.observer.CleanupEventManager;
import com.xiaoxiao.arissweeping.observer.CleanupEvent;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 世界清理命令
 * 对指定世界执行清理操作
 */
public class WorldCleanupCommand implements Command {
    
    private static final Logger logger = Logger.getLogger(WorldCleanupCommand.class.getName());
    
    private final World world;
    private final StrategyManager strategyManager;
    private final CleanupEventManager eventManager;
    private final Map<String, Object> parameters;
    private final boolean dryRun;
    private final Set<String> enabledStrategies;
    
    // 撤销相关
    private List<Entity> removedEntities;
    private boolean executed = false;
    
    public WorldCleanupCommand(World world, StrategyManager strategyManager, 
                             CleanupEventManager eventManager) {
        this(world, strategyManager, eventManager, false, null);
    }
    
    public WorldCleanupCommand(World world, StrategyManager strategyManager, 
                             CleanupEventManager eventManager, boolean dryRun, 
                             Set<String> enabledStrategies) {
        this(world, strategyManager, eventManager, dryRun, enabledStrategies, null);
    }
    
    public WorldCleanupCommand(World world, StrategyManager strategyManager, 
                             CleanupEventManager eventManager, boolean dryRun, 
                             Set<String> enabledStrategies, List<Entity> entities) {
        this.world = world;
        this.strategyManager = strategyManager;
        this.eventManager = eventManager;
        this.dryRun = dryRun;
        this.enabledStrategies = enabledStrategies;
        this.parameters = new HashMap<>();
        this.removedEntities = new ArrayList<>();
        
        // 设置参数
        parameters.put("world", world.getName());
        parameters.put("dryRun", dryRun);
        parameters.put("enabledStrategies", enabledStrategies);
        if (entities != null) {
            parameters.put("entities", entities);
        }
    }
    
    @Override
    public CommandResult execute() {
        if (executed) {
            return CommandResult.failure("Command has already been executed");
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 发布清理开始事件
            List<Entity> allEntities = world.getEntities();
            eventManager.publishEvent(new CleanupEvent.CleanupStartedEvent(
                getCommandName(), world, allEntities.size(), dryRun ? "DRY_RUN" : "NORMAL"));
            
            Map<String, Integer> entityTypeStats = new HashMap<>();
            int processedEntities = 0;
            int removedEntities = 0;
            
            if (dryRun) {
                // 干运行模式 - 只统计不删除
                for (Entity entity : allEntities) {
                    processedEntities++;
                    
                    if (strategyManager.shouldCleanupEntity(entity, enabledStrategies)) {
                        removedEntities++;
                        String entityType = entity.getType().name();
                        entityTypeStats.merge(entityType, 1, Integer::sum);
                    }
                }
            } else {
                // 正常清理模式
                List<Entity> toRemove = new ArrayList<>();
                
                for (Entity entity : allEntities) {
                    processedEntities++;
                    
                    if (strategyManager.shouldCleanupEntity(entity, enabledStrategies)) {
                        toRemove.add(entity);
                        String entityType = entity.getType().name();
                        entityTypeStats.merge(entityType, 1, Integer::sum);
                    }
                }
                
                // 执行删除
                for (Entity entity : toRemove) {
                    try {
                        // 记录被删除的实体（用于撤销）
                        this.removedEntities.add(entity);
                        
                        // 发布实体移除事件
                        eventManager.publishEvent(new CleanupEvent.EntityRemovedEvent(
                            getCommandName(), entity, "Strategy cleanup", "WorldCleanup"));
                        
                        entity.remove();
                        removedEntities++;
                        
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Failed to remove entity: " + entity.getType(), e);
                    }
                }
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 发布清理完成事件
            eventManager.publishEvent(new CleanupEvent.CleanupCompletedEvent(
                getCommandName(), world, processedEntities, removedEntities, 
                executionTime, entityTypeStats));
            
            executed = true;
            
            // 准备结果数据
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("processedEntities", processedEntities);
            resultData.put("removedEntities", removedEntities);
            resultData.put("entityTypeStats", entityTypeStats);
            resultData.put("world", world.getName());
            resultData.put("dryRun", dryRun);
            
            String message = String.format(
                "World cleanup %s: processed %d entities, %s %d entities in %d ms",
                dryRun ? "simulation" : "completed",
                processedEntities,
                dryRun ? "would remove" : "removed",
                removedEntities,
                executionTime
            );
            
            return CommandResult.success(message, resultData, executionTime);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 发布清理失败事件
            eventManager.publishEvent(new CleanupEvent.CleanupFailedEvent(
                getCommandName(), world, e.getMessage(), e, 0));
            
            logger.log(Level.SEVERE, "World cleanup failed for world: " + world.getName(), e);
            return CommandResult.failure("World cleanup failed: " + e.getMessage(), e, executionTime);
        }
    }
    
    @Override
    public CommandResult undo() {
        if (!executed) {
            return CommandResult.failure("Command has not been executed yet");
        }
        
        if (dryRun) {
            return CommandResult.failure("Cannot undo a dry run command");
        }
        
        if (removedEntities.isEmpty()) {
            return CommandResult.success("No entities to restore");
        }
        
        // 注意：在Minecraft中，一旦实体被移除，通常无法恢复
        // 这里只是演示撤销的概念，实际实现可能需要不同的策略
        return CommandResult.failure("Entity restoration is not supported in Minecraft");
    }
    
    @Override
    public boolean isUndoable() {
        // 在当前实现中，实体删除无法撤销
        return false;
    }
    
    @Override
    public String getCommandName() {
        return "WorldCleanup";
    }
    
    @Override
    public String getDescription() {
        return String.format("Clean up entities in world '%s'%s", 
            world.getName(), dryRun ? " (dry run)" : "");
    }
    
    @Override
    public Map<String, Object> getParameters() {
        return new HashMap<>(parameters);
    }
    
    @Override
    public ValidationResult validate() {
        Map<String, String> errors = new HashMap<>();
        
        if (world == null) {
            errors.put("world", "World cannot be null");
        }
        
        if (strategyManager == null) {
            errors.put("strategyManager", "StrategyManager cannot be null");
        }
        
        if (eventManager == null) {
            errors.put("eventManager", "EventManager cannot be null");
        }
        
        if (!errors.isEmpty()) {
            return ValidationResult.invalid("Validation failed", errors);
        }
        
        return ValidationResult.valid();
    }
    
    @Override
    public long getEstimatedExecutionTime() {
        // 基于世界中实体数量估算执行时间
        int entityCount = world.getEntities().size();
        return Math.max(100, entityCount / 10); // 每10个实体大约1毫秒
    }
    
    @Override
    public int getPriority() {
        return 50; // 中等优先级
    }
    
    @Override
    public boolean requiresAsyncExecution() {
        // 大型世界的清理可能需要异步执行
        return world.getEntities().size() > 1000;
    }
    
    @Override
    public CommandType getCommandType() {
        return CommandType.CLEANUP;
    }
    
    // 构建器模式
    public static class Builder {
        private World world;
        private StrategyManager strategyManager;
        private CleanupEventManager eventManager;
        private boolean dryRun = false;
        private Set<String> enabledStrategies;
        private List<Entity> entities;
        
        public Builder world(World world) {
            this.world = world;
            return this;
        }
        
        public Builder strategyManager(StrategyManager strategyManager) {
            this.strategyManager = strategyManager;
            return this;
        }
        
        public Builder eventManager(CleanupEventManager eventManager) {
            this.eventManager = eventManager;
            return this;
        }
        
        public Builder dryRun(boolean dryRun) {
            this.dryRun = dryRun;
            return this;
        }
        
        public Builder enabledStrategies(Set<String> enabledStrategies) {
            this.enabledStrategies = enabledStrategies;
            return this;
        }
        
        public Builder entities(List<Entity> entities) {
            this.entities = entities;
            return this;
        }
        
        public WorldCleanupCommand build() {
            return new WorldCleanupCommand(world, strategyManager, eventManager, dryRun, enabledStrategies, entities);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}