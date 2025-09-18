package com.arisweeping.tasks.models;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * 任务执行结果
 * 
 * 包含任务执行完成后的结果信息和统计数据
 */
public class TaskResult {
    
    private final UUID taskId;
    private final boolean success;
    private final String message;
    private final Instant completionTime;
    
    // 执行统计
    private final long executionDurationMs;
    private final long processedItems;
    private final long failedItems;
    private final Map<String, Object> statistics;
    
    // 错误信息
    private final Throwable error;
    
    /**
     * 成功结果构造函数
     */
    public TaskResult(UUID taskId, long executionDurationMs, long processedItems, Map<String, Object> statistics) {
        this(taskId, true, "Task completed successfully", null, executionDurationMs, 
             processedItems, 0, statistics);
    }
    
    /**
     * 失败结果构造函数
     */
    public TaskResult(UUID taskId, String errorMessage, Throwable error, long executionDurationMs) {
        this(taskId, false, errorMessage, error, executionDurationMs, 0, 0, Collections.emptyMap());
    }
    
    /**
     * 完整构造函数
     */
    public TaskResult(UUID taskId, boolean success, String message, Throwable error,
                     long executionDurationMs, long processedItems, long failedItems,
                     Map<String, Object> statistics) {
        this.taskId = taskId;
        this.success = success;
        this.message = message;
        this.error = error;
        this.completionTime = Instant.now();
        this.executionDurationMs = executionDurationMs;
        this.processedItems = processedItems;
        this.failedItems = failedItems;
        this.statistics = statistics != null ? Map.copyOf(statistics) : Collections.emptyMap();
    }
    
    /**
     * 创建成功结果
     */
    public static TaskResult success(UUID taskId, long executionDurationMs, long processedItems) {
        return new TaskResult(taskId, executionDurationMs, processedItems, Collections.emptyMap());
    }
    
    /**
     * 创建成功结果（带统计信息）
     */
    public static TaskResult success(UUID taskId, long executionDurationMs, long processedItems, 
                                   Map<String, Object> statistics) {
        return new TaskResult(taskId, executionDurationMs, processedItems, statistics);
    }
    
    /**
     * 创建失败结果
     */
    public static TaskResult failure(UUID taskId, String errorMessage, long executionDurationMs) {
        return new TaskResult(taskId, errorMessage, null, executionDurationMs);
    }
    
    /**
     * 创建失败结果（带异常）
     */
    public static TaskResult failure(UUID taskId, String errorMessage, Throwable error, long executionDurationMs) {
        return new TaskResult(taskId, errorMessage, error, executionDurationMs);
    }
    
    /**
     * 创建取消结果
     */
    public static TaskResult cancelled(UUID taskId, long executionDurationMs) {
        return new TaskResult(taskId, false, "Task was cancelled", null, executionDurationMs, 
                             0, 0, Collections.emptyMap());
    }
    
    /**
     * 创建超时结果
     */
    public static TaskResult timeout(UUID taskId, long executionDurationMs) {
        return new TaskResult(taskId, false, "Task execution timeout", null, executionDurationMs, 
                             0, 0, Collections.emptyMap());
    }
    
    /**
     * 获取统计信息中的指定值
     */
    @SuppressWarnings("unchecked")
    public <T> T getStatistic(String key, Class<T> type) {
        Object value = statistics.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * 获取整数统计值
     */
    public Integer getIntStatistic(String key) {
        return getStatistic(key, Integer.class);
    }
    
    /**
     * 获取长整数统计值
     */
    public Long getLongStatistic(String key) {
        return getStatistic(key, Long.class);
    }
    
    /**
     * 获取浮点数统计值
     */
    public Double getDoubleStatistic(String key) {
        return getStatistic(key, Double.class);
    }
    
    /**
     * 获取字符串统计值
     */
    public String getStringStatistic(String key) {
        return getStatistic(key, String.class);
    }
    
    /**
     * 计算成功率
     */
    public double getSuccessRate() {
        long totalItems = processedItems + failedItems;
        if (totalItems == 0) {
            return 0.0;
        }
        return (double) processedItems / totalItems * 100.0;
    }
    
    // Getter方法
    
    public UUID getTaskId() {
        return taskId;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Instant getCompletionTime() {
        return completionTime;
    }
    
    public long getExecutionDurationMs() {
        return executionDurationMs;
    }
    
    public long getProcessedItems() {
        return processedItems;
    }
    
    public long getFailedItems() {
        return failedItems;
    }
    
    public Map<String, Object> getStatistics() {
        return statistics;
    }
    
    public Throwable getError() {
        return error;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TaskResult{");
        sb.append("taskId=").append(taskId);
        sb.append(", success=").append(success);
        sb.append(", message='").append(message).append('\'');
        sb.append(", duration=").append(executionDurationMs).append("ms");
        sb.append(", processed=").append(processedItems);
        if (failedItems > 0) {
            sb.append(", failed=").append(failedItems);
        }
        if (!statistics.isEmpty()) {
            sb.append(", statistics=").append(statistics);
        }
        if (error != null) {
            sb.append(", error=").append(error.getClass().getSimpleName());
        }
        sb.append('}');
        return sb.toString();
    }
}