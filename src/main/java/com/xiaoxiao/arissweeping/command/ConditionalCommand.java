package com.xiaoxiao.arissweeping.command;

import com.xiaoxiao.arissweeping.util.LoggerUtil;
import java.util.function.Supplier;

/**
 * 条件执行命令装饰器
 */
public class ConditionalCommand implements Command {
    private final Command wrappedCommand;
    private final Supplier<Boolean> condition;
    private final String conditionDescription;
    private boolean lastConditionResult = false;
    
    public ConditionalCommand(Command wrappedCommand, Supplier<Boolean> condition, String conditionDescription) {
        this.wrappedCommand = wrappedCommand;
        this.condition = condition;
        this.conditionDescription = conditionDescription != null ? conditionDescription : "Custom condition";
    }
    
    public ConditionalCommand(Command wrappedCommand, Supplier<Boolean> condition) {
        this(wrappedCommand, condition, null);
    }
    
    @Override
    public CommandResult execute() {
        try {
            lastConditionResult = condition.get();
            
            if (!lastConditionResult) {
                String message = String.format("Condition not met: %s", conditionDescription);
                LoggerUtil.info("ConditionalCommand", message);
                return CommandResult.success(message + " - Command skipped");
            }
            
            LoggerUtil.info("ConditionalCommand", 
                String.format("Condition met: %s - Executing command", conditionDescription));
            
            return wrappedCommand.execute();
            
        } catch (Exception e) {
            LoggerUtil.severe("ConditionalCommand", 
                "Error evaluating condition: " + e.getMessage(), e);
            return CommandResult.failure("Failed to evaluate condition: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean canExecute() {
        try {
            return condition.get() && wrappedCommand.canExecute();
        } catch (Exception e) {
            LoggerUtil.severe("ConditionalCommand", 
                "Error checking if command can execute: " + e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public String getDescription() {
        return String.format("Conditional(%s): %s", conditionDescription, wrappedCommand.getDescription());
    }
    
    @Override
    public String getCommandName() {
        return "Conditional[" + wrappedCommand.getCommandName() + "]";
    }
    
    @Override
    public CommandResult undo() {
        if (!lastConditionResult) {
            return CommandResult.success("ConditionalCommand was not executed, nothing to undo");
        }
        return wrappedCommand.undo();
    }
    
    @Override
    public ValidationResult validate() {
        // 验证包装的命令
        ValidationResult wrappedResult = wrappedCommand.validate();
        if (!wrappedResult.isValid()) {
            return wrappedResult;
        }
        
        // 验证条件是否可以执行
        try {
            condition.get();
            return ValidationResult.valid();
        } catch (Exception e) {
            return ValidationResult.invalid("Condition evaluation failed: " + e.getMessage());
        }
    }
    
    public boolean getLastConditionResult() {
        return lastConditionResult;
    }
    
    public String getConditionDescription() {
        return conditionDescription;
    }
    
    public Command getWrappedCommand() {
        return wrappedCommand;
    }
    
    public Supplier<Boolean> getCondition() {
        return condition;
    }
    
    /**
     * 创建一个简单的布尔条件
     */
    public static ConditionalCommand when(Command command, boolean condition, String description) {
        return new ConditionalCommand(command, () -> condition, description);
    }
    
    /**
     * 创建一个基于供应商的条件
     */
    public static ConditionalCommand when(Command command, Supplier<Boolean> condition, String description) {
        return new ConditionalCommand(command, condition, description);
    }
}