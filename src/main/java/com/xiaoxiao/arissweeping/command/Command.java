package com.xiaoxiao.arissweeping.command;

import java.util.Map;
import java.util.HashMap;

/**
 * 命令接口
 * 使用命令模式封装操作
 */
public interface Command {
    
    /**
     * 执行命令
     * 
     * @return 执行结果
     */
    CommandResult execute();
    
    /**
     * 撤销命令（如果支持）
     * 
     * @return 撤销结果
     */
    default CommandResult undo() {
        return CommandResult.failure("Undo operation not supported for command: " + getCommandName());
    }
    
    /**
     * 检查命令是否可以撤销
     * 
     * @return 如果可以撤销返回true，否则返回false
     */
    default boolean isUndoable() {
        return false;
    }
    
    /**
     * 获取命令名称
     * 
     * @return 命令名称
     */
    String getCommandName();
    
    /**
     * 获取命令描述
     * 
     * @return 命令描述
     */
    String getDescription();
    
    /**
     * 获取命令参数
     * 
     * @return 命令参数映射
     */
    default Map<String, Object> getParameters() {
        return new HashMap<>();
    }
    
    /**
     * 验证命令是否可以执行
     * 
     * @return 验证结果
     */
    default ValidationResult validate() {
        return ValidationResult.valid();
    }
    
    /**
     * 获取预估执行时间（毫秒）
     * 
     * @return 预估执行时间，-1表示未知
     */
    default long getEstimatedExecutionTime() {
        return -1;
    }
    
    /**
     * 获取命令优先级
     * 数值越小优先级越高
     * 
     * @return 优先级
     */
    default int getPriority() {
        return 100;
    }
    
    /**
     * 检查命令是否需要异步执行
     * 
     * @return 如果需要异步执行返回true，否则返回false
     */
    default boolean requiresAsyncExecution() {
        return false;
    }
    
    /**
     * 获取命令类型
     * 
     * @return 命令类型
     */
    default CommandType getCommandType() {
        return CommandType.UTILITY;
    }
    
    /**
     * 检查命令是否可以执行
     * 
     * @return 如果可以执行返回true，否则返回false
     */
    default boolean canExecute() {
        return true;
    }
    
    /**
     * 命令类型枚举
     */
    enum CommandType {
        CLEANUP("清理"),
        CONFIGURATION("配置"),
        MONITORING("监控"),
        MAINTENANCE("维护"),
        EMERGENCY("紧急"),
        BATCH("批处理"),
        ANALYSIS("分析"),
        UTILITY("工具"),
        COMPOSITE("复合");
        
        private final String displayName;
        
        CommandType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 命令执行结果
     */
    class CommandResult {
        private final boolean success;
        private final String message;
        private final Map<String, Object> data;
        private final Throwable exception;
        private final long executionTime;
        private final long timestamp;
        
        private CommandResult(boolean success, String message, Map<String, Object> data, 
                            Throwable exception, long executionTime) {
            this.success = success;
            this.message = message;
            this.data = data != null ? new HashMap<>(data) : new HashMap<>();
            this.exception = exception;
            this.executionTime = executionTime;
            this.timestamp = System.currentTimeMillis();
        }
        
        public static CommandResult success(String message) {
            return new CommandResult(true, message, null, null, 0);
        }
        
        public static CommandResult success(String message, Map<String, Object> data) {
            return new CommandResult(true, message, data, null, 0);
        }
        
        public static CommandResult success(String message, Map<String, Object> data, long executionTime) {
            return new CommandResult(true, message, data, null, executionTime);
        }
        
        public static CommandResult failure(String message) {
            return new CommandResult(false, message, null, null, 0);
        }
        
        public static CommandResult failure(String message, Throwable exception) {
            return new CommandResult(false, message, null, exception, 0);
        }
        
        public static CommandResult failure(String message, Throwable exception, long executionTime) {
            return new CommandResult(false, message, null, exception, executionTime);
        }
        
        public static CommandResult failure(String message, Map<String, Object> data, long executionTime) {
            return new CommandResult(false, message, data, null, executionTime);
        }
        
        public static CommandResult failure(String message, Throwable exception, Map<String, Object> data, long executionTime) {
            return new CommandResult(false, message, data, exception, executionTime);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Map<String, Object> getData() { return new HashMap<>(data); }
        public Throwable getException() { return exception; }
        public String getError() { 
            return exception != null ? exception.getMessage() : null; 
        }
        public long getExecutionTime() { return executionTime; }
        public long getTimestamp() { return timestamp; }
        
        // 数据访问方法
        public Object getData(String key) {
            return data.get(key);
        }
        
        public <T> T getData(String key, Class<T> type) {
            Object value = data.get(key);
            if (value != null && type.isInstance(value)) {
                return type.cast(value);
            }
            return null;
        }
        
        public void putData(String key, Object value) {
            data.put(key, value);
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("CommandResult{");
            sb.append("success=").append(success);
            sb.append(", message='").append(message).append("'");
            sb.append(", executionTime=").append(executionTime);
            if (!data.isEmpty()) {
                sb.append(", data=").append(data);
            }
            if (exception != null) {
                sb.append(", exception=").append(exception.getClass().getSimpleName());
            }
            sb.append('}');
            return sb.toString();
        }
    }
    
    /**
     * 验证结果
     */
    class ValidationResult {
        private final boolean valid;
        private final String message;
        private final Map<String, String> fieldErrors;
        
        private ValidationResult(boolean valid, String message, Map<String, String> fieldErrors) {
            this.valid = valid;
            this.message = message;
            this.fieldErrors = fieldErrors != null ? new HashMap<>(fieldErrors) : new HashMap<>();
        }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, "Validation passed", null);
        }
        
        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message, null);
        }
        
        public static ValidationResult invalid(String message, Map<String, String> fieldErrors) {
            return new ValidationResult(false, message, fieldErrors);
        }
        
        // Getters
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public String getErrorMessage() { return message; } // 别名方法
        public Map<String, String> getFieldErrors() { return new HashMap<>(fieldErrors); }
        
        public boolean hasFieldErrors() {
            return !fieldErrors.isEmpty();
        }
        
        public String getFieldError(String field) {
            return fieldErrors.get(field);
        }
        
        @Override
        public String toString() {
            if (valid) {
                return "ValidationResult{valid=true}";
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("ValidationResult{valid=false, message='").append(message).append("'");
                if (!fieldErrors.isEmpty()) {
                    sb.append(", fieldErrors=").append(fieldErrors);
                }
                sb.append('}');
                return sb.toString();
            }
        }
    }
}