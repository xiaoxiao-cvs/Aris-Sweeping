package com.xiaoxiao.arissweeping.command;

import com.xiaoxiao.arissweeping.command.impl.*;
import com.xiaoxiao.arissweeping.command.impl.BatchConfigUpdateCommand;
import com.xiaoxiao.arissweeping.command.impl.BatchStrategyExecutionCommand;
import com.xiaoxiao.arissweeping.strategy.StrategyManager;
import com.xiaoxiao.arissweeping.strategy.CleanupStrategy;
import com.xiaoxiao.arissweeping.observer.CleanupEventManager;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import com.xiaoxiao.arissweeping.ArisSweeping;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * 命令工厂类
 * 用于创建各种类型的命令实例
 */
public class CommandFactory {
    
    private static final Logger logger = Logger.getLogger(CommandFactory.class.getName());
    
    private final ArisSweeping plugin;
    private final StrategyManager strategyManager;
    private final CleanupEventManager eventManager;
    
    public CommandFactory(ArisSweeping plugin, StrategyManager strategyManager, CleanupEventManager eventManager) {
        this.plugin = plugin;
        this.strategyManager = strategyManager;
        this.eventManager = eventManager;
    }
    
    /**
     * 创建世界清理命令
     */
    public WorldCleanupCommand createWorldCleanupCommand(World world, boolean dryRun) {
        return WorldCleanupCommand.builder()
            .world(world)
            .strategyManager(strategyManager)
            .eventManager(eventManager)
            .dryRun(dryRun)
            .build();
    }
    
    /**
     * 创建世界清理命令（带实体列表）
     */
    public WorldCleanupCommand createWorldCleanupCommand(World world, List<Entity> entities, boolean dryRun) {
        return WorldCleanupCommand.builder()
            .world(world)
            .entities(entities)
            .strategyManager(strategyManager)
            .eventManager(eventManager)
            .dryRun(dryRun)
            .build();
    }
    
    /**
     * 创建配置更新命令
     */
    public ConfigUpdateCommand createConfigUpdateCommand(String configPath, Object newValue) {
        return ConfigUpdateCommand.builder()
            .plugin(plugin)
            .eventManager(eventManager)
            .configPath(configPath)
            .newValue(newValue)
            .build();
    }
    
    /**
     * 创建批量配置更新命令
     */
    public BatchConfigUpdateCommand createBatchConfigUpdateCommand(Map<String, Object> configUpdates) {
        return new BatchConfigUpdateCommand(plugin.getModConfig(), configUpdates);
    }
    
    /**
     * 创建策略执行命令
     */
    public StrategyExecutionCommand createStrategyExecutionCommand(String strategyName, World world, 
                                                                 List<Entity> entities, boolean dryRun) {
        return StrategyExecutionCommand.builder()
            .strategyManager(strategyManager)
            .eventManager(eventManager)
            .strategyName(strategyName)
            .world(world)
            .entities(entities)
            .dryRun(dryRun)
            .build();
    }
    
    /**
     * 创建批量策略执行命令
     */
    public Command createBatchStrategyExecutionCommand(List<String> strategyNames, List<World> worlds, boolean dryRun) {
        // 从策略管理器获取策略实例
        List<CleanupStrategy> strategies = new ArrayList<>();
        for (String strategyName : strategyNames) {
            CleanupStrategy strategy = strategyManager.getStrategy(strategyName);
            if (strategy != null) {
                strategies.add(strategy);
            }
        }
        
        // 目前只支持单个世界，取第一个世界
        World world = worlds.isEmpty() ? null : worlds.get(0);
        
        return new BatchStrategyExecutionCommand(strategies, world);
    }
    
    /**
     * 创建复合命令
     */
    public CompositeCommand createCompositeCommand(String name, String description) {
        return new CompositeCommand(name, description);
    }
    
    /**
     * 创建延迟命令
     */
    public DelayedCommand createDelayedCommand(Command command, long delayMillis) {
        return new DelayedCommand(command, delayMillis, (org.bukkit.plugin.java.JavaPlugin) plugin);
    }
    
    /**
     * 创建重试命令
     */
    public RetryCommand createRetryCommand(Command command, int maxRetries, long retryDelayMillis) {
        return new RetryCommand(command, maxRetries, retryDelayMillis);
    }
    
    /**
     * 创建条件命令
     */
    public ConditionalCommand createConditionalCommand(Command command, java.util.function.Supplier<Boolean> condition) {
        return new ConditionalCommand(command, condition);
    }
    
