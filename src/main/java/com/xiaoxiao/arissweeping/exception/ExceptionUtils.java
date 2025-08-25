package com.xiaoxiao.arissweeping.exception;

import com.xiaoxiao.arissweeping.exception.CleanupException.ErrorCode;

/**
 * 异常工具类
 * 提供便捷的异常创建和处理方法
 */
public class ExceptionUtils {
    
    // 全局异常处理器
    private static ExceptionHandler globalHandler = new ExceptionHandler();
    
    /**
     * 设置全局异常处理器
     */
    public static void setGlobalHandler(ExceptionHandler handler) {
        globalHandler = handler;
    }
    
    /**
     * 获取全局异常处理器
     */
    public static ExceptionHandler getGlobalHandler() {
        return globalHandler;
    }
    
    // ========== 配置相关异常 ==========
    
    /**
     * 创建配置加载失败异常
     */
    public static CleanupException configLoadFailed(String configName, Throwable cause) {
        return new CleanupException(
            ErrorCode.CONFIG_LOAD_FAILED,
            "配置文件加载失败: " + configName,
            configName,
            cause
        );
    }
    
    /**
     * 创建配置验证失败异常
     */
    public static CleanupException configValidationFailed(String configName, String reason) {
        return new CleanupException(
            ErrorCode.CONFIG_VALIDATION_FAILED,
            "配置验证失败: " + reason,
            configName
        );
    }
    
    /**
     * 创建配置保存失败异常
     */
    public static CleanupException configSaveFailed(String configName, Throwable cause) {
        return new CleanupException(
            ErrorCode.CONFIG_SAVE_FAILED,
            "配置文件保存失败: " + configName,
            configName,
            cause
        );
    }
    
    // ========== 实体相关异常 ==========
    
    /**
     * 创建实体访问失败异常
     */
    public static CleanupException entityAccessFailed(String entityInfo, Throwable cause) {
        return new CleanupException(
            ErrorCode.ENTITY_ACCESS_FAILED,
            "实体访问失败: " + entityInfo,
            entityInfo,
            cause
        );
    }
    
    /**
     * 创建实体移除失败异常
     */
    public static CleanupException entityRemovalFailed(String entityInfo, Throwable cause) {
        return new CleanupException(
            ErrorCode.ENTITY_REMOVAL_FAILED,
            "实体移除失败: " + entityInfo,
            entityInfo,
            cause
        );
    }
    
    /**
     * 创建实体验证失败异常
     */
    public static CleanupException entityValidationFailed(String entityInfo, String reason) {
        return new CleanupException(
            ErrorCode.ENTITY_VALIDATION_FAILED,
            "实体验证失败: " + reason,
            entityInfo
        );
    }
    
    /**
     * 创建实体过滤失败异常
     */
    public static CleanupException entityFilterFailed(String filterInfo, Throwable cause) {
        return new CleanupException(
            ErrorCode.ENTITY_FILTER_FAILED,
            "实体过滤失败: " + filterInfo,
            filterInfo,
            cause
        );
    }
    
    // ========== 世界/区块相关异常 ==========
    
    /**
     * 创建世界访问失败异常
     */
    public static CleanupException worldAccessFailed(String worldName, Throwable cause) {
        return new CleanupException(
            ErrorCode.WORLD_ACCESS_FAILED,
            "世界访问失败: " + worldName,
            worldName,
            cause
        );
    }
    
    /**
     * 创建区块加载失败异常
     */
    public static CleanupException chunkLoadFailed(String chunkInfo, Throwable cause) {
        return new CleanupException(
            ErrorCode.CHUNK_LOAD_FAILED,
            "区块加载失败: " + chunkInfo,
            chunkInfo,
            cause
        );
    }
    
    /**
     * 创建区块访问失败异常
     */
    public static CleanupException chunkAccessFailed(String chunkInfo, Throwable cause) {
        return new CleanupException(
            ErrorCode.CHUNK_ACCESS_FAILED,
            "区块访问失败: " + chunkInfo,
            chunkInfo,
            cause
        );
    }
    
    // ========== 任务调度相关异常 ==========
    
    /**
     * 创建任务调度失败异常
     */
    public static CleanupException taskScheduleFailed(String taskInfo, Throwable cause) {
        return new CleanupException(
            ErrorCode.TASK_SCHEDULE_FAILED,
            "任务调度失败: " + taskInfo,
            taskInfo,
            cause
        );
    }
    
    /**
     * 创建任务执行失败异常
     */
    public static CleanupException taskExecutionFailed(String taskInfo, Throwable cause) {
        return new CleanupException(
            ErrorCode.TASK_EXECUTION_FAILED,
            "任务执行失败: " + taskInfo,
            taskInfo,
            cause
        );
    }
    
    /**
     * 创建任务取消失败异常
     */
    public static CleanupException taskCancellationFailed(String taskInfo, Throwable cause) {
        return new CleanupException(
            ErrorCode.TASK_CANCELLATION_FAILED,
            "任务取消失败: " + taskInfo,
            taskInfo,
            cause
        );
    }
    
    // ========== 性能监控相关异常 ==========
    
    /**
     * 创建TPS监控失败异常
     */
    public static CleanupException tpsMonitorFailed(String reason, Throwable cause) {
        return new CleanupException(
            ErrorCode.TPS_MONITOR_FAILED,
            "TPS监控失败: " + reason,
            "TPS监控",
            cause
        );
    }
    
