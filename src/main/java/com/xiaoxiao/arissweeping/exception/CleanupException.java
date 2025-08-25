package com.xiaoxiao.arissweeping.exception;

/**
 * 清理操作异常基类
 * 所有清理相关的异常都应该继承此类
 */
public class CleanupException extends Exception {
    
    private final ErrorCode errorCode;
    private final String context;
    
    public CleanupException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.context = null;
    }
    
    public CleanupException(ErrorCode errorCode, String message, String context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context;
    }
    
    public CleanupException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = null;
    }
    
    public CleanupException(ErrorCode errorCode, String message, String context, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = context;
    }
    
    /**
     * 获取错误代码
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    /**
     * 获取错误上下文
     */
    public String getContext() {
        return context;
    }
    
    /**
     * 获取完整的错误信息
     */
    public String getFullMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[错误代码: ").append(errorCode.getCode()).append("] ");
        sb.append(getMessage());
        
        if (context != null && !context.isEmpty()) {
            sb.append(" [上下文: ").append(context).append("]");
        }
        
        return sb.toString();
    }
    
    /**
     * 错误代码枚举
     */
    public enum ErrorCode {
        // 配置相关错误 (1000-1099)
        CONFIG_LOAD_FAILED(1001, "配置加载失败"),
        CONFIG_VALIDATION_FAILED(1002, "配置验证失败"),
        CONFIG_SAVE_FAILED(1003, "配置保存失败"),
        
        // 实体处理错误 (1100-1199)
        ENTITY_ACCESS_FAILED(1101, "实体访问失败"),
        ENTITY_REMOVAL_FAILED(1102, "实体移除失败"),
        ENTITY_VALIDATION_FAILED(1103, "实体验证失败"),
        ENTITY_FILTER_FAILED(1104, "实体过滤失败"),
        
        // 世界/区块处理错误 (1200-1299)
        WORLD_ACCESS_FAILED(1201, "世界访问失败"),
        CHUNK_LOAD_FAILED(1202, "区块加载失败"),
        CHUNK_ACCESS_FAILED(1203, "区块访问失败"),
        
        // 任务调度错误 (1300-1399)
        TASK_SCHEDULE_FAILED(1301, "任务调度失败"),
        TASK_EXECUTION_FAILED(1302, "任务执行失败"),
        TASK_CANCELLATION_FAILED(1303, "任务取消失败"),
        
        // 性能监控错误 (1400-1499)
        TPS_MONITOR_FAILED(1401, "TPS监控失败"),
        PERFORMANCE_ANALYSIS_FAILED(1402, "性能分析失败"),
        METRICS_COLLECTION_FAILED(1403, "指标收集失败"),
        
        // 数据库/存储错误 (1500-1599)
        DATA_SAVE_FAILED(1501, "数据保存失败"),
        DATA_LOAD_FAILED(1502, "数据加载失败"),
        DATA_CORRUPTION(1503, "数据损坏"),
        
        // 网络/通信错误 (1600-1699)
        NETWORK_ERROR(1601, "网络错误"),
        API_CALL_FAILED(1602, "API调用失败"),
        
        // 权限/安全错误 (1700-1799)
        PERMISSION_DENIED(1701, "权限不足"),
        SECURITY_VIOLATION(1702, "安全违规"),
        
        // 系统错误 (1800-1899)
        SYSTEM_ERROR(1801, "系统错误"),
        RESOURCE_EXHAUSTED(1802, "资源耗尽"),
        TIMEOUT(1803, "操作超时"),
        
        // 未知错误 (9999)
        UNKNOWN_ERROR(9999, "未知错误");
        
        private final int code;
        private final String description;
        
        ErrorCode(int code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public int getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        @Override
        public String toString() {
            return String.format("%s(%d)", description, code);
        }
    }
}