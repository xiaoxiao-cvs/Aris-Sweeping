package com.xiaoxiao.arissweeping.command.impl;

import com.xiaoxiao.arissweeping.command.Command;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 实用工具命令集合
 * 包含延迟命令、重试命令和条件命令
 */
public class UtilityCommands {

    /**
     * 延迟命令
     * 在指定延迟后执行命令
     */
    public static class DelayedCommand implements Command {
    
    private static final Logger logger = Logger.getLogger(DelayedCommand.class.getName());
    
    private final Command wrappedCommand;
    private final long delayMillis;
    private final Map<String, Object> parameters;
    
    private boolean executed = false;
    private long executionStartTime;
    
    public DelayedCommand(Command wrappedCommand, long delayMillis) {
        this.wrappedCommand = wrappedCommand;
        this.delayMillis = delayMillis;
        this.parameters = new HashMap<>();
        
        parameters.put("wrappedCommand", wrappedCommand.getCommandName());
        parameters.put("delayMillis", delayMillis);
    }
    
    @Override
    public CommandResult execute() {
        if (executed) {
            return CommandResult.failure("Delayed command has already been executed");
        }
        
        executionStartTime = System.currentTimeMillis();
        
        try {
            // 延迟执行
            if (delayMillis > 0) {
                logger.info(String.format("Delaying command '%s' for %d ms", 
                    wrappedCommand.getCommandName(), delayMillis));
                Thread.sleep(delayMillis);
            }
            
            // 执行包装的命令
            CommandResult result = wrappedCommand.execute();
            executed = true;
            
            long totalTime = System.currentTimeMillis() - executionStartTime;
            
            // 更新结果数据
            Map<String, Object> resultData = new HashMap<>();
            if (result.getData() != null) {
                resultData.putAll(result.getData());
            }
            resultData.put("delayMillis", delayMillis);
            resultData.put("totalExecutionTime", totalTime);
            resultData.put("actualCommandTime", result.getExecutionTime());
            
            if (result.isSuccess()) {
                return CommandResult.success(result.getMessage(), resultData, totalTime);
            } else {
                return CommandResult.failure(result.getMessage(), result.getException(), totalTime);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long executionTime = System.currentTimeMillis() - executionStartTime;
            return CommandResult.failure("Delayed command was interrupted", e, executionTime);
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - executionStartTime;
            return CommandResult.failure("Delayed command execution failed: " + e.getMessage(), e, executionTime);
        }
    }
    
    @Override
    public CommandResult undo() {
        if (!executed) {
            return CommandResult.failure("Delayed command has not been executed yet");
        }
        return wrappedCommand.undo();
    }
    
    @Override
    public boolean isUndoable() {
        return wrappedCommand.isUndoable();
    }
    
    @Override
    public String getCommandName() {
        return "Delayed[" + wrappedCommand.getCommandName() + "]";
    }
    
    @Override
    public String getDescription() {
        return String.format("Execute '%s' after %d ms delay", 
            wrappedCommand.getDescription(), delayMillis);
    }
    
    @Override
    public Map<String, Object> getParameters() {
        return new HashMap<>(parameters);
    }
    
    @Override
    public ValidationResult validate() {
        Map<String, String> errors = new HashMap<>();
        
        if (wrappedCommand == null) {
            errors.put("wrappedCommand", "Wrapped command cannot be null");
        } else {
            ValidationResult wrappedValidation = wrappedCommand.validate();
            if (!wrappedValidation.isValid()) {
                errors.put("wrappedCommand", "Wrapped command validation failed: " + wrappedValidation.getMessage());
            }
        }
        
        if (delayMillis < 0) {
            errors.put("delayMillis", "Delay cannot be negative");
        }
        
        if (!errors.isEmpty()) {
            return ValidationResult.invalid("Delayed command validation failed", errors);
        }
        
        return ValidationResult.valid();
    }
    
    @Override
    public long getEstimatedExecutionTime() {
        return delayMillis + wrappedCommand.getEstimatedExecutionTime();
    }
    
    @Override
    public int getPriority() {
        return wrappedCommand.getPriority();
    }
    
    @Override
    public boolean requiresAsyncExecution() {
        return delayMillis > 1000 || wrappedCommand.requiresAsyncExecution();
    }
    
    @Override
    public CommandType getCommandType() {
        return CommandType.UTILITY;
    }
}

/**
     * 重试命令
     * 在失败时重试执行命令
     */
    public static class RetryCommand implements Command {
    
