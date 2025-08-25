package com.xiaoxiao.arissweeping.command;

import com.xiaoxiao.arissweeping.util.LoggerUtil;

/**
 * 重试执行命令装饰器
 */
public class RetryCommand implements Command {
    private final Command wrappedCommand;
    private final int maxRetries;
    private final long retryDelayMillis;
    private int currentAttempt = 0;
    private CommandResult lastResult;
    
    public RetryCommand(Command wrappedCommand, int maxRetries, long retryDelayMillis) {
        this.wrappedCommand = wrappedCommand;
        this.maxRetries = Math.max(1, maxRetries);
        this.retryDelayMillis = Math.max(0, retryDelayMillis);
    }
    
    public RetryCommand(Command wrappedCommand, int maxRetries) {
        this(wrappedCommand, maxRetries, 1000); // 默认1秒重试间隔
    }
    
    @Override
    public CommandResult execute() {
        currentAttempt = 0;
        
        while (currentAttempt < maxRetries) {
            currentAttempt++;
            
            try {
                lastResult = wrappedCommand.execute();
                
                if (lastResult.isSuccess()) {
                    LoggerUtil.info("RetryCommand", 
                        String.format("Command succeeded on attempt %d/%d", currentAttempt, maxRetries));
                    return lastResult;
                }
                
                LoggerUtil.warning("RetryCommand", 
                    String.format("Command failed on attempt %d/%d: %s", 
                        currentAttempt, maxRetries, lastResult.getMessage()));
                
                // 如果不是最后一次尝试，等待重试间隔
                if (currentAttempt < maxRetries && retryDelayMillis > 0) {
                    try {
                        Thread.sleep(retryDelayMillis);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return CommandResult.failure("Retry interrupted", e);
                    }
                }
                
            } catch (Exception e) {
                LoggerUtil.severe("RetryCommand", 
                    String.format("Exception on attempt %d/%d: %s", 
                        currentAttempt, maxRetries, e.getMessage()), e);
                
                lastResult = CommandResult.failure("Exception during execution: " + e.getMessage(), e);
                
                // 如果不是最后一次尝试，等待重试间隔
                if (currentAttempt < maxRetries && retryDelayMillis > 0) {
                    try {
                        Thread.sleep(retryDelayMillis);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return CommandResult.failure("Retry interrupted", ie);
                    }
                }
            }
        }
        
        LoggerUtil.severe("RetryCommand", 
            String.format("Command failed after %d attempts", maxRetries));
        
        return CommandResult.failure(
            String.format("Command failed after %d attempts. Last error: %s", 
                maxRetries, lastResult != null ? lastResult.getMessage() : "Unknown error"),
            lastResult != null ? lastResult.getException() : null);
    }
    
    @Override
    public boolean canExecute() {
        return wrappedCommand.canExecute();
    }
    
    @Override
    public String getDescription() {
        return String.format("Retry(%d times, %dms delay): %s", 
            maxRetries, retryDelayMillis, wrappedCommand.getDescription());
    }
    
    @Override
    public String getCommandName() {
        return "Retry[" + wrappedCommand.getCommandName() + "]";
    }
    
    @Override
    public CommandResult undo() {
        if (lastResult == null || !lastResult.isSuccess()) {
            return CommandResult.failure("Cannot undo RetryCommand that hasn't succeeded");
        }
        return wrappedCommand.undo();
    }
    
    @Override
    public ValidationResult validate() {
        return wrappedCommand.validate();
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public long getRetryDelayMillis() {
        return retryDelayMillis;
    }
    
    public int getCurrentAttempt() {
        return currentAttempt;
    }
    
    public CommandResult getLastResult() {
        return lastResult;
    }
    
    public Command getWrappedCommand() {
        return wrappedCommand;
    }
}