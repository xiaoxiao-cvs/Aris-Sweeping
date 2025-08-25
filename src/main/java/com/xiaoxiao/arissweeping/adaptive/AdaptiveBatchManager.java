package com.xiaoxiao.arissweeping.adaptive;

import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.TpsMonitor;
import com.xiaoxiao.arissweeping.util.LoggerUtil;
import org.bukkit.Bukkit;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自适应批处理管理器
 * 根据服务器性能动态调整批处理参数
 */
public class AdaptiveBatchManager {
    
    private final ModConfig config;
    private final TpsMonitor tpsMonitor;
    
    // 批处理参数
    private final AtomicInteger currentBatchSize = new AtomicInteger(50);
    private final AtomicInteger currentProcessingDelay = new AtomicInteger(1);
    private final AtomicInteger currentChunkLimit = new AtomicInteger(10);
    
    // 性能指标
    private final AtomicLong lastAdjustmentTime = new AtomicLong(0);
    private final AtomicInteger consecutiveGoodPerformance = new AtomicInteger(0);
    private final AtomicInteger consecutivePoorPerformance = new AtomicInteger(0);
    
    // 配置常量
    private static final int MIN_BATCH_SIZE = 10;
    private static final int MAX_BATCH_SIZE = 200;
    private static final int MIN_PROCESSING_DELAY = 1;
    private static final int MAX_PROCESSING_DELAY = 10;
    private static final int MIN_CHUNK_LIMIT = 5;
    private static final int MAX_CHUNK_LIMIT = 50;
    
    // 性能阈值
    private static final double GOOD_TPS_THRESHOLD = 18.0;
    private static final double POOR_TPS_THRESHOLD = 15.0;
    private static final long ADJUSTMENT_COOLDOWN = 30000; // 30秒
    private static final int PERFORMANCE_STREAK_THRESHOLD = 3;
    
    public AdaptiveBatchManager(ModConfig config, TpsMonitor tpsMonitor) {
        this.config = config;
        this.tpsMonitor = tpsMonitor;
        initializeParameters();
    }
    
    /**
     * 初始化批处理参数
     */
    private void initializeParameters() {
        // 根据配置设置初始值
        currentBatchSize.set(Math.max(MIN_BATCH_SIZE, Math.min(MAX_BATCH_SIZE, 50)));
        currentProcessingDelay.set(Math.max(MIN_PROCESSING_DELAY, Math.min(MAX_PROCESSING_DELAY, 1)));
        currentChunkLimit.set(Math.max(MIN_CHUNK_LIMIT, Math.min(MAX_CHUNK_LIMIT, 10)));
        
        LoggerUtil.info("AdaptiveBatchManager", "初始化批处理参数 - 批大小: %d, 处理延迟: %d, 区块限制: %d", 
                currentBatchSize.get(), currentProcessingDelay.get(), currentChunkLimit.get());
    }
    
    /**
     * 根据当前服务器性能调整批处理参数
     */
    public void adjustParameters() {
        long currentTime = System.currentTimeMillis();
        
        // 检查调整冷却时间
        if (currentTime - lastAdjustmentTime.get() < ADJUSTMENT_COOLDOWN) {
            return;
        }
        
        double currentTps = tpsMonitor.getCurrentTps();
        int playerCount = Bukkit.getOnlinePlayers().size();
        
        // 评估当前性能
        PerformanceLevel performanceLevel = evaluatePerformance(currentTps, playerCount);
        
        // 根据性能调整参数
        boolean adjusted = adjustBasedOnPerformance(performanceLevel);
        
        if (adjusted) {
            lastAdjustmentTime.set(currentTime);
            LoggerUtil.info("AdaptiveBatchManager", "性能调整完成 - TPS: %.2f, 性能等级: %s, 批大小: %d, 延迟: %d, 区块限制: %d",
                    currentTps, performanceLevel, currentBatchSize.get(), currentProcessingDelay.get(), currentChunkLimit.get());
        }
    }
    
    /**
     * 评估当前服务器性能
     */
    private PerformanceLevel evaluatePerformance(double tps, int playerCount) {
        // 基于TPS的基础评估
        if (tps >= GOOD_TPS_THRESHOLD) {
            consecutiveGoodPerformance.incrementAndGet();
            consecutivePoorPerformance.set(0);
            
            // 考虑玩家数量的影响
            if (playerCount > 50) {
                return PerformanceLevel.MODERATE;
            }
            return PerformanceLevel.GOOD;
            
        } else if (tps <= POOR_TPS_THRESHOLD) {
            consecutivePoorPerformance.incrementAndGet();
            consecutiveGoodPerformance.set(0);
            return PerformanceLevel.POOR;
            
        } else {
            // 重置连续计数
            consecutiveGoodPerformance.set(0);
            consecutivePoorPerformance.set(0);
            return PerformanceLevel.MODERATE;
        }
    }
    