    private static final Logger logger = Logger.getLogger(RetryCommand.class.getName());
    
    private final Command wrappedCommand;
    private final int maxRetries;
    private final long retryDelayMillis;
    private final Map<String, Object> parameters;
    
    private boolean executed = false;
    private int attemptCount = 0;
    private final List<CommandResult> attemptResults = new ArrayList<>();
    
    public RetryCommand(Command wrappedCommand, int maxRetries, long retryDelayMillis) {
        this.wrappedCommand = wrappedCommand;
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
        this.parameters = new HashMap<>();
        
        parameters.put("wrappedCommand", wrappedCommand.getCommandName());
        parameters.put("maxRetries", maxRetries);
        parameters.put("retryDelayMillis", retryDelayMillis);
    }
    
    @Override
    public CommandResult execute() {
        if (executed) {
            return CommandResult.failure("Retry command has already been executed");
        }
        
        long startTime = System.currentTimeMillis();
        attemptResults.clear();
        
        for (attemptCount = 1; attemptCount <= maxRetries + 1; attemptCount++) {
            try {
                if (attemptCount > 1) {
                    logger.info(String.format("Retrying command '%s' (attempt %d/%d)", 
                        wrappedCommand.getCommandName(), attemptCount, maxRetries + 1));
                    
                    if (retryDelayMillis > 0) {
                        Thread.sleep(retryDelayMillis);
                    }
                }
                
                CommandResult result = wrappedCommand.execute();
                attemptResults.add(result);
                
                if (result.isSuccess()) {
                    executed = true;
                    long totalTime = System.currentTimeMillis() - startTime;
                    
                    // 准备结果数据
                    Map<String, Object> resultData = new HashMap<>();
                    if (result.getData() != null) {
                        resultData.putAll(result.getData());
                    }
                    resultData.put("attemptCount", attemptCount);
                    resultData.put("maxRetries", maxRetries);
                    resultData.put("totalExecutionTime", totalTime);
                    resultData.put("attemptResults", new ArrayList<>(attemptResults));
                    
                    String message = attemptCount == 1 ? 
                        result.getMessage() : 
                        String.format("%s (succeeded on attempt %d)", result.getMessage(), attemptCount);
                    
                    logger.info(String.format("Command '%s' succeeded on attempt %d", 
                        wrappedCommand.getCommandName(), attemptCount));
                    
                    return CommandResult.success(message, resultData, totalTime);
                }
                
                // 如果这是最后一次尝试，返回失败结果
                if (attemptCount >= maxRetries + 1) {
                    break;
                }
                
                logger.warning(String.format("Command '%s' failed on attempt %d: %s", 
                    wrappedCommand.getCommandName(), attemptCount, result.getMessage()));
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                long executionTime = System.currentTimeMillis() - startTime;
                return CommandResult.failure("Retry command was interrupted", e, executionTime);
            } catch (Exception e) {
                CommandResult errorResult = CommandResult.failure(
                    "Exception during attempt " + attemptCount + ": " + e.getMessage(), e);
                attemptResults.add(errorResult);
                
                if (attemptCount >= maxRetries + 1) {
                    break;
                }
                
                logger.log(Level.WARNING, String.format("Command '%s' threw exception on attempt %d", 
                    wrappedCommand.getCommandName(), attemptCount), e);
            }
        }
        
        // 所有重试都失败了
        executed = true;
        long totalTime = System.currentTimeMillis() - startTime;
        
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("attemptCount", attemptCount - 1);
        resultData.put("maxRetries", maxRetries);
        resultData.put("totalExecutionTime", totalTime);
        resultData.put("attemptResults", new ArrayList<>(attemptResults));
        
        String message = String.format("Command '%s' failed after %d attempts", 
            wrappedCommand.getCommandName(), attemptCount - 1);
        
        logger.severe(message);
        return CommandResult.failure(message, (Throwable)null, totalTime);
    }
    
