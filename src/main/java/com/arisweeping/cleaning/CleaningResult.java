package com.arisweeping.cleaning;

import java.util.*;

/**
 * 清理结果类
 * 
 * 封装清理操作的结果信息
 */
public class CleaningResult {
    private final UUID taskId;
    private final long startTime;
    private final long endTime;
    private final long duration;
    private final boolean successful;
    private final String errorMessage;
    private final String level;
    private final int itemsRemoved;
    private final int animalsRemoved;
    private final List<EntityRemovalInfo> removedEntities;
    
    private CleaningResult(Builder builder) {
        this.taskId = builder.taskId;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.duration = builder.duration;
        this.successful = builder.successful;
        this.errorMessage = builder.errorMessage;
        this.level = builder.level;
        this.itemsRemoved = builder.itemsRemoved;
        this.animalsRemoved = builder.animalsRemoved;
        this.removedEntities = builder.removedEntities != null ? 
            List.copyOf(builder.removedEntities) : Collections.emptyList();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public UUID getTaskId() { return taskId; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public long getDuration() { return duration; }
    public boolean isSuccessful() { return successful; }
    public String getErrorMessage() { return errorMessage; }
    public String getLevel() { return level; }
    public int getItemsRemoved() { return itemsRemoved; }
    public int getAnimalsRemoved() { return animalsRemoved; }
    public List<EntityRemovalInfo> getRemovedEntities() { return removedEntities; }
    
    @Override
    public String toString() {
        return String.format("CleaningResult{taskId=%s, successful=%s, items=%d, animals=%d, duration=%dms}",
            taskId, successful, itemsRemoved, animalsRemoved, duration);
    }
    
    public static class Builder {
        private UUID taskId;
        private long startTime;
        private long endTime;
        private long duration;
        private boolean successful;
        private String errorMessage;
        private String level;
        private int itemsRemoved;
        private int animalsRemoved;
        private List<EntityRemovalInfo> removedEntities = new ArrayList<>();
        
        public Builder setTaskId(UUID taskId) {
            this.taskId = taskId;
            return this;
        }
        
        public Builder setStartTime(long startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public Builder setEndTime(long endTime) {
            this.endTime = endTime;
            return this;
        }
        
        public Builder setDuration(long duration) {
            this.duration = duration;
            return this;
        }
        
        public Builder setSuccessful(boolean successful) {
            this.successful = successful;
            return this;
        }
        
        public Builder setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public Builder setLevel(String level) {
            this.level = level;
            return this;
        }
        
        public Builder addItemsRemoved(int count) {
            this.itemsRemoved += count;
            return this;
        }
        
        public Builder addAnimalsRemoved(int count) {
            this.animalsRemoved += count;
            return this;
        }
        
        public Builder addRemovedEntities(List<EntityRemovalInfo> entities) {
            this.removedEntities.addAll(entities);
            return this;
        }
        
        public CleaningResult build() {
            return new CleaningResult(this);
        }
    }
}