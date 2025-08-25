package com.xiaoxiao.arissweeping.command;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 命令调用器
 * 负责执行命令、管理命令历史和撤销操作
 */
public class CommandInvoker {
    
    private static final Logger logger = Logger.getLogger(CommandInvoker.class.getName());
    
    private final Deque<CommandExecution> commandHistory;
    private final ExecutorService commandExecutor;
    private final int maxHistorySize;
    private final boolean enableUndo;
    private final Map<String, CommandStatistics> commandStats;
    private volatile boolean shutdown;
    
    public CommandInvoker() {
        this(true, 100, 5);
    }
    
    public CommandInvoker(boolean enableUndo, int maxHistorySize, int threadPoolSize) {
        this.enableUndo = enableUndo;
        this.maxHistorySize = maxHistorySize;
        this.commandHistory = new ArrayDeque<>();
        this.commandStats = new ConcurrentHashMap<>();
        this.shutdown = false;
        
        // 创建线程池
        this.commandExecutor = Executors.newFixedThreadPool(threadPoolSize, r -> {
            Thread t = new Thread(r, "CommandExecutor");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * 同步执行命令
     */
    public Command.CommandResult execute(Command command) {
        if (command == null) {
            return Command.CommandResult.failure("Command cannot be null");
        }
        
        if (shutdown) {
            return Command.CommandResult.failure("CommandInvoker is shutdown");
        }
        
        // 验证命令
        Command.ValidationResult validation = command.validate();
        if (!validation.isValid()) {
            return Command.CommandResult.failure("Command validation failed: " + validation.getMessage());
        }
        
        long startTime = System.currentTimeMillis();
        Command.CommandResult result;
        
        try {
            logger.info("Executing command: " + command.getCommandName());
            
            // 执行命令
            result = command.execute();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 记录执行历史
            if (enableUndo && command.isUndoable()) {
                recordExecution(command, result, executionTime);
            }
            
            // 更新统计信息
            updateStatistics(command, result, executionTime);
            
            logger.info(String.format("Command '%s' executed in %d ms with result: %s", 
                command.getCommandName(), executionTime, result.isSuccess() ? "SUCCESS" : "FAILURE"));
                
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            result = Command.CommandResult.failure("Command execution failed: " + e.getMessage(), e, executionTime);
            
            updateStatistics(command, result, executionTime);
            
            logger.log(Level.SEVERE, "Error executing command: " + command.getCommandName(), e);
        }
        
        return result;
    }
    
    /**
     * 异步执行命令
     */
    public CompletableFuture<Command.CommandResult> executeAsync(Command command) {
        if (command == null) {
            return CompletableFuture.completedFuture(
                Command.CommandResult.failure("Command cannot be null"));
        }
        
        if (shutdown) {
            return CompletableFuture.completedFuture(
                Command.CommandResult.failure("CommandInvoker is shutdown"));
        }
        
        return CompletableFuture.supplyAsync(() -> execute(command), commandExecutor);
    }
    
    /**
     * 批量执行命令
     */
    public List<Command.CommandResult> executeBatch(List<Command> commands) {
        if (commands == null || commands.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Command.CommandResult> results = new ArrayList<>();
        
        // 按优先级排序
        List<Command> sortedCommands = new ArrayList<>(commands);
        sortedCommands.sort(Comparator.comparingInt(Command::getPriority));
        
        for (Command command : sortedCommands) {
            Command.CommandResult result = execute(command);
            results.add(result);
            
            // 如果命令失败且是关键命令，停止执行后续命令
            if (!result.isSuccess() && command.getPriority() < 50) {
                logger.warning("Critical command failed, stopping batch execution: " + command.getCommandName());
                break;
            }
        }
        
        return results;
    }
    
    /**
     * 异步批量执行命令
     */
    public CompletableFuture<List<Command.CommandResult>> executeBatchAsync(List<Command> commands) {
        if (commands == null || commands.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        
        return CompletableFuture.supplyAsync(() -> executeBatch(commands), commandExecutor);
    }
    
    /**
     * 撤销最后一个命令
     */
    public Command.CommandResult undoLast() {
        if (!enableUndo) {
            return Command.CommandResult.failure("Undo is not enabled");
        }
        
        synchronized (commandHistory) {
            if (commandHistory.isEmpty()) {
                return Command.CommandResult.failure("No commands to undo");
            }
            
            CommandExecution lastExecution = commandHistory.removeLast();
            
            try {
                logger.info("Undoing command: " + lastExecution.getCommand().getCommandName());
                
                Command.CommandResult undoResult = lastExecution.getCommand().undo();
                
                if (undoResult.isSuccess()) {
                    logger.info("Successfully undid command: " + lastExecution.getCommand().getCommandName());
                } else {
                    logger.warning("Failed to undo command: " + lastExecution.getCommand().getCommandName() + 
                        ", reason: " + undoResult.getMessage());
                }
                
                return undoResult;
                
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error undoing command: " + lastExecution.getCommand().getCommandName(), e);
                return Command.CommandResult.failure("Undo failed: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * 撤销指定数量的命令
     */
    public List<Command.CommandResult> undoMultiple(int count) {
        List<Command.CommandResult> results = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Command.CommandResult result = undoLast();
            results.add(result);
            
            if (!result.isSuccess()) {
                break;
            }
        }
        
        return results;
    }
    
    /**
     * 获取命令历史
     */
    public List<CommandExecution> getCommandHistory() {
        synchronized (commandHistory) {
            return new ArrayList<>(commandHistory);
        }
    }
    
    /**
     * 获取命令历史（最近的n个）
     */
    public List<CommandExecution> getRecentCommandHistory(int count) {
        synchronized (commandHistory) {
            List<CommandExecution> recent = new ArrayList<>();
            Iterator<CommandExecution> iterator = commandHistory.descendingIterator();
            
            int added = 0;
            while (iterator.hasNext() && added < count) {
                recent.add(iterator.next());
                added++;
            }
            
            return recent;
        }
    }
    
    /**
     * 清除命令历史
     */
    public void clearHistory() {
        synchronized (commandHistory) {
            commandHistory.clear();
        }
        logger.info("Command history cleared");
    }
    
    /**
     * 获取命令统计信息
     */
    public Map<String, CommandStatistics> getCommandStatistics() {
        return new HashMap<>(commandStats);
    }
    
    /**
     * 获取特定命令的统计信息
     */
    public CommandStatistics getCommandStatistics(String commandName) {
        return commandStats.get(commandName);
    }
    
    /**
     * 清除统计信息
     */
    public void clearStatistics() {
        commandStats.clear();
        logger.info("Command statistics cleared");
    }
    
    /**
     * 检查是否可以撤销
     */
    public boolean canUndo() {
        return enableUndo && !commandHistory.isEmpty();
    }
    
    /**
     * 获取可撤销的命令数量
     */
    public int getUndoableCommandCount() {
        return enableUndo ? commandHistory.size() : 0;
    }
    
    /**
     * 关闭命令调用器
     */
    public void shutdown() {
        shutdown = true;
        
        commandExecutor.shutdown();
        try {
            if (!commandExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                commandExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            commandExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("CommandInvoker shutdown completed");
    }
    
    /**
     * 记录命令执行
     */
    private void recordExecution(Command command, Command.CommandResult result, long executionTime) {
        synchronized (commandHistory) {
            commandHistory.addLast(new CommandExecution(command, result, executionTime));
            
            // 限制历史记录大小
            while (commandHistory.size() > maxHistorySize) {
                commandHistory.removeFirst();
            }
        }
    }
    
    /**
     * 更新统计信息
     */
    private void updateStatistics(Command command, Command.CommandResult result, long executionTime) {
        String commandName = command.getCommandName();
        
        commandStats.compute(commandName, (key, stats) -> {
            if (stats == null) {
                stats = new CommandStatistics(commandName);
            }
            stats.recordExecution(result.isSuccess(), executionTime);
            return stats;
        });
    }
    
    /**
     * 命令执行记录
     */
    public static class CommandExecution {
        private final Command command;
        private final Command.CommandResult result;
        private final long executionTime;
        private final long timestamp;
        
        public CommandExecution(Command command, Command.CommandResult result, long executionTime) {
            this.command = command;
            this.result = result;
            this.executionTime = executionTime;
            this.timestamp = System.currentTimeMillis();
        }
        
        public Command getCommand() { return command; }
        public Command.CommandResult getResult() { return result; }
        public long getExecutionTime() { return executionTime; }
        public long getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("CommandExecution{command='%s', success=%s, executionTime=%d, timestamp=%d}",
                command.getCommandName(), result.isSuccess(), executionTime, timestamp);
        }
    }
    
    /**
     * 命令统计信息
     */
    public static class CommandStatistics {
        private final String commandName;
        private long totalExecutions;
        private long successfulExecutions;
        private long failedExecutions;
        private long totalExecutionTime;
        private long minExecutionTime = Long.MAX_VALUE;
        private long maxExecutionTime = Long.MIN_VALUE;
        private final long createdTime;
        
        public CommandStatistics(String commandName) {
            this.commandName = commandName;
            this.createdTime = System.currentTimeMillis();
        }
        
        public synchronized void recordExecution(boolean success, long executionTime) {
            totalExecutions++;
            if (success) {
                successfulExecutions++;
            } else {
                failedExecutions++;
            }
            
            totalExecutionTime += executionTime;
            minExecutionTime = Math.min(minExecutionTime, executionTime);
            maxExecutionTime = Math.max(maxExecutionTime, executionTime);
        }
        
        // Getters
        public String getCommandName() { return commandName; }
        public long getTotalExecutions() { return totalExecutions; }
        public long getSuccessfulExecutions() { return successfulExecutions; }
        public long getFailedExecutions() { return failedExecutions; }
        public long getTotalExecutionTime() { return totalExecutionTime; }
        public long getMinExecutionTime() { return minExecutionTime == Long.MAX_VALUE ? 0 : minExecutionTime; }
        public long getMaxExecutionTime() { return maxExecutionTime == Long.MIN_VALUE ? 0 : maxExecutionTime; }
        public long getCreatedTime() { return createdTime; }
        
        public double getSuccessRate() {
            return totalExecutions > 0 ? (double) successfulExecutions / totalExecutions : 0.0;
        }
        
        public double getAverageExecutionTime() {
            return totalExecutions > 0 ? (double) totalExecutionTime / totalExecutions : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("CommandStatistics{name='%s', total=%d, success=%d, failed=%d, " +
                "successRate=%.2f%%, avgTime=%.2fms}",
                commandName, totalExecutions, successfulExecutions, failedExecutions,
                getSuccessRate() * 100, getAverageExecutionTime());
        }
    }
}