    @Override
    public CommandResult undo() {
        if (!executed) {
            return CommandResult.failure("Retry command has not been executed yet");
        }
        return wrappedCommand.undo();
    }
    
    @Override
    public boolean isUndoable() {
        return wrappedCommand.isUndoable();
    }
    
    @Override
    public String getCommandName() {
        return "Retry[" + wrappedCommand.getCommandName() + "]";
    }
    
    @Override
    public String getDescription() {
        return String.format("Execute '%s' with up to %d retries", 
            wrappedCommand.getDescription(), maxRetries);
    }
    
    @Override
    public Map<String, Object> getParameters() {
        return new HashMap<>(parameters);
    }
    
    @Override
    public ValidationResult validate() {
        Map<String, String> errors = new HashMap<>();
        
        if (wrappedCommand == null) {
            errors.put("wrappedCommand", "Wrapped command cannot be null");
        } else {
            ValidationResult wrappedValidation = wrappedCommand.validate();
            if (!wrappedValidation.isValid()) {
                errors.put("wrappedCommand", "Wrapped command validation failed: " + wrappedValidation.getMessage());
            }
        }
        
        if (maxRetries < 0) {
            errors.put("maxRetries", "Max retries cannot be negative");
        }
        
        if (retryDelayMillis < 0) {
            errors.put("retryDelayMillis", "Retry delay cannot be negative");
        }
        
        if (!errors.isEmpty()) {
            return ValidationResult.invalid("Retry command validation failed", errors);
        }
        
        return ValidationResult.valid();
    }
    
    @Override
    public long getEstimatedExecutionTime() {
        // 估算最坏情况下的执行时间
        return (maxRetries + 1) * (wrappedCommand.getEstimatedExecutionTime() + retryDelayMillis);
    }
    
    @Override
    public int getPriority() {
        return wrappedCommand.getPriority();
    }
    
    @Override
    public boolean requiresAsyncExecution() {
        return maxRetries > 0 || wrappedCommand.requiresAsyncExecution();
    }
    
    @Override
    public CommandType getCommandType() {
        return CommandType.UTILITY;
    }
    
    /**
     * 获取尝试次数
     */
    public int getAttemptCount() {
        return attemptCount;
    }
    
    /**
     * 获取所有尝试的结果
     */
    public List<CommandResult> getAttemptResults() {
        return new ArrayList<>(attemptResults);
    }
}

    /**
     * 条件命令
     * 只有在满足条件时才执行的命令
     */
    public static class ConditionalCommand implements Command {
    
    private static final Logger logger = Logger.getLogger(ConditionalCommand.class.getName());
    
    private final Command wrappedCommand;
    private final Supplier<Boolean> condition;
    private final String conditionDescription;
    private final Map<String, Object> parameters;
    
    private boolean executed = false;
    private boolean conditionMet = false;
    
    public ConditionalCommand(Command wrappedCommand, Supplier<Boolean> condition) {
        this(wrappedCommand, condition, "Custom condition");
    }
    
    public ConditionalCommand(Command wrappedCommand, Supplier<Boolean> condition, String conditionDescription) {
        this.wrappedCommand = wrappedCommand;
        this.condition = condition;
        this.conditionDescription = conditionDescription;
        this.parameters = new HashMap<>();
        
        parameters.put("wrappedCommand", wrappedCommand.getCommandName());
        parameters.put("conditionDescription", conditionDescription);
    }
    
