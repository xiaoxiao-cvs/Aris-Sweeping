package com.xiaoxiao.arissweeping.command.impl;

import com.xiaoxiao.arissweeping.command.Command;
import com.xiaoxiao.arissweeping.strategy.StrategyManager;
import com.xiaoxiao.arissweeping.strategy.CleanupStrategy;
import com.xiaoxiao.arissweeping.observer.CleanupEventManager;
import com.xiaoxiao.arissweeping.observer.CleanupEvent;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 策略执行命令
 * 用于执行特定的清理策略
 */
public class StrategyExecutionCommand implements Command {
    
    private static final Logger logger = Logger.getLogger(StrategyExecutionCommand.class.getName());
    
    private final StrategyManager strategyManager;
    private final CleanupEventManager eventManager;
    private final String strategyName;
    private final World world;
    private final List<Entity> entities;
    private final boolean dryRun;
    private final Map<String, Object> parameters;
    
    // 执行结果
    private CleanupStrategy.CleanupResult result;
    private boolean executed = false;
    private long executionStartTime;
    
    public StrategyExecutionCommand(StrategyManager strategyManager, 
                                  CleanupEventManager eventManager,
                                  String strategyName, 
                                  World world, 
                                  List<Entity> entities,
                                  boolean dryRun) {
        this.strategyManager = strategyManager;
        this.eventManager = eventManager;
        this.strategyName = strategyName;
        this.world = world;
        this.entities = new ArrayList<>(entities);
        this.dryRun = dryRun;
        this.parameters = new HashMap<>();
        
        // 设置参数
        parameters.put("strategyName", strategyName);
        parameters.put("worldName", world.getName());
        parameters.put("entityCount", entities.size());
        parameters.put("dryRun", dryRun);
    }
    
    @Override
    public CommandResult execute() {
        if (executed) {
            return CommandResult.failure("Command has already been executed");
        }
        
        executionStartTime = System.currentTimeMillis();
        
        try {
            // 获取策略
            CleanupStrategy strategy = strategyManager.getStrategy(strategyName);
            if (strategy == null) {
                return CommandResult.failure("Strategy not found: " + strategyName);
            }
            
            if (!strategy.isEnabled()) {
                return CommandResult.failure("Strategy is disabled: " + strategyName);
            }
            
            // 发布策略开始事件
            eventManager.publishEvent(new CleanupEvent.StrategyExecutedEvent(
                getCommandName(), strategyName, world, entities.size(), 0, 0, true));
            
            // 执行策略
            result = strategy.execute(world, entities, dryRun);
            
            long executionTime = System.currentTimeMillis() - executionStartTime;
            executed = true;
            
            // 发布策略完成事件
            eventManager.publishEvent(new CleanupEvent.StrategyExecutedEvent(
                getCommandName(), strategyName, world, 
                result.getProcessedCount(), result.getRemovedCount(), executionTime, true));
            
            // 准备结果数据
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("strategyName", strategyName);
            resultData.put("worldName", world.getName());
            resultData.put("processedCount", result.getProcessedCount());
            resultData.put("removedCount", result.getRemovedCount());
            resultData.put("dryRun", dryRun);
            resultData.put("executionTime", executionTime);
            resultData.put("errors", result.getErrors());
            
            String message = String.format(
                "Strategy '%s' executed in world '%s': processed %d entities, %s %d entities%s",
                strategyName, world.getName(), result.getProcessedCount(),
                dryRun ? "would remove" : "removed", result.getRemovedCount(),
                result.hasErrors() ? " (with errors)" : ""
            );
            
            if (result.hasErrors()) {
                logger.warning(message + ". Errors: " + result.getErrors());
            } else {
                logger.info(message);
            }
            
            return CommandResult.success(message, resultData, executionTime);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - executionStartTime;
            
            // 发布策略失败事件
            eventManager.publishEvent(new CleanupEvent.CleanupFailedEvent(
                getCommandName(), world, "Strategy execution failed: " + e.getMessage(), e, 0));
            
            logger.log(Level.SEVERE, "Failed to execute strategy: " + strategyName, e);
            return CommandResult.failure("Strategy execution failed: " + e.getMessage(), e, executionTime);
        }
    }
    
