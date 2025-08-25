package com.xiaoxiao.arissweeping.exception;

import com.xiaoxiao.arissweeping.util.LoggerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 统一异常处理器
 * 负责处理、记录和报告所有清理相关的异常
 */
public class ExceptionHandler {
    
    // 异常统计
    private final ConcurrentHashMap<CleanupException.ErrorCode, AtomicLong> errorCounts = new ConcurrentHashMap<>();
    private final AtomicLong totalExceptions = new AtomicLong(0);
    
    // 异常处理策略
    private final ConcurrentHashMap<CleanupException.ErrorCode, Consumer<CleanupException>> errorHandlers = new ConcurrentHashMap<>();
    
    // 配置
    private boolean enableDetailedLogging = true;
    private boolean enablePlayerNotification = false;
    private boolean enableStackTrace = false;
    
    public ExceptionHandler() {
        initializeDefaultHandlers();
    }
    
    /**
     * 初始化默认异常处理器
     */
    private void initializeDefaultHandlers() {
        // 配置相关错误处理
        registerHandler(CleanupException.ErrorCode.CONFIG_LOAD_FAILED, this::handleConfigError);
        registerHandler(CleanupException.ErrorCode.CONFIG_VALIDATION_FAILED, this::handleConfigError);
        registerHandler(CleanupException.ErrorCode.CONFIG_SAVE_FAILED, this::handleConfigError);
        
        // 实体处理错误
        registerHandler(CleanupException.ErrorCode.ENTITY_ACCESS_FAILED, this::handleEntityError);
        registerHandler(CleanupException.ErrorCode.ENTITY_REMOVAL_FAILED, this::handleEntityError);
        registerHandler(CleanupException.ErrorCode.ENTITY_VALIDATION_FAILED, this::handleEntityError);
        
        // 性能监控错误
        registerHandler(CleanupException.ErrorCode.TPS_MONITOR_FAILED, this::handlePerformanceError);
        registerHandler(CleanupException.ErrorCode.PERFORMANCE_ANALYSIS_FAILED, this::handlePerformanceError);
        
        // 系统错误
        registerHandler(CleanupException.ErrorCode.SYSTEM_ERROR, this::handleSystemError);
        registerHandler(CleanupException.ErrorCode.RESOURCE_EXHAUSTED, this::handleSystemError);
        registerHandler(CleanupException.ErrorCode.TIMEOUT, this::handleSystemError);
    }
    
    /**
     * 注册异常处理器
     */
    public void registerHandler(CleanupException.ErrorCode errorCode, Consumer<CleanupException> handler) {
        errorHandlers.put(errorCode, handler);
    }
    
    /**
     * 处理异常
     */
    public void handleException(CleanupException exception) {
        if (exception == null) {
            return;
        }
        
        // 更新统计
        totalExceptions.incrementAndGet();
        errorCounts.computeIfAbsent(exception.getErrorCode(), k -> new AtomicLong(0)).incrementAndGet();
        
        // 记录异常
        logException(exception);
        
        // 执行特定的异常处理逻辑
        Consumer<CleanupException> handler = errorHandlers.get(exception.getErrorCode());
        if (handler != null) {
            try {
                handler.accept(exception);
            } catch (Exception e) {
                LoggerUtil.severe("ExceptionHandler", "异常处理器执行失败", e);
            }
        } else {
            // 默认处理
            handleDefaultException(exception);
        }
    }
    
    /**
     * 处理普通异常（非CleanupException）
     */
    public void handleException(String component, String message, Throwable throwable) {
        CleanupException.ErrorCode errorCode = determineErrorCode(throwable);
        CleanupException exception = new CleanupException(errorCode, message, component, throwable);
        handleException(exception);
    }
    
    /**
     * 记录异常
     */
    private void logException(CleanupException exception) {
        String logMessage = formatExceptionMessage(exception);
        
        // 根据错误严重程度选择日志级别
        switch (exception.getErrorCode()) {
            case CONFIG_LOAD_FAILED:
            case SYSTEM_ERROR:
            case RESOURCE_EXHAUSTED:
            case DATA_CORRUPTION:
                LoggerUtil.severe("ExceptionHandler", logMessage, enableStackTrace ? exception : null);
                break;
                
            case ENTITY_ACCESS_FAILED:
            case CHUNK_ACCESS_FAILED:
            case TASK_EXECUTION_FAILED:
                LoggerUtil.warning("ExceptionHandler", logMessage);
                if (enableStackTrace && exception.getCause() != null) {
                    LoggerUtil.warning("ExceptionHandler", "堆栈跟踪", exception.getCause());
                }
                break;
                
            default:
                LoggerUtil.info("ExceptionHandler", logMessage);
                break;
        }
    }
    
    /**
     * 格式化异常消息
     */
    private String formatExceptionMessage(CleanupException exception) {
        StringBuilder sb = new StringBuilder();
        
        if (enableDetailedLogging) {
            sb.append(exception.getFullMessage());
            
            if (exception.getCause() != null) {
                sb.append(" [原因: ").append(exception.getCause().getMessage()).append("]");
            }
        } else {
            sb.append(exception.getMessage());
        }
        
        return sb.toString();
    }
    