    /**
     * 根据类型和参数创建命令
     */
    public Command createCommand(Command.CommandType type, Map<String, Object> parameters) {
        switch (type) {
            case CLEANUP:
                return createCleanupCommand(parameters);
            case CONFIGURATION:
                return createConfigurationCommand(parameters);
            case COMPOSITE:
                return createCompositeCommandFromParams(parameters);
            default:
                throw new IllegalArgumentException("Unsupported command type: " + type);
        }
    }
    
    /**
     * 创建清理命令
     */
    private Command createCleanupCommand(Map<String, Object> parameters) {
        World world = (World) parameters.get("world");
        Boolean dryRun = (Boolean) parameters.getOrDefault("dryRun", false);
        
        @SuppressWarnings("unchecked")
        List<Entity> entities = (List<Entity>) parameters.get("entities");
        
        if (entities != null) {
            return createWorldCleanupCommand(world, entities, dryRun);
        } else {
            return createWorldCleanupCommand(world, dryRun);
        }
    }
    
    /**
     * 创建配置命令
     */
    private Command createConfigurationCommand(Map<String, Object> parameters) {
        String configPath = (String) parameters.get("configPath");
        Object newValue = parameters.get("newValue");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> configUpdates = (Map<String, Object>) parameters.get("configUpdates");
        
        if (configUpdates != null) {
            return createBatchConfigUpdateCommand(configUpdates);
        } else {
            return createConfigUpdateCommand(configPath, newValue);
        }
    }
    

    
    /**
     * 从参数创建复合命令
     */
    private Command createCompositeCommandFromParams(Map<String, Object> parameters) {
        String name = (String) parameters.get("name");
        String description = (String) parameters.get("description");
        
        @SuppressWarnings("unchecked")
        List<Command> commands = (List<Command>) parameters.get("commands");
        
        CompositeCommand composite = createCompositeCommand(name, description);
        
        if (commands != null) {
            for (Command command : commands) {
                composite.addCommand(command);
            }
        }
        
        return composite;
    }
    
    /**
     * 验证命令参数
     */
    public boolean validateParameters(Command.CommandType type, Map<String, Object> parameters) {
        switch (type) {
            case CLEANUP:
                return validateCleanupParameters(parameters);
            case CONFIGURATION:
                return validateConfigurationParameters(parameters);
            case COMPOSITE:
                return validateCompositeParameters(parameters);
            default:
                return false;
        }
    }
    
    private boolean validateCleanupParameters(Map<String, Object> parameters) {
        return parameters.containsKey("world") && parameters.get("world") instanceof World;
    }
    
    private boolean validateConfigurationParameters(Map<String, Object> parameters) {
        return (parameters.containsKey("configPath") && parameters.containsKey("newValue")) ||
               parameters.containsKey("configUpdates");
    }
    

    
    private boolean validateCompositeParameters(Map<String, Object> parameters) {
        return parameters.containsKey("name");
    }
    
    /**
     * 获取支持的命令类型
     */
    public Command.CommandType[] getSupportedCommandTypes() {
        return Command.CommandType.values();
    }
    
    /**
     * 创建命令构建器
     */
    public CommandBuilder createBuilder() {
        return new CommandBuilder(this);
    }
    
    /**
     * 命令构建器
     */
    public static class CommandBuilder {
        private final CommandFactory factory;
        private final Map<String, Object> parameters = new HashMap<>();
        private Command.CommandType type;
        
        public CommandBuilder(CommandFactory factory) {
            this.factory = factory;
        }
        
        public CommandBuilder type(Command.CommandType type) {
            this.type = type;
            return this;
        }
        
        public CommandBuilder parameter(String key, Object value) {
            parameters.put(key, value);
            return this;
        }
        
        public CommandBuilder world(World world) {
            return parameter("world", world);
        }
        
        public CommandBuilder entities(List<Entity> entities) {
            return parameter("entities", entities);
        }
        
        public CommandBuilder dryRun(boolean dryRun) {
            return parameter("dryRun", dryRun);
        }
        
        public CommandBuilder strategyName(String strategyName) {
            return parameter("strategyName", strategyName);
        }
        
        public CommandBuilder configPath(String configPath) {
            return parameter("configPath", configPath);
        }
        
        public CommandBuilder newValue(Object newValue) {
            return parameter("newValue", newValue);
        }
        
        public CommandBuilder name(String name) {
            return parameter("name", name);
        }
        
        public CommandBuilder description(String description) {
            return parameter("description", description);
        }
        
        public Command build() {
            if (type == null) {
                throw new IllegalStateException("Command type must be specified");
            }
            
            if (!factory.validateParameters(type, parameters)) {
                throw new IllegalArgumentException("Invalid parameters for command type: " + type);
            }
            
            return factory.createCommand(type, parameters);
        }
    }
    
    // 使用Command.CommandType枚举
}