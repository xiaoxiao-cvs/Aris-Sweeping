package com.arisweeping.tasks;

import com.arisweeping.core.Constants;
import com.arisweeping.tasks.models.TaskExecution;
import com.arisweeping.tasks.models.TaskResult;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 任务历史管理器
 * 
 * 负责任务执行历史存储、性能数据统计和历史数据清理机制
 */
public class TaskHistoryManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final int maxHistorySize;
    private final long dataRetentionDays;
    
    // 历史记录存储
    private final ConcurrentLinkedQueue<TaskHistoryRecord> historyQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<UUID, TaskHistoryRecord> historyMap = new ConcurrentHashMap<>();
    
    // 统计信息
    private final AtomicLong totalTasksExecuted = new AtomicLong(0);
    private final AtomicLong totalSuccessfulTasks = new AtomicLong(0);
    private final AtomicLong totalFailedTasks = new AtomicLong(0);
    private final AtomicLong totalExecutionTimeMs = new AtomicLong(0);
    
    // 性能统计
    private final Map<String, TaskTypeStatistics> taskTypeStats = new ConcurrentHashMap<>();
    
    // 清理任务
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "TaskHistoryManager-Cleanup");
        thread.setDaemon(true);
        return thread;
    });
    
    public TaskHistoryManager() {
        this(Constants.TaskManagement.MAX_TASK_HISTORY_SIZE, 7L); // 默认保留7天
    }
    
    public TaskHistoryManager(int maxHistorySize, long dataRetentionDays) {
        this.maxHistorySize = maxHistorySize;
        this.dataRetentionDays = dataRetentionDays;
        
        LOGGER.info("TaskHistoryManager initialized with max size: {}, retention: {} days", 
                   maxHistorySize, dataRetentionDays);
        
        // 启动定期清理任务
        startPeriodicCleanup();
    }
    
    /**
     * 记录任务执行历史
     */
    public void recordTask(TaskExecution execution, TaskResult result) {
        if (execution == null || result == null) {
            LOGGER.warn("Cannot record task with null execution or result");
            return;
        }
        
        UUID taskId = execution.getTaskId();
        
        try {
            // 创建历史记录
            TaskHistoryRecord record = new TaskHistoryRecord(
                taskId,
                execution.getTaskType(),
                execution.getPriority(),
                execution.getCreateTime(),
                execution.getStartTime(),
                execution.getEndTime(),
                result.isSuccess(),
                result.getExecutionDurationMs(),
                result.getProcessedItems(),
                result.getFailedItems(),
                result.getError() != null ? result.getError().getMessage() : null,
                result.getStatistics()
            );
            
            // 检查容量限制
            enforceCapacityLimit();
            
            // 添加到历史记录
            historyQueue.offer(record);
            historyMap.put(taskId, record);
            
            // 更新统计信息
            updateStatistics(record);
            
            LOGGER.debug("Recorded task history: {} - {}", taskId, execution.getTaskType());
            
        } catch (Exception e) {
            LOGGER.error("Failed to record task history for: {}", taskId, e);
        }
    }
    
    /**
     * 更新统计信息
     */
    private void updateStatistics(TaskHistoryRecord record) {
        totalTasksExecuted.incrementAndGet();
        totalExecutionTimeMs.addAndGet(record.getExecutionDurationMs());
        
        if (record.isSuccessful()) {
            totalSuccessfulTasks.incrementAndGet();
        } else {
            totalFailedTasks.incrementAndGet();
        }
        
        // 更新任务类型统计
        updateTaskTypeStatistics(record);
    }
    
    /**
     * 更新任务类型统计
     */
    private void updateTaskTypeStatistics(TaskHistoryRecord record) {
        String taskType = record.getTaskType();
        
        taskTypeStats.compute(taskType, (key, stats) -> {
            if (stats == null) {
                stats = new TaskTypeStatistics(taskType);
            }
            
            stats.incrementTotalCount();
            stats.addExecutionTime(record.getExecutionDurationMs());
            stats.addProcessedItems(record.getProcessedItems());
            
            if (record.isSuccessful()) {
                stats.incrementSuccessCount();
            } else {
                stats.incrementFailureCount();
            }
            
            return stats;
        });
    }
    
    /**
     * 获取任务历史记录
     */
    public Optional<TaskHistoryRecord> getTaskHistory(UUID taskId) {
        return Optional.ofNullable(historyMap.get(taskId));
    }
    
    /**
     * 获取最近的历史记录
     */
    public List<TaskHistoryRecord> getRecentHistory(int count) {
        return historyQueue.stream()
            .sorted((r1, r2) -> r2.getRecordTime().compareTo(r1.getRecordTime())) // 最新的在前
            .limit(Math.min(count, historyQueue.size()))
            .collect(Collectors.toList());
    }
    
    /**
     * 获取指定时间范围内的历史记录
     */
    public List<TaskHistoryRecord> getHistoryByTimeRange(Instant startTime, Instant endTime) {
        return historyQueue.stream()
            .filter(record -> {
                Instant recordTime = record.getRecordTime();
                return recordTime.isAfter(startTime) && recordTime.isBefore(endTime);
            })
            .sorted((r1, r2) -> r2.getRecordTime().compareTo(r1.getRecordTime()))
            .collect(Collectors.toList());
    }
    
    /**
     * 按任务类型获取历史记录
     */
    public List<TaskHistoryRecord> getHistoryByTaskType(String taskType) {
        return historyQueue.stream()
            .filter(record -> taskType.equals(record.getTaskType()))
            .sorted((r1, r2) -> r2.getRecordTime().compareTo(r1.getRecordTime()))
            .collect(Collectors.toList());
    }
    
    /**
     * 获取总体统计信息
     */
    public TaskHistoryStatistics getOverallStatistics() {
        return new TaskHistoryStatistics(
            totalTasksExecuted.get(),
            totalSuccessfulTasks.get(),
            totalFailedTasks.get(),
            totalExecutionTimeMs.get(),
            historyQueue.size(),
            calculateAverageExecutionTime(),
            calculateSuccessRate()
        );
    }
    
    /**
     * 获取任务类型统计信息
     */
    public Map<String, TaskTypeStatistics> getTaskTypeStatistics() {
        return new HashMap<>(taskTypeStats);
    }
    
    /**
     * 获取指定任务类型的统计信息
     */
    public Optional<TaskTypeStatistics> getTaskTypeStatistics(String taskType) {
        return Optional.ofNullable(taskTypeStats.get(taskType));
    }
    
    /**
     * 清除所有历史数据
     */
    public void clearAllHistory() {
        historyQueue.clear();
        historyMap.clear();
        taskTypeStats.clear();
        
        // 重置统计计数器
        totalTasksExecuted.set(0);
        totalSuccessfulTasks.set(0);
        totalFailedTasks.set(0);
        totalExecutionTimeMs.set(0);
        
        LOGGER.info("Cleared all task history data");
    }
    
    /**
     * 计算平均执行时间
     */
    private double calculateAverageExecutionTime() {
        long totalTasks = totalTasksExecuted.get();
        if (totalTasks == 0) {
            return 0.0;
        }
        return (double) totalExecutionTimeMs.get() / totalTasks;
    }
    
    /**
     * 计算成功率
     */
    private double calculateSuccessRate() {
        long totalTasks = totalTasksExecuted.get();
        if (totalTasks == 0) {
            return 0.0;
        }
        return ((double) totalSuccessfulTasks.get() / totalTasks) * 100.0;
    }
    
    /**
     * 强制执行容量限制
     */
    private void enforceCapacityLimit() {
        while (historyQueue.size() >= maxHistorySize) {
            TaskHistoryRecord oldest = historyQueue.poll();
            if (oldest != null) {
                historyMap.remove(oldest.getTaskId());
                LOGGER.debug("Removed oldest history record: {}", oldest.getTaskId());
            }
        }
    }
    
    /**
     * 启动定期清理任务
     */
    private void startPeriodicCleanup() {
        cleanupExecutor.scheduleAtFixedRate(
            this::cleanupExpiredData,
            1, // 初始延迟
            6, // 每6小时清理一次
            TimeUnit.HOURS
        );
        
        LOGGER.info("Started periodic cleanup task for expired history data");
    }
    
    /**
     * 清理过期数据
     */
    private void cleanupExpiredData() {
        try {
            Instant cutoffTime = Instant.now().minusSeconds(dataRetentionDays * 24 * 60 * 60);
            
            List<TaskHistoryRecord> expiredRecords = historyQueue.stream()
                .filter(record -> record.getRecordTime().isBefore(cutoffTime))
                .collect(Collectors.toList());
            
            for (TaskHistoryRecord record : expiredRecords) {
                historyQueue.remove(record);
                historyMap.remove(record.getTaskId());
            }
            
            if (!expiredRecords.isEmpty()) {
                LOGGER.info("Cleaned up {} expired history records", expiredRecords.size());
            }
            
        } catch (Exception e) {
            LOGGER.error("Error during history data cleanup", e);
        }
    }
    
    /**
     * 优雅关闭
     */
    public void shutdown() {
        LOGGER.info("Shutting down TaskHistoryManager...");
        
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cleanupExecutor.shutdownNow();
        }
        
        LOGGER.info("TaskHistoryManager shutdown completed");
    }
    
    /**
     * 任务历史记录
     */
    public static class TaskHistoryRecord {
        private final UUID taskId;
        private final String taskType;
        private final Object priority; // 使用 Object 避免依赖问题
        private final Instant createTime;
        private final Instant startTime;
        private final Instant endTime;
        private final boolean successful;
        private final long executionDurationMs;
        private final long processedItems;
        private final long failedItems;
        private final String errorMessage;
        private final Map<String, Object> statistics;
        private final Instant recordTime;
        
        public TaskHistoryRecord(UUID taskId, String taskType, Object priority,
                               Instant createTime, Instant startTime, Instant endTime,
                               boolean successful, long executionDurationMs,
                               long processedItems, long failedItems,
                               String errorMessage, Map<String, Object> statistics) {
            this.taskId = taskId;
            this.taskType = taskType;
            this.priority = priority;
            this.createTime = createTime;
            this.startTime = startTime;
            this.endTime = endTime;
            this.successful = successful;
            this.executionDurationMs = executionDurationMs;
            this.processedItems = processedItems;
            this.failedItems = failedItems;
            this.errorMessage = errorMessage;
            this.statistics = statistics != null ? new HashMap<>(statistics) : Collections.emptyMap();
            this.recordTime = Instant.now();
        }
        
        // Getters
        public UUID getTaskId() { return taskId; }
        public String getTaskType() { return taskType; }
        public Object getPriority() { return priority; }
        public Instant getCreateTime() { return createTime; }
        public Instant getStartTime() { return startTime; }
        public Instant getEndTime() { return endTime; }
        public boolean isSuccessful() { return successful; }
        public long getExecutionDurationMs() { return executionDurationMs; }
        public long getProcessedItems() { return processedItems; }
        public long getFailedItems() { return failedItems; }
        public String getErrorMessage() { return errorMessage; }
        public Map<String, Object> getStatistics() { return statistics; }
        public Instant getRecordTime() { return recordTime; }
        
        @Override
        public String toString() {
            return String.format("TaskHistoryRecord{id=%s, type='%s', successful=%s, duration=%dms, items=%d}",
                taskId, taskType, successful, executionDurationMs, processedItems);
        }
    }
    
    /**
     * 总体历史统计信息
     */
    public static class TaskHistoryStatistics {
        private final long totalTasksExecuted;
        private final long successfulTasks;
        private final long failedTasks;
        private final long totalExecutionTimeMs;
        private final int currentHistorySize;
        private final double averageExecutionTimeMs;
        private final double successRate;
        
        public TaskHistoryStatistics(long totalTasksExecuted, long successfulTasks, long failedTasks,
                                   long totalExecutionTimeMs, int currentHistorySize,
                                   double averageExecutionTimeMs, double successRate) {
            this.totalTasksExecuted = totalTasksExecuted;
            this.successfulTasks = successfulTasks;
            this.failedTasks = failedTasks;
            this.totalExecutionTimeMs = totalExecutionTimeMs;
            this.currentHistorySize = currentHistorySize;
            this.averageExecutionTimeMs = averageExecutionTimeMs;
            this.successRate = successRate;
        }
        
        // Getters
        public long getTotalTasksExecuted() { return totalTasksExecuted; }
        public long getSuccessfulTasks() { return successfulTasks; }
        public long getFailedTasks() { return failedTasks; }
        public long getTotalExecutionTimeMs() { return totalExecutionTimeMs; }
        public int getCurrentHistorySize() { return currentHistorySize; }
        public double getAverageExecutionTimeMs() { return averageExecutionTimeMs; }
        public double getSuccessRate() { return successRate; }
        
        @Override
        public String toString() {
            return String.format("TaskHistoryStatistics{total=%d, success=%d, failed=%d, avgTime=%.2fms, successRate=%.1f%%}",
                totalTasksExecuted, successfulTasks, failedTasks, averageExecutionTimeMs, successRate);
        }
    }
    
    /**
     * 任务类型统计信息
     */
    public static class TaskTypeStatistics {
        private final String taskType;
        private long totalCount = 0;
        private long successCount = 0;
        private long failureCount = 0;
        private long totalExecutionTimeMs = 0;
        private long totalProcessedItems = 0;
        
        public TaskTypeStatistics(String taskType) {
            this.taskType = taskType;
        }
        
        public synchronized void incrementTotalCount() { totalCount++; }
        public synchronized void incrementSuccessCount() { successCount++; }
        public synchronized void incrementFailureCount() { failureCount++; }
        public synchronized void addExecutionTime(long timeMs) { totalExecutionTimeMs += timeMs; }
        public synchronized void addProcessedItems(long items) { totalProcessedItems += items; }
        
        // Getters
        public String getTaskType() { return taskType; }
        public synchronized long getTotalCount() { return totalCount; }
        public synchronized long getSuccessCount() { return successCount; }
        public synchronized long getFailureCount() { return failureCount; }
        public synchronized long getTotalExecutionTimeMs() { return totalExecutionTimeMs; }
        public synchronized long getTotalProcessedItems() { return totalProcessedItems; }
        
        public synchronized double getSuccessRate() {
            return totalCount == 0 ? 0.0 : ((double) successCount / totalCount) * 100.0;
        }
        
        public synchronized double getAverageExecutionTime() {
            return totalCount == 0 ? 0.0 : (double) totalExecutionTimeMs / totalCount;
        }
        
        @Override
        public synchronized String toString() {
            return String.format("TaskTypeStatistics{type='%s', total=%d, success=%d, avgTime=%.2fms, successRate=%.1f%%}",
                taskType, totalCount, successCount, getAverageExecutionTime(), getSuccessRate());
        }
    }
}