    /**
     * 创建性能分析失败异常
     */
    public static CleanupException performanceAnalysisFailed(String analysisInfo, Throwable cause) {
        return new CleanupException(
            ErrorCode.PERFORMANCE_ANALYSIS_FAILED,
            "性能分析失败: " + analysisInfo,
            analysisInfo,
            cause
        );
    }
    
    /**
     * 创建指标收集失败异常
     */
    public static CleanupException metricsCollectionFailed(String metricsInfo, Throwable cause) {
        return new CleanupException(
            ErrorCode.METRICS_COLLECTION_FAILED,
            "指标收集失败: " + metricsInfo,
            metricsInfo,
            cause
        );
    }
    
    // ========== 系统相关异常 ==========
    
    /**
     * 创建系统错误异常
     */
    public static CleanupException systemError(String reason, Throwable cause) {
        return new CleanupException(
            ErrorCode.SYSTEM_ERROR,
            "系统错误: " + reason,
            "系统",
            cause
        );
    }
    
    /**
     * 创建资源耗尽异常
     */
    public static CleanupException resourceExhausted(String resourceType, String details) {
        return new CleanupException(
            ErrorCode.RESOURCE_EXHAUSTED,
            "资源耗尽: " + resourceType + " - " + details,
            resourceType
        );
    }
    
    /**
     * 创建超时异常
     */
    public static CleanupException timeout(String operation, long timeoutMs) {
        return new CleanupException(
            ErrorCode.TIMEOUT,
            "操作超时: " + operation + " (超时时间: " + timeoutMs + "ms)",
            operation
        );
    }
    
    // ========== 便捷处理方法 ==========
    
    /**
     * 安全执行操作，捕获并处理异常
     */
    public static void safeExecute(String operation, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            CleanupException exception = new CleanupException(
                ErrorCode.SYSTEM_ERROR,
                "操作执行失败: " + operation,
                operation,
                e
            );
            globalHandler.handleException(exception);
        }
    }
    
    /**
     * 安全执行操作，捕获并处理异常，返回结果
     */
    public static <T> T safeExecute(String operation, java.util.function.Supplier<T> action, T defaultValue) {
        try {
            return action.get();
        } catch (Exception e) {
            CleanupException exception = new CleanupException(
                ErrorCode.SYSTEM_ERROR,
                "操作执行失败: " + operation,
                operation,
                e
            );
            globalHandler.handleException(exception);
            return defaultValue;
        }
    }
    
    /**
     * 处理并重新抛出异常
     */
    public static void handleAndRethrow(CleanupException exception) throws CleanupException {
        globalHandler.handleException(exception);
        throw exception;
    }
    
    /**
     * 处理普通异常并转换为CleanupException
     */
    public static CleanupException wrapException(String component, String operation, Throwable cause) {
        ErrorCode errorCode = determineErrorCode(cause);
        return new CleanupException(
            errorCode,
            operation + "失败",
            component,
            cause
        );
    }
    
    /**
     * 确定异常的错误代码
     */
    private static ErrorCode determineErrorCode(Throwable throwable) {
        if (throwable == null) {
            return ErrorCode.UNKNOWN_ERROR;
        }
        
        String className = throwable.getClass().getSimpleName().toLowerCase();
        String message = throwable.getMessage() != null ? throwable.getMessage().toLowerCase() : "";
        
        // 根据异常类型推断错误代码
        if (className.contains("nullpointer")) {
            return ErrorCode.SYSTEM_ERROR;
        } else if (className.contains("illegalargument") || className.contains("illegalstate")) {
            return ErrorCode.ENTITY_VALIDATION_FAILED;
        } else if (className.contains("timeout") || message.contains("timeout")) {
            return ErrorCode.TIMEOUT;
        } else if (className.contains("outofmemory") || message.contains("memory")) {
            return ErrorCode.RESOURCE_EXHAUSTED;
        } else if (className.contains("io") || className.contains("file")) {
            return ErrorCode.DATA_SAVE_FAILED;
        } else if (className.contains("security") || className.contains("permission")) {
            return ErrorCode.PERMISSION_DENIED;
        }
        
        return ErrorCode.UNKNOWN_ERROR;
    }
    
    /**
     * 格式化异常信息用于显示
     */
    public static String formatExceptionForDisplay(CleanupException exception) {
        StringBuilder sb = new StringBuilder();
        sb.append("[错误] ").append(exception.getMessage());
        
        if (exception.getContext() != null) {
            sb.append(" (组件: ").append(exception.getContext()).append(")");
        }
        
        return sb.toString();
    }
    
    /**
     * 检查异常是否为严重错误
     */
    public static boolean isCriticalError(CleanupException exception) {
        ErrorCode code = exception.getErrorCode();
        return code == ErrorCode.SYSTEM_ERROR ||
               code == ErrorCode.RESOURCE_EXHAUSTED ||
               code == ErrorCode.DATA_CORRUPTION ||
               code == ErrorCode.CONFIG_LOAD_FAILED;
    }
    
    /**
     * 检查异常是否可以重试
     */
    public static boolean isRetryable(CleanupException exception) {
        ErrorCode code = exception.getErrorCode();
        return code == ErrorCode.TIMEOUT ||
               code == ErrorCode.NETWORK_ERROR ||
               code == ErrorCode.TASK_EXECUTION_FAILED ||
               code == ErrorCode.CHUNK_ACCESS_FAILED;
    }
}