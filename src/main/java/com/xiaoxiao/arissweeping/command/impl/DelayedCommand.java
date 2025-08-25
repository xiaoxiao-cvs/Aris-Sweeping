package com.xiaoxiao.arissweeping.command.impl;

import com.xiaoxiao.arissweeping.command.Command;
import com.xiaoxiao.arissweeping.command.Command.CommandResult;
import com.xiaoxiao.arissweeping.command.Command.CommandType;
import com.xiaoxiao.arissweeping.util.LoggerUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.HashMap;

/**
 * 延迟执行命令
 */
public class DelayedCommand implements Command {
    private final Command wrappedCommand;
    private final long delayTicks;
    private final JavaPlugin plugin;
    private BukkitTask scheduledTask;
    private CommandResult executionResult;
    private boolean executed = false;
    
    public DelayedCommand(Command command, long delayTicks, JavaPlugin plugin) {
        this.wrappedCommand = command;
        this.delayTicks = delayTicks;
        this.plugin = plugin;
    }
    
    @Override
    public CommandResult execute() {
        long startTime = System.currentTimeMillis();
        
        try {
            if (executed) {
                return CommandResult.failure("延迟命令已经执行过了", (Throwable)null, 0);
            }
            
            // 调度延迟执行
            scheduledTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    executionResult = wrappedCommand.execute();
                    executed = true;
                    LoggerUtil.info("延迟命令执行完成: " + wrappedCommand.getCommandName());
                } catch (Exception e) {
                    executionResult = CommandResult.failure("延迟命令执行失败: " + e.getMessage(), e, 0);
                    executed = true;
                    LoggerUtil.error("延迟命令执行失败", e);
                }
            }, delayTicks);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("wrappedCommand", wrappedCommand.getCommandName());
            resultData.put("delayTicks", delayTicks);
            resultData.put("scheduled", true);
            
            LoggerUtil.info("延迟命令已调度，将在 " + delayTicks + " tick后执行: " + wrappedCommand.getCommandName());
            
            return CommandResult.success("延迟命令调度成功", resultData, executionTime);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            LoggerUtil.error("延迟命令调度失败", e);
            return CommandResult.failure("延迟命令调度失败: " + e.getMessage(), e, executionTime);
        }
    }
    
    @Override
    public CommandResult undo() {
        long startTime = System.currentTimeMillis();
        
        try {
            // 如果任务还没执行，取消它
            if (scheduledTask != null && !executed) {
                scheduledTask.cancel();
                long executionTime = System.currentTimeMillis() - startTime;
                
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("cancelled", true);
                resultData.put("wrappedCommand", wrappedCommand.getCommandName());
                
                LoggerUtil.info("延迟命令已取消: " + wrappedCommand.getCommandName());
                
                return CommandResult.success("延迟命令取消成功", resultData, executionTime);
            }
            
            // 如果已经执行，尝试撤销包装的命令
            if (executed && wrappedCommand.isUndoable()) {
                CommandResult undoResult = wrappedCommand.undo();
                long executionTime = System.currentTimeMillis() - startTime;
                
                if (undoResult.isSuccess()) {
                    LoggerUtil.info("延迟命令撤销成功: " + wrappedCommand.getCommandName());
                    return CommandResult.success("延迟命令撤销成功", undoResult.getData(), executionTime);
                } else {
                    return CommandResult.failure("延迟命令撤销失败", 
                        undoResult.getException(), executionTime);
                }
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            return CommandResult.failure("延迟命令无法撤销", (Throwable)null, executionTime);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            LoggerUtil.error("延迟命令撤销失败", e);
            return CommandResult.failure("延迟命令撤销失败: " + e.getMessage(), e, executionTime);
        }
    }
    
    @Override
    public boolean isUndoable() {
        // 如果还没执行，可以取消；如果已执行，取决于包装的命令是否可撤销
        return !executed || (executed && wrappedCommand.isUndoable());
    }
    
    @Override
    public String getCommandName() {
        return "DelayedCommand[" + wrappedCommand.getCommandName() + "]";
    }
    
    @Override
    public String getDescription() {
        return "延迟 " + delayTicks + " tick执行: " + wrappedCommand.getDescription();
    }
    
    @Override
    public CommandType getCommandType() {
        return wrappedCommand.getCommandType();
    }
    
    /**
     * 检查延迟命令是否已执行
     */
    public boolean isExecuted() {
        return executed;
    }
    
    /**
     * 获取执行结果（如果已执行）
     */
    public CommandResult getExecutionResult() {
        return executionResult;
    }
    
    /**
     * 获取包装的命令
     */
    public Command getWrappedCommand() {
        return wrappedCommand;
    }
}