    @Override
    public CommandResult execute() {
        if (executed) {
            return CommandResult.failure("Conditional command has already been executed");
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 检查条件
            conditionMet = condition.get();
            
            if (!conditionMet) {
                executed = true;
                long executionTime = System.currentTimeMillis() - startTime;
                
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("conditionMet", false);
                resultData.put("conditionDescription", conditionDescription);
                
                String message = String.format("Command '%s' skipped - condition not met: %s", 
                    wrappedCommand.getCommandName(), conditionDescription);
                
                logger.info(message);
                return CommandResult.success(message, resultData, executionTime);
            }
            
            // 条件满足，执行命令
            logger.info(String.format("Condition met for command '%s': %s", 
                wrappedCommand.getCommandName(), conditionDescription));
            
            CommandResult result = wrappedCommand.execute();
            executed = true;
            
            long totalTime = System.currentTimeMillis() - startTime;
            
            // 更新结果数据
            Map<String, Object> resultData = new HashMap<>();
            if (result.getData() != null) {
                resultData.putAll(result.getData());
            }
            resultData.put("conditionMet", true);
            resultData.put("conditionDescription", conditionDescription);
            resultData.put("totalExecutionTime", totalTime);
            resultData.put("actualCommandTime", result.getExecutionTime());
            
            if (result.isSuccess()) {
                return CommandResult.success(result.getMessage(), resultData, totalTime);
            } else {
                return CommandResult.failure(result.getMessage(), result.getException(), totalTime);
            }
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.log(Level.SEVERE, "Failed to evaluate condition or execute conditional command", e);
            return CommandResult.failure("Conditional command execution failed: " + e.getMessage(), e, executionTime);
        }
    }
    
    @Override
    public CommandResult undo() {
        if (!executed) {
            return CommandResult.failure("Conditional command has not been executed yet");
        }
        
        if (!conditionMet) {
            return CommandResult.success("No undo needed - command was not executed due to unmet condition");
        }
        
        return wrappedCommand.undo();
    }
    
    @Override
    public boolean isUndoable() {
        return wrappedCommand.isUndoable();
    }
    
    @Override
    public String getCommandName() {
        return "Conditional[" + wrappedCommand.getCommandName() + "]";
    }
    
    @Override
    public String getDescription() {
        return String.format("Execute '%s' if condition met: %s", 
            wrappedCommand.getDescription(), conditionDescription);
    }
    
    @Override
    public Map<String, Object> getParameters() {
        return new HashMap<>(parameters);
    }
    
    @Override
    public ValidationResult validate() {
        Map<String, String> errors = new HashMap<>();
        
        if (wrappedCommand == null) {
            errors.put("wrappedCommand", "Wrapped command cannot be null");
        } else {
            ValidationResult wrappedValidation = wrappedCommand.validate();
            if (!wrappedValidation.isValid()) {
                errors.put("wrappedCommand", "Wrapped command validation failed: " + wrappedValidation.getMessage());
            }
        }
        
        if (condition == null) {
            errors.put("condition", "Condition supplier cannot be null");
        }
        
        if (!errors.isEmpty()) {
            return ValidationResult.invalid("Conditional command validation failed", errors);
        }
        
        return ValidationResult.valid();
    }
    
    @Override
    public long getEstimatedExecutionTime() {
        // 假设条件检查很快，主要时间在命令执行
        return 10 + wrappedCommand.getEstimatedExecutionTime();
    }
    
    @Override
    public int getPriority() {
        return wrappedCommand.getPriority();
    }
    
    @Override
    public boolean requiresAsyncExecution() {
        return wrappedCommand.requiresAsyncExecution();
    }
    
    @Override
    public CommandType getCommandType() {
        return CommandType.UTILITY;
    }
    
    /**
     * 检查条件是否满足（不执行命令）
     */
    public boolean checkCondition() {
        try {
            return condition.get();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to check condition", e);
            return false;
        }
    }
    
    /**
     * 获取条件描述
     */
    public String getConditionDescription() {
        return conditionDescription;
    }
    }
}