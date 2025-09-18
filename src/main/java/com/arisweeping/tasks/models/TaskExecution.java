package com.arisweeping.tasks.models;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.arisweeping.tasks.enums.TaskPriority;
import com.arisweeping.tasks.enums.TaskStatus;

/**
 * 任务执行信息
 * 
 * 包含任务执行过程中的所有状态信息和元数据
 */
public class TaskExecution {
    
    private final UUID taskId;
    private final long sequence;
    private final String taskType;
    private final TaskPriority priority;
    private final Instant createTime;
    private final Object requestData;
    
    private volatile TaskStatus status;
    private volatile Instant startTime;
    private volatile Instant endTime;
    private volatile String errorMessage;
    private volatile CompletableFuture<TaskResult> future;
    
    // 执行统计信息
    private volatile long processedItems;
    private volatile long totalItems;
    private volatile double progressPercentage;
    
    /**
     * 构造函数
     * 
     * @param taskId 任务唯一标识符
     * @param sequence 任务序列号
     * @param taskType 任务类型
     * @param priority 任务优先级
     * @param requestData 请求数据
     */
    public TaskExecution(UUID taskId, long sequence, String taskType, TaskPriority priority, Object requestData) {
        this.taskId = taskId;
        this.sequence = sequence;
        this.taskType = taskType;
        this.priority = priority;
        this.requestData = requestData;
        this.createTime = Instant.now();
        this.status = TaskStatus.PENDING;
        this.processedItems = 0;
        this.totalItems = 0;
        this.progressPercentage = 0.0;
    }
    
    /**
     * 标记任务开始执行
     */
    public void markStarted() {
        this.status = TaskStatus.RUNNING;
        this.startTime = Instant.now();
    }
    
    /**
     * 标记任务完成
     */
    public void markCompleted() {
        this.status = TaskStatus.COMPLETED;
        this.endTime = Instant.now();
        this.progressPercentage = 100.0;
    }
    
    /**
     * 标记任务失败
     */
    public void markFailed(String errorMessage) {
        this.status = TaskStatus.FAILED;
        this.endTime = Instant.now();
        this.errorMessage = errorMessage;
    }
    
    /**
     * 标记任务取消
     */
    public void markCancelled() {
        this.status = TaskStatus.CANCELLED;
        this.endTime = Instant.now();
    }
    
    /**
     * 标记任务暂停
     */
    public void markPaused() {
        this.status = TaskStatus.PAUSED;
    }
    
    /**
     * 标记任务超时
     */
    public void markTimeout() {
        this.status = TaskStatus.TIMEOUT;
        this.endTime = Instant.now();
        this.errorMessage = "Task execution timeout";
    }
    
    /**
     * 更新任务进度
     */
    public void updateProgress(long processedItems, long totalItems) {
        this.processedItems = processedItems;
        this.totalItems = totalItems;
        if (totalItems > 0) {
            this.progressPercentage = (double) processedItems / totalItems * 100.0;
        }
    }
    
    /**
     * 获取任务执行时长（毫秒）
     */
    public long getExecutionDurationMs() {
        if (startTime == null) {
            return 0;
        }
        Instant end = endTime != null ? endTime : Instant.now();
        return end.toEpochMilli() - startTime.toEpochMilli();
    }
    
    /**
     * 检查任务是否可以取消
     */
    public boolean isCancellable() {
        return status.isCancellable();
    }
    
    /**
     * 检查任务是否已完成
     */
    public boolean isFinished() {
        return status.isFinished();
    }
    
    // Getter方法
    
    public UUID getTaskId() {
        return taskId;
    }
    
    public long getSequence() {
        return sequence;
    }
    
    public String getTaskType() {
        return taskType;
    }
    
    public TaskPriority getPriority() {
        return priority;
    }
    
    public Instant getCreateTime() {
        return createTime;
    }
    
    public Object getRequestData() {
        return requestData;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public CompletableFuture<TaskResult> getFuture() {
        return future;
    }
    
    public void setFuture(CompletableFuture<TaskResult> future) {
        this.future = future;
    }
    
    public long getProcessedItems() {
        return processedItems;
    }
    
    public long getTotalItems() {
        return totalItems;
    }
    
    public double getProgressPercentage() {
        return progressPercentage;
    }
    
    @Override
    public String toString() {
        return "TaskExecution{" +
                "taskId=" + taskId +
                ", sequence=" + sequence +
                ", taskType='" + taskType + '\'' +
                ", priority=" + priority +
                ", status=" + status +
                ", progress=" + String.format("%.1f%%", progressPercentage) +
                ", duration=" + getExecutionDurationMs() + "ms" +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TaskExecution that = (TaskExecution) obj;
        return taskId.equals(that.taskId);
    }
    
    @Override
    public int hashCode() {
        return taskId.hashCode();
    }
}