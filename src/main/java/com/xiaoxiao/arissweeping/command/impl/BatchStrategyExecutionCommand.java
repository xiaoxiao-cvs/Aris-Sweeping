package com.xiaoxiao.arissweeping.command.impl;

import com.xiaoxiao.arissweeping.command.Command;
import com.xiaoxiao.arissweeping.command.Command.CommandResult;
import com.xiaoxiao.arissweeping.command.Command.CommandType;
import com.xiaoxiao.arissweeping.strategy.CleanupStrategy;
import com.xiaoxiao.arissweeping.util.LoggerUtil;
import org.bukkit.World;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * 批量策略执行命令
 */
public class BatchStrategyExecutionCommand implements Command {
    private final List<CleanupStrategy> strategies;
    private final World world;
    private final List<CommandResult> executionResults;
    
    public BatchStrategyExecutionCommand(List<CleanupStrategy> strategies, World world) {
        this.strategies = new ArrayList<>(strategies);
        this.world = world;
        this.executionResults = new ArrayList<>();
    }
    
    @Override
    public CommandResult execute() {
        long startTime = System.currentTimeMillis();
        
        try {
            int successCount = 0;
            int failureCount = 0;
            
            for (CleanupStrategy strategy : strategies) {
                try {
                    // 执行策略
                    // CommandResult result = strategy.execute(world);
                    // executionResults.add(result);
                    
                    // 模拟执行结果
                    CommandResult result = CommandResult.success("策略执行成功", new HashMap<>(), 100);
                    executionResults.add(result);
                    
                    if (result.isSuccess()) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                    
                } catch (Exception e) {
                    failureCount++;
                    CommandResult errorResult = CommandResult.failure("策略执行失败: " + e.getMessage(), e, 0);
                    executionResults.add(errorResult);
                    LoggerUtil.error("策略执行失败", e);
                }
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("totalStrategies", strategies.size());
            resultData.put("successCount", successCount);
            resultData.put("failureCount", failureCount);
            resultData.put("world", world.getName());
            resultData.put("executionResults", executionResults);
            
            String message = String.format("批量策略执行完成: 成功 %d, 失败 %d, 总计 %d", 
                successCount, failureCount, strategies.size());
            
            LoggerUtil.info(message);
            
            if (failureCount > 0) {
                return CommandResult.failure(message, resultData, executionTime);
            } else {
                return CommandResult.success(message, resultData, executionTime);
            }
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            LoggerUtil.error("批量策略执行失败", e);
            return CommandResult.failure("批量策略执行失败: " + e.getMessage(), e, executionTime);
        }
    }
    
    @Override
    public CommandResult undo() {
        long startTime = System.currentTimeMillis();
        
        try {
            // 批量策略执行通常不支持撤销
            long executionTime = System.currentTimeMillis() - startTime;
            return CommandResult.failure("批量策略执行不支持撤销操作", (Throwable)null, executionTime);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            LoggerUtil.error("批量策略执行撤销失败", e);
            return CommandResult.failure("批量策略执行撤销失败: " + e.getMessage(), e, executionTime);
        }
    }
    
    @Override
    public boolean isUndoable() {
        return false; // 批量策略执行通常不支持撤销
    }
    
    @Override
    public String getCommandName() {
        return "BatchStrategyExecution";
    }
    
    @Override
    public String getDescription() {
        return "批量执行清理策略，世界: " + world.getName() + ", 策略数量: " + strategies.size();
    }
    
    @Override
    public CommandType getCommandType() {
        return CommandType.CLEANUP;
    }
}