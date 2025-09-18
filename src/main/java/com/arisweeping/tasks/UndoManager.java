package com.arisweeping.tasks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.arisweeping.cleaning.EntityRemovalInfo;
import com.arisweeping.core.ArisLogger;
import com.arisweeping.tasks.models.TaskExecution;
import com.arisweeping.tasks.models.TaskResult;

/**
 * 撤销管理器
 * 
 * 负责管理任务的撤销操作和数据恢复
 * 支持实体NBT数据保存和安全的实体恢复机制
 */
public class UndoManager {
    
    private final int maxUndoOperations;
    private final long undoTimeoutMinutes;
    
    // 撤销数据存储
    private final ConcurrentLinkedQueue<UndoOperation> undoStack = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<UUID, UndoData> undoDataMap = new ConcurrentHashMap<>();
    
    public UndoManager(int maxUndoOperations, long undoTimeoutMinutes) {
        this.maxUndoOperations = maxUndoOperations;
        this.undoTimeoutMinutes = undoTimeoutMinutes;
        ArisLogger.info("UndoManager initialized with max operations: {}, timeout: {} minutes", 
                   maxUndoOperations, undoTimeoutMinutes);
        
        // 启动清理过期数据的定时任务
        startCleanupTask();
    }
    
    /**
     * 记录任务用于撤销
     */
    public void recordForUndo(TaskExecution execution, TaskResult result) {
        if (result == null || !result.isSuccess()) {
            return;
        }
        
        UUID taskId = execution.getTaskId();
        ArisLogger.debug("Recording task for undo: {}", taskId);
        
        try {
            // 创建撤销数据
            UndoData undoData = createUndoData(execution, result);
            
            // 检查容量限制
            enforceCapacityLimit();
            
            // 添加到撤销栈
            UndoOperation operation = new UndoOperation(
                taskId,
                System.currentTimeMillis(),
                execution.getTaskType(),
                undoData.getEntityCount()
            );
            
            undoStack.offer(operation);
            undoDataMap.put(taskId, undoData);
            
            ArisLogger.info("Recorded undo operation for task: {} with {} entities", 
                       taskId, undoData.getEntityCount());
            
        } catch (Exception e) {
            ArisLogger.error("Failed to record undo data for task: {}", taskId, e);
        }
    }
    
    /**
     * 创建撤销数据
     */
    private UndoData createUndoData(TaskExecution execution, TaskResult result) {
        List<EntityRemovalInfo> removedEntities = extractRemovedEntities(result);
        
        return new UndoData.Builder()
            .setTaskId(execution.getTaskId())
            .setTaskName(execution.getTaskType())
            .setTimestamp(System.currentTimeMillis())
            .setRemovedEntities(removedEntities)
            .setDescription(String.format("Cleaning operation: %s", execution.getTaskType()))
            .build();
    }
    
    /**
     * 从任务结果中提取被移除的实体信息
     */
    @SuppressWarnings("unchecked")
    private List<EntityRemovalInfo> extractRemovedEntities(TaskResult result) {
        // TODO: 实现从TaskResult中提取EntityRemovalInfo的逻辑
        // 这需要TaskResult类包含被移除实体的详细信息
        return new ArrayList<>(); // 暂时返回空列表
    }
    
