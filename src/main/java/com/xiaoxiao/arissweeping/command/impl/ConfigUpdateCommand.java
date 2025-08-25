package com.xiaoxiao.arissweeping.command.impl;

import com.xiaoxiao.arissweeping.command.Command;
import com.xiaoxiao.arissweeping.observer.CleanupEventManager;
import com.xiaoxiao.arissweeping.observer.CleanupEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 配置更新命令
 * 用于更新插件配置
 */
public class ConfigUpdateCommand implements Command {
    
    private static final Logger logger = Logger.getLogger(ConfigUpdateCommand.class.getName());
    
    private final Plugin plugin;
    private final CleanupEventManager eventManager;
    private final String configPath;
    private final Object newValue;
    private final Map<String, Object> parameters;
    
    // 撤销相关
    private Object oldValue;
    private boolean executed = false;
    
    public ConfigUpdateCommand(Plugin plugin, CleanupEventManager eventManager, 
                             String configPath, Object newValue) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.configPath = configPath;
        this.newValue = newValue;
        this.parameters = new HashMap<>();
        
        // 设置参数
        parameters.put("configPath", configPath);
        parameters.put("newValue", newValue);
    }
    
    @Override
    public CommandResult execute() {
        if (executed) {
            return CommandResult.failure("Command has already been executed");
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            FileConfiguration config = plugin.getConfig();
            
            // 保存旧值用于撤销
            oldValue = config.get(configPath);
            
            // 准备事件数据
            Map<String, Object> oldValues = new HashMap<>();
            Map<String, Object> newValues = new HashMap<>();
            oldValues.put(configPath, oldValue);
            newValues.put(configPath, newValue);
            
            // 更新配置
            config.set(configPath, newValue);
            plugin.saveConfig();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 发布配置变更事件
            eventManager.publishEvent(new CleanupEvent.ConfigurationChangedEvent(
                getCommandName(), configPath, oldValues, newValues, "Manual update"));
            
            executed = true;
            
            // 准备结果数据
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("configPath", configPath);
            resultData.put("oldValue", oldValue);
            resultData.put("newValue", newValue);
            
            String message = String.format(
                "Configuration updated: %s = %s (was: %s)",
                configPath, newValue, oldValue
            );
            
            logger.info(message);
            return CommandResult.success(message, resultData, executionTime);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.log(Level.SEVERE, "Failed to update configuration: " + configPath, e);
            return CommandResult.failure("Configuration update failed: " + e.getMessage(), e, executionTime);
        }
    }
    
    @Override
    public CommandResult undo() {
        if (!executed) {
            return CommandResult.failure("Command has not been executed yet");
        }
        
        try {
            FileConfiguration config = plugin.getConfig();
            
            // 恢复旧值
            if (oldValue != null) {
                config.set(configPath, oldValue);
            } else {
                // 如果旧值为null，则移除配置项
                config.set(configPath, null);
            }
            
            plugin.saveConfig();
            
            // 准备事件数据
            Map<String, Object> oldValues = new HashMap<>();
            Map<String, Object> newValues = new HashMap<>();
            oldValues.put(configPath, newValue);
            newValues.put(configPath, oldValue);
            
            // 发布配置变更事件
            eventManager.publishEvent(new CleanupEvent.ConfigurationChangedEvent(
                getCommandName() + "-Undo", configPath, oldValues, newValues, "Undo operation"));
            
            executed = false;
            
            String message = String.format(
                "Configuration reverted: %s = %s",
                configPath, oldValue
            );
            
            logger.info(message);
            return CommandResult.success(message);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to undo configuration update: " + configPath, e);
            return CommandResult.failure("Configuration undo failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isUndoable() {
        return true;
    }
    
    @Override
    public String getCommandName() {
        return "ConfigUpdate";
    }
    
    @Override
    public String getDescription() {
        return String.format("Update configuration '%s' to '%s'", configPath, newValue);
    }
    
    @Override
    public Map<String, Object> getParameters() {
        return new HashMap<>(parameters);
    }
    
    @Override
    public ValidationResult validate() {
        Map<String, String> errors = new HashMap<>();
        
        if (plugin == null) {
            errors.put("plugin", "Plugin cannot be null");
        }
        
        if (configPath == null || configPath.trim().isEmpty()) {
            errors.put("configPath", "Config path cannot be null or empty");
        }
        
        if (eventManager == null) {
            errors.put("eventManager", "EventManager cannot be null");
        }
        
        // 验证配置路径格式
        if (configPath != null && !isValidConfigPath(configPath)) {
            errors.put("configPath", "Invalid config path format");
        }
        
        if (!errors.isEmpty()) {
            return ValidationResult.invalid("Validation failed", errors);
        }
        
        return ValidationResult.valid();
    }
    
    @Override
    public long getEstimatedExecutionTime() {
        return 50; // 配置更新通常很快
    }
    
    @Override
    public int getPriority() {
        return 30; // 高优先级
    }
    
    @Override
    public boolean requiresAsyncExecution() {
        return false; // 配置更新应该同步执行
    }
    
    @Override
    public CommandType getCommandType() {
        return CommandType.CONFIGURATION;
    }
    
    /**
     * 验证配置路径格式
     */
    private boolean isValidConfigPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        
        // 基本格式验证
        return path.matches("^[a-zA-Z0-9._-]+$");
    }
    
    // 构建器模式
    public static class Builder {
        private Plugin plugin;
        private CleanupEventManager eventManager;
        private String configPath;
        private Object newValue;
        
        public Builder plugin(Plugin plugin) {
            this.plugin = plugin;
            return this;
        }
        
        public Builder eventManager(CleanupEventManager eventManager) {
            this.eventManager = eventManager;
            return this;
        }
        
        public Builder configPath(String configPath) {
            this.configPath = configPath;
            return this;
        }
        
        public Builder newValue(Object newValue) {
            this.newValue = newValue;
            return this;
        }
        
        public ConfigUpdateCommand build() {
            return new ConfigUpdateCommand(plugin, eventManager, configPath, newValue);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}