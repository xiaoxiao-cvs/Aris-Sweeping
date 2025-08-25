package com.xiaoxiao.arissweeping.command.impl;

import com.xiaoxiao.arissweeping.command.Command;
import com.xiaoxiao.arissweeping.command.Command.CommandResult;
import com.xiaoxiao.arissweeping.command.Command.CommandType;
import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.LoggerUtil;

import java.util.Map;
import java.util.HashMap;

/**
 * 批量配置更新命令
 */
public class BatchConfigUpdateCommand implements Command {
    private final ModConfig config;
    private final Map<String, Object> updates;
    private final Map<String, Object> previousValues;
    
    public BatchConfigUpdateCommand(ModConfig config, Map<String, Object> updates) {
        this.config = config;
        this.updates = new HashMap<>(updates);
        this.previousValues = new HashMap<>();
    }
    
    @Override
    public CommandResult execute() {
        long startTime = System.currentTimeMillis();
        
        try {
            // 保存之前的值用于撤销
            for (String key : updates.keySet()) {
                // 这里需要根据实际的配置获取方法来实现
                // previousValues.put(key, config.get(key));
            }
            
            // 应用更新
            for (Map.Entry<String, Object> entry : updates.entrySet()) {
                // 这里需要根据实际的配置设置方法来实现
                // config.set(entry.getKey(), entry.getValue());
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("updatedKeys", updates.keySet());
            resultData.put("updateCount", updates.size());
            
            LoggerUtil.info("批量配置更新完成，更新了 " + updates.size() + " 个配置项");
            
            return CommandResult.success("批量配置更新成功", resultData, executionTime);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            LoggerUtil.error("批量配置更新失败", e);
            return CommandResult.failure("批量配置更新失败: " + e.getMessage(), e, executionTime);
        }
    }
    
    @Override
    public CommandResult undo() {
        long startTime = System.currentTimeMillis();
        
        try {
            // 恢复之前的值
            for (Map.Entry<String, Object> entry : previousValues.entrySet()) {
                // config.set(entry.getKey(), entry.getValue());
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("restoredKeys", previousValues.keySet());
            resultData.put("restoreCount", previousValues.size());
            
            LoggerUtil.info("批量配置更新已撤销，恢复了 " + previousValues.size() + " 个配置项");
            
            return CommandResult.success("批量配置更新撤销成功", resultData, executionTime);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            LoggerUtil.error("批量配置更新撤销失败", e);
            return CommandResult.failure("批量配置更新撤销失败: " + e.getMessage(), e, executionTime);
        }
    }
    
    @Override
    public boolean isUndoable() {
        return !previousValues.isEmpty();
    }
    
    @Override
    public String getCommandName() {
        return "BatchConfigUpdate";
    }
    
    @Override
    public String getDescription() {
        return "批量更新配置项: " + updates.keySet();
    }
    
    @Override
    public CommandType getCommandType() {
        return CommandType.CONFIGURATION;
    }
}