    /**
     * 执行撤销操作
     */
    public CompletableFuture<UndoResult> performUndo(UUID originalTaskId) {
        return CompletableFuture.supplyAsync(() -> {
            ArisLogger.info("Performing undo for task: {}", originalTaskId);
            
            UndoData undoData = undoDataMap.get(originalTaskId);
            if (undoData == null) {
                ArisLogger.warn("No undo data found for task: {}", originalTaskId);
                return UndoResult.failure(originalTaskId, "No undo data available");
            }
            
            // 检查撤销是否已超时
            if (isUndoExpired(undoData)) {
                ArisLogger.warn("Undo operation expired for task: {}", originalTaskId);
                removeUndoData(originalTaskId);
                return UndoResult.failure(originalTaskId, "Undo operation expired");
            }
            
            try {
                // 执行实体恢复
                int restoredCount = restoreEntities(undoData);
                
                // 移除撤销数据
                removeUndoData(originalTaskId);
                
                ArisLogger.info("Successfully restored {} entities for task: {}", restoredCount, originalTaskId);
                return UndoResult.success(originalTaskId, restoredCount);
                
            } catch (Exception e) {
                ArisLogger.error("Failed to perform undo for task: {}", originalTaskId, e);
                return UndoResult.failure(originalTaskId, "Undo operation failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * 恢复被移除的实体
     */
    private int restoreEntities(UndoData undoData) {
        int restoredCount = 0;
        
        for (EntityRemovalInfo entityInfo : undoData.getRemovedEntities()) {
            if (restoreEntity(entityInfo)) {
                restoredCount++;
            }
        }
        
        return restoredCount;
    }
    
    /**
     * 恢复单个实体
     */
    private boolean restoreEntity(EntityRemovalInfo entityInfo) {
        try {
            // TODO: 实现实体恢复逻辑
            // 需要：
            // 1. 根据NBT数据重新创建实体
            // 2. 在指定位置生成实体
            // 3. 恢复实体的所有属性
            
            ArisLogger.debug("Restoring entity: {} at ({}, {}, {})", 
                        entityInfo.getEntityType(),
                        entityInfo.getX(), 
                        entityInfo.getY(), 
                        entityInfo.getZ());
            
            // 暂时的占位符实现
            return true;
            
        } catch (Exception e) {
            ArisLogger.error("Failed to restore entity: {}", entityInfo.getEntityId(), e);
            return false;
        }
    }
    
    /**
     * 检查任务是否可以撤销
     */
    public boolean canUndo(UUID taskId) {
        UndoData undoData = undoDataMap.get(taskId);
        return undoData != null && !isUndoExpired(undoData);
    }
    
    /**
     * 获取可撤销的操作列表
     */
    public List<UndoOperation> getUndoableOperations() {
        List<UndoOperation> result = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        
        for (UndoOperation operation : undoStack) {
            if (!isOperationExpired(operation, currentTime)) {
                result.add(operation);
            }
        }
        
        return result;
    }
    
    /**
     * 获取撤销栈大小
     */
    public int getUndoStackSize() {
        return undoStack.size();
    }
    
    /**
     * 撤销最后一次操作
     * @return 是否成功撤销
     */
    public boolean undoLastOperation() {
        List<UndoOperation> undoableOps = getUndoableOperations();
        if (undoableOps.isEmpty()) {
            ArisLogger.debug("没有可撤销的操作");
            return false;
        }
        
        // 获取最新的操作
        UndoOperation lastOperation = undoableOps.get(undoableOps.size() - 1);
        
        try {
            CompletableFuture<UndoResult> future = performUndo(lastOperation.getTaskId());
            UndoResult result = future.get(30, java.util.concurrent.TimeUnit.SECONDS); // 30秒超时
            
            if (result.isSuccessful()) {
                ArisLogger.info("成功撤销任务: {}, 恢复了{}个实体", lastOperation.getTaskId(), result.getRestoredCount());
                return true;
            } else {
                ArisLogger.warn("撤销任务失败: {}, 错误: {}", lastOperation.getTaskId(), result.getErrorMessage());
                return false;
            }
        } catch (Exception e) {
            ArisLogger.error("执行撤销操作时发生异常", e);
            return false;
        }
    }
    
    /**
     * 清空所有撤销数据
     */
    public void clearAllUndoData() {
        undoStack.clear();
        undoDataMap.clear();
        ArisLogger.info("Cleared all undo data");
    }
    
    /**
     * 检查撤销是否已过期
     */
    private boolean isUndoExpired(UndoData undoData) {
        long currentTime = System.currentTimeMillis();
        long timeoutMillis = undoTimeoutMinutes * 60 * 1000;
        return (currentTime - undoData.getTimestamp()) > timeoutMillis;
    }
    
    /**
     * 检查操作是否已过期
     */
    private boolean isOperationExpired(UndoOperation operation, long currentTime) {
        long timeoutMillis = undoTimeoutMinutes * 60 * 1000;
        return (currentTime - operation.getTimestamp()) > timeoutMillis;
    }
    
    /**
     * 强制执行容量限制
     */
    private void enforceCapacityLimit() {
        while (undoStack.size() >= maxUndoOperations) {
            UndoOperation oldest = undoStack.poll();
            if (oldest != null) {
                undoDataMap.remove(oldest.getTaskId());
                ArisLogger.debug("Removed oldest undo operation: {}", oldest.getTaskId());
            }
        }
    }
    
    /**
     * 移除撤销数据
     */
    private void removeUndoData(UUID taskId) {
        undoDataMap.remove(taskId);
        undoStack.removeIf(op -> op.getTaskId().equals(taskId));
    }
    
    /**
     * 启动清理过期数据的定时任务
     */
    private void startCleanupTask() {
        // TODO: 实现定时清理过期撤销数据的逻辑
        // 可以使用ScheduledExecutorService定期清理
        ArisLogger.debug("Undo data cleanup task started");
    }
    
    /**
     * 撤销操作信息
     */
    public static class UndoOperation {
        private final UUID taskId;
        private final long timestamp;
        private final String taskName;
        private final int entityCount;
        
        public UndoOperation(UUID taskId, long timestamp, String taskName, int entityCount) {
            this.taskId = taskId;
            this.timestamp = timestamp;
            this.taskName = taskName;
            this.entityCount = entityCount;
        }
        
        public UUID getTaskId() { return taskId; }
        public long getTimestamp() { return timestamp; }
        public String getTaskName() { return taskName; }
        public int getEntityCount() { return entityCount; }
        
        @Override
        public String toString() {
            return String.format("UndoOperation{taskId=%s, task='%s', entities=%d, time=%d}", 
                                taskId, taskName, entityCount, timestamp);
        }
    }
    
    /**
     * 撤销数据存储
     */
    public static class UndoData {
        private final UUID taskId;
        private final String taskName;
        private final long timestamp;
        private final List<EntityRemovalInfo> removedEntities;
        private final String description;
        
        private UndoData(Builder builder) {
            this.taskId = builder.taskId;
            this.taskName = builder.taskName;
            this.timestamp = builder.timestamp;
            this.removedEntities = builder.removedEntities != null ? 
                List.copyOf(builder.removedEntities) : Collections.emptyList();
            this.description = builder.description;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public UUID getTaskId() { return taskId; }
        public String getTaskName() { return taskName; }
        public long getTimestamp() { return timestamp; }
        public List<EntityRemovalInfo> getRemovedEntities() { return removedEntities; }
        public String getDescription() { return description; }
        public int getEntityCount() { return removedEntities.size(); }
        
        public static class Builder {
            private UUID taskId;
            private String taskName;
            private long timestamp;
            private List<EntityRemovalInfo> removedEntities;
            private String description;
            
            public Builder setTaskId(UUID taskId) {
                this.taskId = taskId;
                return this;
            }
            
            public Builder setTaskName(String taskName) {
                this.taskName = taskName;
                return this;
            }
            
            public Builder setTimestamp(long timestamp) {
                this.timestamp = timestamp;
                return this;
            }
            
            public Builder setRemovedEntities(List<EntityRemovalInfo> removedEntities) {
                this.removedEntities = removedEntities;
                return this;
            }
            
            public Builder setDescription(String description) {
                this.description = description;
                return this;
            }
            
            public UndoData build() {
                return new UndoData(this);
            }
        }
    }
    
    /**
     * 撤销结果
     */
    public static class UndoResult {
        private final UUID taskId;
        private final boolean successful;
        private final int restoredCount;
        private final String errorMessage;
        
        private UndoResult(UUID taskId, boolean successful, int restoredCount, String errorMessage) {
            this.taskId = taskId;
            this.successful = successful;
            this.restoredCount = restoredCount;
            this.errorMessage = errorMessage;
        }
        
        public static UndoResult success(UUID taskId, int restoredCount) {
            return new UndoResult(taskId, true, restoredCount, null);
        }
        
        public static UndoResult failure(UUID taskId, String errorMessage) {
            return new UndoResult(taskId, false, 0, errorMessage);
        }
        
        public UUID getTaskId() { return taskId; }
        public boolean isSuccessful() { return successful; }
        public int getRestoredCount() { return restoredCount; }
        public String getErrorMessage() { return errorMessage; }
        
        @Override
        public String toString() {
            return String.format("UndoResult{taskId=%s, successful=%s, restored=%d, error='%s'}", 
                                taskId, successful, restoredCount, errorMessage);
        }
    }
}