    /**
     * 确定异常的错误代码
     */
    private CleanupException.ErrorCode determineErrorCode(Throwable throwable) {
        if (throwable == null) {
            return CleanupException.ErrorCode.UNKNOWN_ERROR;
        }
        
        String className = throwable.getClass().getSimpleName().toLowerCase();
        String message = throwable.getMessage() != null ? throwable.getMessage().toLowerCase() : "";
        
        // 根据异常类型和消息推断错误代码
        if (className.contains("nullpointer")) {
            return CleanupException.ErrorCode.SYSTEM_ERROR;
        } else if (className.contains("illegalargument") || className.contains("illegalstate")) {
            return CleanupException.ErrorCode.ENTITY_VALIDATION_FAILED;
        } else if (className.contains("timeout") || message.contains("timeout")) {
            return CleanupException.ErrorCode.TIMEOUT;
        } else if (className.contains("outofmemory") || message.contains("memory")) {
            return CleanupException.ErrorCode.RESOURCE_EXHAUSTED;
        } else if (className.contains("io") || className.contains("file")) {
            return CleanupException.ErrorCode.DATA_SAVE_FAILED;
        } else if (className.contains("security") || className.contains("permission")) {
            return CleanupException.ErrorCode.PERMISSION_DENIED;
        }
        
        return CleanupException.ErrorCode.UNKNOWN_ERROR;
    }
    
    /**
     * 配置错误处理
     */
    private void handleConfigError(CleanupException exception) {
        LoggerUtil.severe("ConfigError", "配置系统发生严重错误: %s", exception.getFullMessage());
        
        // 通知管理员
        if (enablePlayerNotification) {
            notifyAdmins("§c[配置错误] " + exception.getMessage());
        }
    }
    
    /**
     * 实体错误处理
     */
    private void handleEntityError(CleanupException exception) {
        LoggerUtil.warning("EntityError", "实体处理错误: %s", exception.getFullMessage());
        
        // 实体错误通常不需要立即通知，但需要记录
        long errorCount = errorCounts.get(exception.getErrorCode()).get();
        if (errorCount % 100 == 0) { // 每100次错误通知一次
            LoggerUtil.warning("EntityError", "实体错误已发生 %d 次: %s", errorCount, exception.getErrorCode());
        }
    }
    
    /**
     * 性能错误处理
     */
    private void handlePerformanceError(CleanupException exception) {
        LoggerUtil.warning("PerformanceError", "性能监控错误: %s", exception.getFullMessage());
        
        // 性能错误可能影响自适应功能
        if (enablePlayerNotification) {
            notifyAdmins("§e[性能警告] " + exception.getMessage());
        }
    }
    
    /**
     * 系统错误处理
     */
    private void handleSystemError(CleanupException exception) {
        LoggerUtil.severe("SystemError", "系统严重错误: %s", exception.getFullMessage());
        
        // 系统错误需要立即通知
        if (enablePlayerNotification) {
            notifyAdmins("§4[系统错误] " + exception.getMessage());
        }
        
        // 可能需要触发紧急措施
        if (exception.getErrorCode() == CleanupException.ErrorCode.RESOURCE_EXHAUSTED) {
            LoggerUtil.severe("SystemError", "检测到资源耗尽，建议立即检查服务器状态");
        }
    }
    
    /**
     * 默认异常处理
     */
    private void handleDefaultException(CleanupException exception) {
        LoggerUtil.info("DefaultHandler", "未处理的异常: %s", exception.getFullMessage());
    }
    
    /**
     * 通知管理员
     */
    private void notifyAdmins(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("arissweeping.admin.notify")) {
                player.sendMessage(message);
            }
        }
    }
    
    /**
     * 获取异常统计
     */
    public ExceptionStatistics getStatistics() {
        return new ExceptionStatistics(totalExceptions.get(), new ConcurrentHashMap<>(errorCounts));
    }
    
    /**
     * 重置统计
     */
    public void resetStatistics() {
        totalExceptions.set(0);
        errorCounts.clear();
    }
    
    /**
     * 配置方法
     */
    public void setDetailedLogging(boolean enabled) {
        this.enableDetailedLogging = enabled;
    }
    
    public void setPlayerNotification(boolean enabled) {
        this.enablePlayerNotification = enabled;
    }
    
    public void setStackTrace(boolean enabled) {
        this.enableStackTrace = enabled;
    }
    
    /**
     * 异常统计类
     */
    public static class ExceptionStatistics {
        private final long totalExceptions;
        private final ConcurrentHashMap<CleanupException.ErrorCode, AtomicLong> errorCounts;
        
        public ExceptionStatistics(long totalExceptions, ConcurrentHashMap<CleanupException.ErrorCode, AtomicLong> errorCounts) {
            this.totalExceptions = totalExceptions;
            this.errorCounts = errorCounts;
        }
        
        public long getTotalExceptions() {
            return totalExceptions;
        }
        
        public long getErrorCount(CleanupException.ErrorCode errorCode) {
            AtomicLong count = errorCounts.get(errorCode);
            return count != null ? count.get() : 0;
        }
        
        public ConcurrentHashMap<CleanupException.ErrorCode, AtomicLong> getAllErrorCounts() {
            return new ConcurrentHashMap<>(errorCounts);
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("异常统计 - 总计: ").append(totalExceptions);
            
            if (!errorCounts.isEmpty()) {
                sb.append(", 详细: {");
                errorCounts.forEach((code, count) -> 
                    sb.append(code.getDescription()).append(": ").append(count.get()).append(", "));
                sb.setLength(sb.length() - 2); // 移除最后的逗号和空格
                sb.append("}");
            }
            
            return sb.toString();
        }
    }
}