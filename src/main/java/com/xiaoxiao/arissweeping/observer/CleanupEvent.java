package com.xiaoxiao.arissweeping.observer;

import org.bukkit.entity.Entity;
import org.bukkit.World;
import org.bukkit.Chunk;

import java.util.List;
import java.util.Map;
import java.util.Collections;

/**
 * 清理事件类
 * 封装清理过程中的各种事件信息
 */
public abstract class CleanupEvent {
    
    private final long timestamp;
    private final String source;
    
    protected CleanupEvent(String source) {
        this.timestamp = System.currentTimeMillis();
        this.source = source;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public String getSource() {
        return source;
    }
    
    public abstract CleanupEventType getEventType();
    
    /**
     * 清理事件类型
     */
    public enum CleanupEventType {
        CLEANUP_STARTED,
        CLEANUP_COMPLETED,
        CLEANUP_FAILED,
        ENTITY_REMOVED,
        CHUNK_PROCESSED,
        WORLD_PROCESSED,
        STRATEGY_EXECUTED,
        PERFORMANCE_WARNING,
        CONFIGURATION_CHANGED,
        STATISTICS_UPDATED
    }
    
    /**
     * 清理开始事件
     */
    public static class CleanupStartedEvent extends CleanupEvent {
        private final World world;
        private final int totalEntities;
        private final String cleanupType;
        
        public CleanupStartedEvent(String source, World world, int totalEntities, String cleanupType) {
            super(source);
            this.world = world;
            this.totalEntities = totalEntities;
            this.cleanupType = cleanupType;
        }
        
        @Override
        public CleanupEventType getEventType() {
            return CleanupEventType.CLEANUP_STARTED;
        }
        
        public World getWorld() { return world; }
        public int getTotalEntities() { return totalEntities; }
        public String getCleanupType() { return cleanupType; }
    }
    
    /**
     * 清理完成事件
     */
    public static class CleanupCompletedEvent extends CleanupEvent {
        private final World world;
        private final int processedEntities;
        private final int removedEntities;
        private final long executionTime;
        private final Map<String, Integer> entityTypeStats;

        public CleanupCompletedEvent(String source, World world, int processedEntities, 
                                   int removedEntities, long executionTime, 
                                   Map<String, Integer> entityTypeStats) {
            super(source);
            this.world = world;
            this.processedEntities = processedEntities;
            this.removedEntities = removedEntities;
            this.executionTime = executionTime;
            this.entityTypeStats = entityTypeStats != null ? entityTypeStats : Collections.emptyMap();
        }
        
        // 兼容性构造器，用于测试
        public CleanupCompletedEvent(String strategyName, World world, int entitiesProcessed, 
                                   int entitiesRemoved, long executionTime, long timestamp) {
            super(strategyName);
            this.world = world;
            this.processedEntities = entitiesProcessed;
            this.removedEntities = entitiesRemoved;
            this.executionTime = executionTime;
            this.entityTypeStats = Collections.emptyMap();
        }
        
        @Override
        public CleanupEventType getEventType() {
            return CleanupEventType.CLEANUP_COMPLETED;
        }
        
        public World getWorld() { return world; }
        public int getProcessedEntities() { return processedEntities; }
        public int getRemovedEntities() { return removedEntities; }
        public long getExecutionTime() { return executionTime; }
        public Map<String, Integer> getEntityTypeStats() { return entityTypeStats; }
        
        public double getEfficiency() {
            return processedEntities > 0 ? (double) removedEntities / processedEntities : 0.0;
        }
        
        // 兼容性方法，用于测试
        public String getStrategyName() {
            return getSource();
        }
        
        public int getEntitiesProcessed() {
            return processedEntities;
        }
        
        public int getEntitiesRemoved() {
            return removedEntities;
        }
        
        public double getRemovalRate() {
            return processedEntities > 0 ? (double) removedEntities / processedEntities : 0.0;
        }
        
        public double getEntitiesPerSecond() {
            return executionTime > 0 ? (double) processedEntities * 1000.0 / executionTime : 0.0;
        }
    }
    
    /**
     * 清理失败事件
     */
    public static class CleanupFailedEvent extends CleanupEvent {
        private final World world;
        private final String errorMessage;
        private final Throwable cause;
        private final int processedEntities;
        
        public CleanupFailedEvent(String source, World world, String errorMessage, 
                                Throwable cause, int processedEntities) {
            super(source);
            this.world = world;
            this.errorMessage = errorMessage;
            this.cause = cause;
            this.processedEntities = processedEntities;
        }
        
        @Override
        public CleanupEventType getEventType() {
            return CleanupEventType.CLEANUP_FAILED;
        }
        
        public World getWorld() { return world; }
        public String getErrorMessage() { return errorMessage; }
        public Throwable getCause() { return cause; }
        public int getProcessedEntities() { return processedEntities; }
    }
    
    /**
     * 实体移除事件
     */
    public static class EntityRemovedEvent extends CleanupEvent {
        private final Entity entity;
        private final String reason;
        private final String strategy;

        public EntityRemovedEvent(String source, Entity entity, String reason, String strategy) {
            super(source);
            this.entity = entity;
            this.reason = reason;
            this.strategy = strategy;
        }
        
        // 兼容性构造器，用于测试
        public EntityRemovedEvent(Entity entity, String strategyName, String reason, long timestamp) {
            super(strategyName);
            this.entity = entity;
            this.reason = reason;
            this.strategy = strategyName;
        }
        
        @Override
        public CleanupEventType getEventType() {
            return CleanupEventType.ENTITY_REMOVED;
        }
        
        public Entity getEntity() { return entity; }
        public String getReason() { return reason; }
        public String getStrategy() { return strategy; }
        
        // 兼容性方法，用于测试
        public String getStrategyName() {
            return strategy;
        }
        
        public java.util.UUID getEntityId() {
            return entity.getUniqueId();
        }
        
        public org.bukkit.entity.EntityType getEntityType() {
            return entity.getType();
        }
        
        public World getWorld() {
            return entity.getWorld();
        }
    }
    
    /**
     * 区块处理事件
     */
    public static class ChunkProcessedEvent extends CleanupEvent {
        private final Chunk chunk;
        private final int entitiesFound;
        private final int entitiesRemoved;
        private final long processingTime;
        
        public ChunkProcessedEvent(String source, Chunk chunk, int entitiesFound, 
                                 int entitiesRemoved, long processingTime) {
            super(source);
            this.chunk = chunk;
            this.entitiesFound = entitiesFound;
            this.entitiesRemoved = entitiesRemoved;
            this.processingTime = processingTime;
        }
        
        @Override
        public CleanupEventType getEventType() {
            return CleanupEventType.CHUNK_PROCESSED;
        }
        
        public Chunk getChunk() { return chunk; }
        public int getEntitiesFound() { return entitiesFound; }
        public int getEntitiesRemoved() { return entitiesRemoved; }
        public long getProcessingTime() { return processingTime; }
    }
    
    /**
     * 世界处理事件
     */
    public static class WorldProcessedEvent extends CleanupEvent {
        private final World world;
        private final int chunksProcessed;
        private final int totalEntitiesFound;
        private final int totalEntitiesRemoved;
        private final long processingTime;
        
        public WorldProcessedEvent(String source, World world, int chunksProcessed, 
                                 int totalEntitiesFound, int totalEntitiesRemoved, 
                                 long processingTime) {
            super(source);
            this.world = world;
            this.chunksProcessed = chunksProcessed;
            this.totalEntitiesFound = totalEntitiesFound;
            this.totalEntitiesRemoved = totalEntitiesRemoved;
            this.processingTime = processingTime;
        }
        
        @Override
        public CleanupEventType getEventType() {
            return CleanupEventType.WORLD_PROCESSED;
        }
        
        public World getWorld() { return world; }
        public int getChunksProcessed() { return chunksProcessed; }
        public int getTotalEntitiesFound() { return totalEntitiesFound; }
        public int getTotalEntitiesRemoved() { return totalEntitiesRemoved; }
        public long getProcessingTime() { return processingTime; }
    }
    
    /**
     * 策略执行事件
     */
    public static class StrategyExecutedEvent extends CleanupEvent {
        private final String strategyName;
        private final World world;
        private final int processedEntities;
        private final int removedEntities;
        private final long executionTime;
        private final boolean success;
        
        public StrategyExecutedEvent(String source, String strategyName, World world, 
                                   int processedEntities, int removedEntities, 
                                   long executionTime, boolean success) {
            super(source);
            this.strategyName = strategyName;
            this.world = world;
            this.processedEntities = processedEntities;
            this.removedEntities = removedEntities;
            this.executionTime = executionTime;
            this.success = success;
        }
        
        @Override
        public CleanupEventType getEventType() {
            return CleanupEventType.STRATEGY_EXECUTED;
        }
        
        public String getStrategyName() { return strategyName; }
        public World getWorld() { return world; }
        public int getProcessedEntities() { return processedEntities; }
        public int getRemovedEntities() { return removedEntities; }
        public long getExecutionTime() { return executionTime; }
        public boolean isSuccess() { return success; }
    }
    
    /**
     * 性能警告事件
     */
    public static class PerformanceWarningEvent extends CleanupEvent {
        private final String warningType;
        private final String message;
        private final double currentValue;
        private final double threshold;
        private final Map<String, Object> metrics;
        
        public PerformanceWarningEvent(String source, String warningType, String message, 
                                     double currentValue, double threshold, 
                                     Map<String, Object> metrics) {
            super(source);
            this.warningType = warningType;
            this.message = message;
            this.currentValue = currentValue;
            this.threshold = threshold;
            this.metrics = metrics != null ? metrics : Collections.emptyMap();
        }
        
        @Override
        public CleanupEventType getEventType() {
            return CleanupEventType.PERFORMANCE_WARNING;
        }
        
        public String getWarningType() { return warningType; }
        public String getMessage() { return message; }
        public double getCurrentValue() { return currentValue; }
        public double getThreshold() { return threshold; }
        public Map<String, Object> getMetrics() { return metrics; }
    }
    
    /**
     * 配置变更事件
     */
    public static class ConfigurationChangedEvent extends CleanupEvent {
        private final String configSection;
        private final Map<String, Object> oldValues;
        private final Map<String, Object> newValues;
        private final String changeReason;
        
        public ConfigurationChangedEvent(String source, String configSection, 
                                       Map<String, Object> oldValues, 
                                       Map<String, Object> newValues, 
                                       String changeReason) {
            super(source);
            this.configSection = configSection;
            this.oldValues = oldValues != null ? oldValues : Collections.emptyMap();
            this.newValues = newValues != null ? newValues : Collections.emptyMap();
            this.changeReason = changeReason;
        }
        
        @Override
        public CleanupEventType getEventType() {
            return CleanupEventType.CONFIGURATION_CHANGED;
        }
        
        public String getConfigSection() { return configSection; }
        public Map<String, Object> getOldValues() { return oldValues; }
        public Map<String, Object> getNewValues() { return newValues; }
        public String getChangeReason() { return changeReason; }
    }
    
    /**
     * 统计更新事件
     */
    public static class StatisticsUpdatedEvent extends CleanupEvent {
        private final String statisticsType;
        private final Map<String, Object> statistics;
        private final long updateInterval;
        
        public StatisticsUpdatedEvent(String source, String statisticsType, 
                                    Map<String, Object> statistics, long updateInterval) {
            super(source);
            this.statisticsType = statisticsType;
            this.statistics = statistics != null ? statistics : Collections.emptyMap();
            this.updateInterval = updateInterval;
        }
        
        @Override
        public CleanupEventType getEventType() {
            return CleanupEventType.STATISTICS_UPDATED;
        }
        
        public String getStatisticsType() { return statisticsType; }
        public Map<String, Object> getStatistics() { return statistics; }
        public long getUpdateInterval() { return updateInterval; }
    }
    
    @Override
    public String toString() {
        return String.format("%s{type=%s, source='%s', timestamp=%d}", 
            getClass().getSimpleName(), getEventType(), source, timestamp);
    }
}