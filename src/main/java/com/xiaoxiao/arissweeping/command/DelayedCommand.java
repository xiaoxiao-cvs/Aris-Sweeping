package com.xiaoxiao.arissweeping.command;

import com.xiaoxiao.arissweeping.util.LoggerUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 延迟执行命令装饰器
 */
public class DelayedCommand implements Command {
    private final Command wrappedCommand;
    private final long delayMillis;
    private final JavaPlugin plugin;
    private volatile boolean executed = false;
    
    public DelayedCommand(Command wrappedCommand, long delayMillis, JavaPlugin plugin) {
        this.wrappedCommand = wrappedCommand;
        this.delayMillis = delayMillis;
        this.plugin = plugin;
    }
    
    @Override
    public CommandResult execute() {
        if (executed) {
            return CommandResult.failure("DelayedCommand has already been executed");
        }
        
        try {
            // 使用Bukkit调度器延迟执行
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    CommandResult result = wrappedCommand.execute();
                    executed = true;
                    LoggerUtil.info("DelayedCommand", "Delayed command executed with result: " + result.isSuccess());
                } catch (Exception e) {
                    LoggerUtil.severe("DelayedCommand", "Error executing delayed command: " + e.getMessage(), e);
                }
            }, delayMillis / 50); // 转换为ticks (50ms = 1 tick)
            
            return CommandResult.success("DelayedCommand scheduled for execution in " + delayMillis + "ms");
        } catch (Exception e) {
            LoggerUtil.severe("DelayedCommand", "Failed to schedule delayed command: " + e.getMessage(), e);
            return CommandResult.failure("Failed to schedule delayed command: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean canExecute() {
        return !executed && wrappedCommand.canExecute();
    }
    
    @Override
    public String getDescription() {
        return "Delayed(" + delayMillis + "ms): " + wrappedCommand.getDescription();
    }
    
    @Override
    public CommandResult undo() {
        if (!executed) {
            return CommandResult.failure("Cannot undo DelayedCommand that hasn't been executed yet");
        }
        return wrappedCommand.undo();
    }
    
    @Override
    public ValidationResult validate() {
        return wrappedCommand.validate();
    }
    
    @Override
    public String getCommandName() {
        return "Delayed[" + wrappedCommand.getCommandName() + "]";
    }
    
    public boolean isExecuted() {
        return executed;
    }
    
    public long getDelayMillis() {
        return delayMillis;
    }
    
    public Command getWrappedCommand() {
        return wrappedCommand;
    }
}