    @Override
    public CommandResult undo() {
        // 策略执行无法撤销（实体已被移除）
        return CommandResult.failure("Strategy execution cannot be undone - entities have been removed from the world");
    }
    
    @Override
    public boolean isUndoable() {
        return false; // 实体移除无法撤销
    }
    
    @Override
    public String getCommandName() {
        return "StrategyExecution";
    }
    
    @Override
    public String getDescription() {
        return String.format(
            "Execute strategy '%s' on %d entities in world '%s'%s",
            strategyName, entities.size(), world.getName(),
            dryRun ? " (dry run)" : ""
        );
    }
    
    @Override
    public Map<String, Object> getParameters() {
        return new HashMap<>(parameters);
    }
    
    @Override
    public ValidationResult validate() {
        Map<String, String> errors = new HashMap<>();
        
        if (strategyManager == null) {
            errors.put("strategyManager", "StrategyManager cannot be null");
        }
        
        if (eventManager == null) {
            errors.put("eventManager", "EventManager cannot be null");
        }
        
        if (strategyName == null || strategyName.trim().isEmpty()) {
            errors.put("strategyName", "Strategy name cannot be null or empty");
        }
        
        if (world == null) {
            errors.put("world", "World cannot be null");
        }
        
        if (entities == null) {
            errors.put("entities", "Entities list cannot be null");
        }
        
        // 验证策略是否存在且可用
        if (strategyManager != null && strategyName != null) {
            CleanupStrategy strategy = strategyManager.getStrategy(strategyName);
            if (strategy == null) {
                errors.put("strategy", "Strategy not found: " + strategyName);
            } else if (!strategy.isEnabled()) {
                errors.put("strategy", "Strategy is disabled: " + strategyName);
            }
        }
        
        if (!errors.isEmpty()) {
            return ValidationResult.invalid("Validation failed", errors);
        }
        
        return ValidationResult.valid();
    }
    
    @Override
    public long getEstimatedExecutionTime() {
        // 基于实体数量估算执行时间
        int entityCount = entities.size();
        if (entityCount < 100) {
            return 100;
        } else if (entityCount < 1000) {
            return 500;
        } else if (entityCount < 5000) {
            return 2000;
        } else {
            return 5000;
        }
    }
    
    @Override
    public int getPriority() {
        return 20; // 中高优先级
    }
    
    @Override
    public boolean requiresAsyncExecution() {
        return entities.size() > 500; // 大量实体时异步执行
    }
    
    @Override
    public CommandType getCommandType() {
        return CommandType.CLEANUP;
    }
    
    /**
     * 获取执行结果
     */
    public CleanupStrategy.CleanupResult getResult() {
        return result;
    }
    
    // 构建器模式
    public static class Builder {
        private StrategyManager strategyManager;
        private CleanupEventManager eventManager;
        private String strategyName;
        private World world;
        private List<Entity> entities;
        private boolean dryRun = false;
        
        public Builder strategyManager(StrategyManager strategyManager) {
            this.strategyManager = strategyManager;
            return this;
        }
        
        public Builder eventManager(CleanupEventManager eventManager) {
            this.eventManager = eventManager;
            return this;
        }
        
        public Builder strategyName(String strategyName) {
            this.strategyName = strategyName;
            return this;
        }
        
        public Builder world(World world) {
            this.world = world;
            return this;
        }
        
        public Builder entities(List<Entity> entities) {
            this.entities = entities;
            return this;
        }
        
        public Builder dryRun(boolean dryRun) {
            this.dryRun = dryRun;
            return this;
        }
        
        public StrategyExecutionCommand build() {
            return new StrategyExecutionCommand(
                strategyManager, eventManager, strategyName, world, entities, dryRun);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}