package com.xiaoxiao.arissweeping.command.impl;

import com.xiaoxiao.arissweeping.command.Command;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 复合命令
 * 用于组合多个命令并按顺序或并行执行
 */
public class CompositeCommand implements Command {
    
    private static final Logger logger = Logger.getLogger(CompositeCommand.class.getName());
    
    private final String name;
    private final String description;
    private final List<Command> commands;
    private final Map<String, Object> parameters;
    private final ExecutionMode executionMode;
    
    // 执行结果
    private final List<CommandResult> results = new ArrayList<>();
    private boolean executed = false;
    private long executionStartTime;
    
    public CompositeCommand(String name, String description) {
        this(name, description, ExecutionMode.SEQUENTIAL);
    }
    
    public CompositeCommand(String name, String description, ExecutionMode executionMode) {
        this.name = name;
        this.description = description;
        this.executionMode = executionMode;
        this.commands = new ArrayList<>();
        this.parameters = new HashMap<>();
        
        // 设置参数
        parameters.put("name", name);
        parameters.put("description", description);
        parameters.put("executionMode", executionMode);
    }
    
    /**
     * 添加命令
     */
    public CompositeCommand addCommand(Command command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        commands.add(command);
        parameters.put("commandCount", commands.size());
        return this;
    }
    
    /**
     * 添加多个命令
     */
    public CompositeCommand addCommands(Command... commands) {
        for (Command command : commands) {
            addCommand(command);
        }
        return this;
    }
    
    /**
     * 添加命令列表
     */
    public CompositeCommand addCommands(List<Command> commands) {
        for (Command command : commands) {
            addCommand(command);
        }
        return this;
    }
    
    /**
     * 移除命令
     */
    public boolean removeCommand(Command command) {
        boolean removed = commands.remove(command);
        if (removed) {
            parameters.put("commandCount", commands.size());
        }
        return removed;
    }
    
    /**
     * 清空所有命令
     */
    public void clearCommands() {
        commands.clear();
        parameters.put("commandCount", 0);
    }
    
    @Override
    public CommandResult execute() {
        if (executed) {
            return CommandResult.failure("Composite command has already been executed");
        }
        
        if (commands.isEmpty()) {
            return CommandResult.failure("No commands to execute");
        }
        
        executionStartTime = System.currentTimeMillis();
        results.clear();
        
        try {
            CommandResult result;
            
            switch (executionMode) {
                case SEQUENTIAL:
                    result = executeSequential();
                    break;
                case PARALLEL:
                    result = executeParallel();
                    break;
                case FAIL_FAST:
                    result = executeFailFast();
                    break;
                default:
                    result = CommandResult.failure("Unknown execution mode: " + executionMode);
            }
            
            executed = true;
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - executionStartTime;
            logger.log(Level.SEVERE, "Failed to execute composite command: " + name, e);
            return CommandResult.failure("Composite command execution failed: " + e.getMessage(), e, executionTime);
        }
    }
    
    /**
     * 顺序执行所有命令
     */
    private CommandResult executeSequential() {
        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (int i = 0; i < commands.size(); i++) {
            Command command = commands.get(i);
            
            try {
                logger.info(String.format("Executing command %d/%d: %s", i + 1, commands.size(), command.getCommandName()));
                
                CommandResult result = command.execute();
                results.add(result);
                
                if (result.isSuccess()) {
                    successCount++;
                } else {
                    failureCount++;
                    errors.add(String.format("Command %d (%s): %s", i + 1, command.getCommandName(), result.getMessage()));
                }
                
            } catch (Exception e) {
                failureCount++;
                String error = String.format("Command %d (%s) threw exception: %s", i + 1, command.getCommandName(), e.getMessage());
                errors.add(error);
                results.add(CommandResult.failure(error, e));
                logger.log(Level.WARNING, error, e);
            }
        }
        
        return createCompositeResult(successCount, failureCount, errors);
    }
    
    /**
     * 并行执行所有命令
     */
    private CommandResult executeParallel() {
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(commands.size(), 10));
        List<CompletableFuture<CommandResult>> futures = new ArrayList<>();
        