    /**
     * 根据性能等级调整参数
     */
    private boolean adjustBasedOnPerformance(PerformanceLevel level) {
        boolean adjusted = false;
        
        switch (level) {
            case GOOD:
                // 性能良好，可以增加批处理效率
                if (consecutiveGoodPerformance.get() >= PERFORMANCE_STREAK_THRESHOLD) {
                    adjusted = increaseBatchEfficiency();
                }
                break;
                
            case POOR:
                // 性能较差，需要降低批处理负载
                if (consecutivePoorPerformance.get() >= PERFORMANCE_STREAK_THRESHOLD) {
                    adjusted = decreaseBatchEfficiency();
                }
                break;
                
            case MODERATE:
                // 性能中等，保持当前设置或微调
                adjusted = moderateAdjustment();
                break;
        }
        
        return adjusted;
    }
    
    /**
     * 增加批处理效率
     */
    private boolean increaseBatchEfficiency() {
        boolean adjusted = false;
        
        // 增加批大小
        int newBatchSize = Math.min(MAX_BATCH_SIZE, currentBatchSize.get() + 10);
        if (newBatchSize != currentBatchSize.get()) {
            currentBatchSize.set(newBatchSize);
            adjusted = true;
        }
        
        // 减少处理延迟
        int newDelay = Math.max(MIN_PROCESSING_DELAY, currentProcessingDelay.get() - 1);
        if (newDelay != currentProcessingDelay.get()) {
            currentProcessingDelay.set(newDelay);
            adjusted = true;
        }
        
        // 增加区块限制
        int newChunkLimit = Math.min(MAX_CHUNK_LIMIT, currentChunkLimit.get() + 2);
        if (newChunkLimit != currentChunkLimit.get()) {
            currentChunkLimit.set(newChunkLimit);
            adjusted = true;
        }
        
        return adjusted;
    }
    
    /**
     * 降低批处理效率
     */
    private boolean decreaseBatchEfficiency() {
        boolean adjusted = false;
        
        // 减少批大小
        int newBatchSize = Math.max(MIN_BATCH_SIZE, currentBatchSize.get() - 15);
        if (newBatchSize != currentBatchSize.get()) {
            currentBatchSize.set(newBatchSize);
            adjusted = true;
        }
        
        // 增加处理延迟
        int newDelay = Math.min(MAX_PROCESSING_DELAY, currentProcessingDelay.get() + 1);
        if (newDelay != currentProcessingDelay.get()) {
            currentProcessingDelay.set(newDelay);
            adjusted = true;
        }
        
        // 减少区块限制
        int newChunkLimit = Math.max(MIN_CHUNK_LIMIT, currentChunkLimit.get() - 3);
        if (newChunkLimit != currentChunkLimit.get()) {
            currentChunkLimit.set(newChunkLimit);
            adjusted = true;
        }
        
        return adjusted;
    }
    
    /**
     * 中等性能时的微调
     */
    private boolean moderateAdjustment() {
        // 根据当前参数进行微调
        boolean adjusted = false;
        
        // 如果批大小过大，适当减少
        if (currentBatchSize.get() > 100) {
            currentBatchSize.set(currentBatchSize.get() - 5);
            adjusted = true;
        }
        
        // 如果延迟过小，适当增加
        if (currentProcessingDelay.get() < 2) {
            currentProcessingDelay.set(2);
            adjusted = true;
        }
        
        return adjusted;
    }
    
    // Getter方法
    public int getCurrentBatchSize() {
        return currentBatchSize.get();
    }
    
    public int getCurrentProcessingDelay() {
        return currentProcessingDelay.get();
    }
    
    public int getCurrentChunkLimit() {
        return currentChunkLimit.get();
    }
    
    /**
     * 获取当前批处理配置信息
     */
    public BatchConfiguration getCurrentConfiguration() {
        return new BatchConfiguration(
                currentBatchSize.get(),
                currentProcessingDelay.get(),
                currentChunkLimit.get()
        );
    }
    
    /**
     * 重置为默认参数
     */
    public void resetToDefaults() {
        initializeParameters();
        consecutiveGoodPerformance.set(0);
        consecutivePoorPerformance.set(0);
        lastAdjustmentTime.set(0);
    }
    
    /**
     * 性能等级枚举
     */
    private enum PerformanceLevel {
        GOOD,     // 性能良好
        MODERATE, // 性能中等
        POOR      // 性能较差
    }
    
    /**
     * 批处理配置类
     */
    public static class BatchConfiguration {
        private final int batchSize;
        private final int processingDelay;
        private final int chunkLimit;
        
        public BatchConfiguration(int batchSize, int processingDelay, int chunkLimit) {
            this.batchSize = batchSize;
            this.processingDelay = processingDelay;
            this.chunkLimit = chunkLimit;
        }
        
        public int getBatchSize() { return batchSize; }
        public int getProcessingDelay() { return processingDelay; }
        public int getChunkLimit() { return chunkLimit; }
        
        @Override
        public String toString() {
            return String.format("BatchConfiguration{batchSize=%d, processingDelay=%d, chunkLimit=%d}",
                    batchSize, processingDelay, chunkLimit);
        }
    }
}