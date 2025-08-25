package com.xiaoxiao.arissweeping.strategy;

import org.bukkit.entity.Entity;
import org.bukkit.World;
import java.util.List;
import java.util.Map;

/**
 * 清理策略接口
 * 定义不同清理策略的通用行为
 */
public interface CleanupStrategy {
    
    /**
     * 策略名称
     */
    String getName();
    
    /**
     * 策略描述
     */
    String getDescription();
    
    /**
     * 策略优先级（数值越小优先级越高）
     */
    int getPriority();
    
    /**
     * 检查策略是否适用于当前环境
     */
    boolean isApplicable(World world, List<Entity> entities);
    
    /**
     * 执行清理策略
     * 
     * @param world 目标世界
     * @param entities 待处理的实体列表
     * @return 清理结果统计
     */
    CleanupResult execute(World world, List<Entity> entities);
    
    /**
     * 执行清理策略（支持干运行模式）
     * 
     * @param world 目标世界
     * @param entities 待处理的实体列表
     * @param dryRun 是否为干运行模式
     * @return 清理结果统计
     */
    default CleanupResult execute(World world, List<Entity> entities, boolean dryRun) {
        return execute(world, entities);
    }
    
    /**
     * 获取策略配置参数
     */
    Map<String, Object> getConfiguration();
    
    /**
     * 更新策略配置
     */
    void updateConfiguration(Map<String, Object> config);
    
    /**
     * 验证策略配置是否有效
     */
    boolean validateConfiguration(Map<String, Object> config);
    
    /**
     * 获取策略统计信息
     */
    StrategyStatistics getStatistics();
    
    /**
     * 重置策略统计
     */
    void resetStatistics();
    
    /**
     * 检查策略是否启用
     */
    boolean isEnabled();
    
    /**
     * 检查单个实体是否应该被清理
     * @param entity 要检查的实体
     * @return 如果实体应该被清理则返回true
     */
    default boolean shouldCleanup(Entity entity) {
        return false;
    }
    
    /**
     * 清理结果类
     */
    class CleanupResult {
        private final int processedEntities;
        private final int removedEntities;
        private final long executionTime;
        private final boolean success;
        private final String errorMessage;
        private final Map<String, Integer> entityTypeStats;
        
        public CleanupResult(int processedEntities, int removedEntities, long executionTime, 
                           boolean success, String errorMessage, Map<String, Integer> entityTypeStats) {
            this.processedEntities = processedEntities;
            this.removedEntities = removedEntities;
            this.executionTime = executionTime;
            this.success = success;
            this.errorMessage = errorMessage;
            this.entityTypeStats = entityTypeStats;
        }
        
        public int getProcessedEntities() { return processedEntities; }
        public int getRemovedEntities() { return removedEntities; }
        public long getExecutionTime() { return executionTime; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public Map<String, Integer> getEntityTypeStats() { return entityTypeStats; }
        
        public double getEfficiency() {
            return processedEntities > 0 ? (double) removedEntities / processedEntities : 0.0;
        }
        
        // 兼容性方法
        public int getRemovedCount() { return removedEntities; }
        public int getProcessedCount() { return processedEntities; }
        public java.util.List<String> getErrors() {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                return java.util.Arrays.asList(errorMessage);
            }
            return java.util.Collections.emptyList();
        }
        
        public boolean hasErrors() { 
            return !success || errorMessage != null;
        }
        
        
        
        @Override
        public String toString() {
            return String.format("CleanupResult{processed=%d, removed=%d, time=%dms, success=%s}",
                processedEntities, removedEntities, executionTime, success);
        }
    }
    
    /**
     * 策略统计信息类
     */
    class StrategyStatistics {
        private int executionCount;
        private long totalExecutionTime;
        private int totalProcessedEntities;
        private int totalRemovedEntities;
        private int successCount;
        private int failureCount;
        private long lastExecutionTime;
        
        public void recordExecution(CleanupResult result) {
            executionCount++;
            totalExecutionTime += result.getExecutionTime();
            totalProcessedEntities += result.getProcessedEntities();
            totalRemovedEntities += result.getRemovedEntities();
            lastExecutionTime = System.currentTimeMillis();
            
            if (result.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
            }
        }
        
        public double getAverageExecutionTime() {
            return executionCount > 0 ? (double) totalExecutionTime / executionCount : 0.0;
        }
        
        public double getSuccessRate() {
            return executionCount > 0 ? (double) successCount / executionCount : 0.0;
        }
        
        public double getAverageEfficiency() {
            return totalProcessedEntities > 0 ? (double) totalRemovedEntities / totalProcessedEntities : 0.0;
        }
        
        public void reset() {
            executionCount = 0;
            totalExecutionTime = 0;
            totalProcessedEntities = 0;
            totalRemovedEntities = 0;
            successCount = 0;
            failureCount = 0;
            lastExecutionTime = 0;
        }
        
        // Getters
        public int getExecutionCount() { return executionCount; }
        public long getTotalExecutionTime() { return totalExecutionTime; }
        public int getTotalProcessedEntities() { return totalProcessedEntities; }
        public int getTotalRemovedEntities() { return totalRemovedEntities; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public long getLastExecutionTime() { return lastExecutionTime; }
        
        @Override
        public String toString() {
            return String.format("StrategyStatistics{executions=%d, avgTime=%.2fms, successRate=%.2f%%, efficiency=%.2f%%}",
                executionCount, getAverageExecutionTime(), getSuccessRate() * 100, getAverageEfficiency() * 100);
        }
    }
}