        try {
            // 提交所有命令
            for (Command command : commands) {
                CompletableFuture<CommandResult> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return command.execute();
                    } catch (Exception e) {
                        return CommandResult.failure("Command execution failed: " + e.getMessage(), e);
                    }
                }, executor);
                futures.add(future);
            }
            
            // 等待所有命令完成
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
            
            allFutures.get(30, TimeUnit.SECONDS); // 30秒超时
            
            // 收集结果
            int successCount = 0;
            int failureCount = 0;
            List<String> errors = new ArrayList<>();
            
            for (int i = 0; i < futures.size(); i++) {
                try {
                    CommandResult result = futures.get(i).get();
                    results.add(result);
                    
                    if (result.isSuccess()) {
                        successCount++;
                    } else {
                        failureCount++;
                        errors.add(String.format("Command %d (%s): %s", i + 1, commands.get(i).getCommandName(), result.getMessage()));
                    }
                } catch (Exception e) {
                    failureCount++;
                    String error = String.format("Command %d (%s) failed: %s", i + 1, commands.get(i).getCommandName(), e.getMessage());
                    errors.add(error);
                    results.add(CommandResult.failure(error, e));
                }
            }
            
            return createCompositeResult(successCount, failureCount, errors);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - executionStartTime;
            return CommandResult.failure("Parallel execution failed: " + e.getMessage(), e, executionTime);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 快速失败执行（遇到第一个失败就停止）
     */
    private CommandResult executeFailFast() {
        int successCount = 0;
        
        for (int i = 0; i < commands.size(); i++) {
            Command command = commands.get(i);
            
            try {
                logger.info(String.format("Executing command %d/%d: %s", i + 1, commands.size(), command.getCommandName()));
                
                CommandResult result = command.execute();
                results.add(result);
                
                if (result.isSuccess()) {
                    successCount++;
                } else {
                    // 快速失败
                    long executionTime = System.currentTimeMillis() - executionStartTime;
                    String message = String.format(
                        "Composite command failed at step %d/%d (%s): %s",
                        i + 1, commands.size(), command.getCommandName(), result.getMessage()
                    );
                    
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("failedAt", i + 1);
                    resultData.put("failedCommand", command.getCommandName());
                    resultData.put("successCount", successCount);
                    resultData.put("totalCommands", commands.size());
                    resultData.put("results", new ArrayList<>(results));
                    
                    return CommandResult.failure(message, resultData, executionTime);
                }
                
            } catch (Exception e) {
                long executionTime = System.currentTimeMillis() - executionStartTime;
                String error = String.format("Command %d (%s) threw exception: %s", i + 1, command.getCommandName(), e.getMessage());
                results.add(CommandResult.failure(error, e));
                
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("failedAt", i + 1);
                resultData.put("failedCommand", command.getCommandName());
                resultData.put("successCount", successCount);
                resultData.put("totalCommands", commands.size());
                resultData.put("results", new ArrayList<>(results));
                
                return CommandResult.failure(error, e, resultData, executionTime);
            }
        }
        
        // 所有命令都成功
        return createCompositeResult(successCount, 0, Collections.emptyList());
    }
    
    /**
     * 创建复合命令结果
     */
    private CommandResult createCompositeResult(int successCount, int failureCount, List<String> errors) {
        long executionTime = System.currentTimeMillis() - executionStartTime;
        
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("totalCommands", commands.size());
        resultData.put("successCount", successCount);
        resultData.put("failureCount", failureCount);
        resultData.put("executionMode", executionMode);
        resultData.put("results", new ArrayList<>(results));
        resultData.put("errors", errors);
        
        String message = String.format(
            "Composite command '%s' completed: %d/%d commands succeeded%s",
            name, successCount, commands.size(),
            failureCount > 0 ? " (" + failureCount + " failed)" : ""
        );
        
        if (failureCount == 0) {
            logger.info(message);
            return CommandResult.success(message, resultData, executionTime);
        } else {
            logger.warning(message + ". Errors: " + String.join("; ", errors));
            return CommandResult.failure(message, null, resultData, executionTime);
        }
    }
    
    @Override
    public CommandResult undo() {
        if (!executed) {
            return CommandResult.failure("Composite command has not been executed yet");
        }
        
        // 反向撤销所有已执行的命令
        int undoCount = 0;
        int undoFailures = 0;
        List<String> undoErrors = new ArrayList<>();
        
        // 从最后一个成功的命令开始反向撤销
        for (int i = results.size() - 1; i >= 0; i--) {
            CommandResult result = results.get(i);
            if (result.isSuccess()) {
                Command command = commands.get(i);
                if (command.isUndoable()) {
                    try {
                        CommandResult undoResult = command.undo();
                        if (undoResult.isSuccess()) {
                            undoCount++;
                        } else {
                            undoFailures++;
                            undoErrors.add(String.format("Failed to undo command %s: %s", 
                                command.getCommandName(), undoResult.getMessage()));
                        }
                    } catch (Exception e) {
                        undoFailures++;
                        undoErrors.add(String.format("Exception during undo of command %s: %s", 
                            command.getCommandName(), e.getMessage()));
                    }
                }
            }
        }
        
        executed = false;
        results.clear();
        
        String message = String.format(
            "Composite command '%s' undo completed: %d commands undone%s",
            name, undoCount, undoFailures > 0 ? " (" + undoFailures + " undo failures)" : ""
        );
        
        Map<String, Object> undoData = new HashMap<>();
        undoData.put("undoCount", undoCount);
        undoData.put("undoFailures", undoFailures);
        undoData.put("undoErrors", undoErrors);
        
        if (undoFailures == 0) {
            return CommandResult.success(message, undoData);
        } else {
            return CommandResult.failure(message, undoData, System.currentTimeMillis() - executionStartTime);
        }
    }
    
    @Override
    public boolean isUndoable() {
        // 如果至少有一个命令可撤销，则复合命令可撤销
        return commands.stream().anyMatch(Command::isUndoable);
    }
    
    @Override
    public String getCommandName() {
        return name;
    }
    
    @Override
    public String getDescription() {
        return description + " (" + commands.size() + " commands, " + executionMode + " mode)";
    }
    
    @Override
    public Map<String, Object> getParameters() {
        return new HashMap<>(parameters);
    }
    
    @Override
    public ValidationResult validate() {
        Map<String, String> errors = new HashMap<>();
        
        if (name == null || name.trim().isEmpty()) {
            errors.put("name", "Composite command name cannot be null or empty");
        }
        
        if (commands.isEmpty()) {
            errors.put("commands", "Composite command must contain at least one command");
        }
        
        // 验证所有子命令
        for (int i = 0; i < commands.size(); i++) {
            Command command = commands.get(i);
            if (command == null) {
                errors.put("command_" + i, "Command at index " + i + " is null");
            } else {
                ValidationResult validation = command.validate();
                if (!validation.isValid()) {
                    errors.put("command_" + i + "_" + command.getCommandName(), 
                        "Command validation failed: " + validation.getErrorMessage());
                }
            }
        }
        
        if (!errors.isEmpty()) {
            return ValidationResult.invalid("Composite command validation failed", errors);
        }
        
        return ValidationResult.valid();
    }
    
    @Override
    public long getEstimatedExecutionTime() {
        if (executionMode == ExecutionMode.PARALLEL) {
            // 并行执行时间约等于最长的命令执行时间
            return commands.stream()
                .mapToLong(Command::getEstimatedExecutionTime)
                .max()
                .orElse(0);
        } else {
            // 顺序执行时间等于所有命令执行时间之和
            return commands.stream()
                .mapToLong(Command::getEstimatedExecutionTime)
                .sum();
        }
    }
    
    @Override
    public int getPriority() {
        // 使用最高优先级的子命令优先级
        return commands.stream()
            .mapToInt(Command::getPriority)
            .max()
            .orElse(10);
    }
    
    @Override
    public boolean requiresAsyncExecution() {
        // 如果有任何子命令需要异步执行，或者是并行模式，则需要异步执行
        return executionMode == ExecutionMode.PARALLEL || 
               commands.stream().anyMatch(Command::requiresAsyncExecution);
    }
    
    @Override
    public CommandType getCommandType() {
        return CommandType.COMPOSITE;
    }
    
    /**
     * 获取子命令列表
     */
    public List<Command> getCommands() {
        return new ArrayList<>(commands);
    }
    
    /**
     * 获取执行结果列表
     */
    public List<CommandResult> getResults() {
        return new ArrayList<>(results);
    }
    
    /**
     * 获取执行模式
     */
    public ExecutionMode getExecutionMode() {
        return executionMode;
    }
    
    /**
     * 执行模式枚举
     */
    public enum ExecutionMode {
        SEQUENTIAL,  // 顺序执行
        PARALLEL,    // 并行执行
        FAIL_FAST    // 快速失败
    }
}