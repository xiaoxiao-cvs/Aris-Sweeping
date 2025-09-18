package com.arisweeping.async;

import com.arisweeping.core.ArisLogger;
import com.arisweeping.core.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 区块处理器
 * 提供分块异步处理、负载均衡和进度追踪功能
 */
public class ChunkProcessor {
    
    /**
     * 区块处理结果
     */
    public static class ChunkProcessingResult {
        private final ChunkPos chunkPos;
        private final int entitiesProcessed;
        private final int entitiesRemoved;
        private final long processingTime;
        private final boolean successful;
        private final String errorMessage;
        
        public ChunkProcessingResult(ChunkPos chunkPos, int entitiesProcessed, 
                                   int entitiesRemoved, long processingTime, 
                                   boolean successful, String errorMessage) {
            this.chunkPos = chunkPos;
            this.entitiesProcessed = entitiesProcessed;
            this.entitiesRemoved = entitiesRemoved;
            this.processingTime = processingTime;
            this.successful = successful;
            this.errorMessage = errorMessage;
        }
        
        // 成功结果构造器
        public static ChunkProcessingResult success(ChunkPos pos, int processed, int removed, long time) {
            return new ChunkProcessingResult(pos, processed, removed, time, true, null);
        }
        
        // 失败结果构造器
        public static ChunkProcessingResult failure(ChunkPos pos, String error, long time) {
            return new ChunkProcessingResult(pos, 0, 0, time, false, error);
        }
        
        // Getters
        public ChunkPos getChunkPos() { return chunkPos; }
        public int getEntitiesProcessed() { return entitiesProcessed; }
        public int getEntitiesRemoved() { return entitiesRemoved; }
        public long getProcessingTime() { return processingTime; }
        public boolean isSuccessful() { return successful; }
        public String getErrorMessage() { return errorMessage; }
        
        @Override
        public String toString() {
            if (successful) {
                return String.format("ChunkResult{pos=%s, processed=%d, removed=%d, time=%dms}", 
                                   chunkPos, entitiesProcessed, entitiesRemoved, processingTime);
            } else {
                return String.format("ChunkResult{pos=%s, FAILED: %s, time=%dms}", 
                                   chunkPos, errorMessage, processingTime);
            }
        }
    }
    
    /**
     * 处理进度信息
     */
    public static class ProcessingProgress {
        private final int totalChunks;
        private final AtomicInteger processedChunks;
        private final AtomicInteger successfulChunks;
        private final AtomicInteger failedChunks;
        private final AtomicLong totalProcessingTime;
        private final long startTime;
        
        public ProcessingProgress(int totalChunks) {
            this.totalChunks = totalChunks;
            this.processedChunks = new AtomicInteger(0);
            this.successfulChunks = new AtomicInteger(0);
            this.failedChunks = new AtomicInteger(0);
            this.totalProcessingTime = new AtomicLong(0);
            this.startTime = System.currentTimeMillis();
        }
        
        public void recordResult(ChunkProcessingResult result) {
            processedChunks.incrementAndGet();
            totalProcessingTime.addAndGet(result.getProcessingTime());
            
            if (result.isSuccessful()) {
                successfulChunks.incrementAndGet();
            } else {
                failedChunks.incrementAndGet();
            }
        }
        
        public double getProgressPercent() {
            return totalChunks > 0 ? (double) processedChunks.get() / totalChunks * 100.0 : 0.0;
        }
        
        public boolean isComplete() {
            return processedChunks.get() >= totalChunks;
        }
        
        public double getAverageProcessingTime() {
            int processed = processedChunks.get();
            return processed > 0 ? (double) totalProcessingTime.get() / processed : 0.0;
        }
        
        public long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }
        
        // Getters
        public int getTotalChunks() { return totalChunks; }
        public int getProcessedChunks() { return processedChunks.get(); }
        public int getSuccessfulChunks() { return successfulChunks.get(); }
        public int getFailedChunks() { return failedChunks.get(); }
        public long getTotalProcessingTime() { return totalProcessingTime.get(); }
        public long getStartTime() { return startTime; }
        
        @Override
        public String toString() {
            return String.format("Progress{%.1f%% (%d/%d), success=%d, failed=%d, avgTime=%.1fms}", 
                               getProgressPercent(), processedChunks.get(), totalChunks, 
                               successfulChunks.get(), failedChunks.get(), getAverageProcessingTime());
        }
    }
    
    /**
     * 区块负载信息
     */
    public static class ChunkLoadInfo {
        private final ChunkPos pos;
        private final int entityCount;
        private final int priority;
        
        public ChunkLoadInfo(ChunkPos pos, int entityCount, int priority) {
            this.pos = pos;
            this.entityCount = entityCount;
            this.priority = priority;
        }
        
        public ChunkPos getPos() { return pos; }
        public int getEntityCount() { return entityCount; }
        public int getPriority() { return priority; }
        
        @Override
        public String toString() {
            return String.format("ChunkLoad{%s, entities=%d, priority=%d}", pos, entityCount, priority);
        }
    }
    
    private final AsyncTaskManager asyncManager;
    private final ReentrantReadWriteLock progressLock = new ReentrantReadWriteLock();
    private final Map<UUID, ProcessingProgress> activeOperations = new ConcurrentHashMap<>();
    
    public ChunkProcessor(AsyncTaskManager asyncManager) {
        this.asyncManager = asyncManager;
    }
    
    /**
     * 处理指定区块范围内的实体
     */
    public CompletableFuture<List<ChunkProcessingResult>> processChunksInRange(
            ServerLevel level, BlockPos center, int radius, 
            Predicate<Entity> entityFilter, Function<Entity, Boolean> processor) {
        
        UUID operationId = UUID.randomUUID();
        ArisLogger.info("Starting chunk processing operation: {} at {} with radius {}", 
                   operationId, center, radius);
        
        // 收集需要处理的区块
        List<ChunkPos> chunksToProcess = collectChunksInRange(level, center, radius);
        
        if (chunksToProcess.isEmpty()) {
            ArisLogger.warn("No chunks found to process for operation: {}", operationId);
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        
        // 创建进度追踪
        ProcessingProgress progress = new ProcessingProgress(chunksToProcess.size());
        activeOperations.put(operationId, progress);
        
        // 按负载排序区块（优先处理实体较多的区块）
        List<ChunkLoadInfo> sortedChunks = prioritizeChunks(level, chunksToProcess);
        
        ArisLogger.info("Processing {} chunks for operation: {}", sortedChunks.size(), operationId);
        
        // 并行处理区块
        return processChunksParallel(level, sortedChunks, entityFilter, processor, progress)
                .whenComplete((results, throwable) -> {
                    activeOperations.remove(operationId);
                    if (throwable != null) {
                        ArisLogger.error("Chunk processing operation {} failed", operationId, throwable);
                    } else {
                        ArisLogger.info("Chunk processing operation {} completed: {}", operationId, progress);
                    }
                });
    }
    
    /**
     * 收集指定范围内的区块
     */
    private List<ChunkPos> collectChunksInRange(ServerLevel level, BlockPos center, int radius) {
        List<ChunkPos> chunks = new ArrayList<>();
        int chunkCenterX = center.getX() >> 4;
        int chunkCenterZ = center.getZ() >> 4;
        int chunkRadius = (radius >> 4) + 1;
        
        for (int x = chunkCenterX - chunkRadius; x <= chunkCenterX + chunkRadius; x++) {
            for (int z = chunkCenterZ - chunkRadius; z <= chunkCenterZ + chunkRadius; z++) {
                ChunkPos chunkPos = new ChunkPos(x, z);
                
                // 检查区块是否已加载
                if (isChunkLoaded(level, chunkPos)) {
                    chunks.add(chunkPos);
                }
            }
        }
        
        return chunks;
    }
    
    /**
     * 检查区块是否已加载
     */
    private boolean isChunkLoaded(ServerLevel level, ChunkPos pos) {
        try {
            // 使用更安全的方法检查区块是否已加载
            return level.isLoaded(new BlockPos(pos.getMinBlockX(), 64, pos.getMinBlockZ()));
        } catch (Exception e) {
            ArisLogger.debug("Failed to check chunk loading status for {}: {}", pos, e.getMessage());
            return false;
        }
    }
    
    /**
     * 按负载优先级排序区块
     */
    private List<ChunkLoadInfo> prioritizeChunks(ServerLevel level, List<ChunkPos> chunks) {
        return chunks.parallelStream()
                .map(pos -> {
                    try {
                        LevelChunk chunk = level.getChunk(pos.x, pos.z);
                        // 在Minecraft 1.20.1中，我们使用level来获取区块内的实体
                        List<net.minecraft.world.entity.Entity> entities = level.getEntities(null, 
                            new net.minecraft.world.phys.AABB(pos.getMinBlockX(), level.getMinBuildHeight(), pos.getMinBlockZ(),
                                                             pos.getMaxBlockX() + 1, level.getMaxBuildHeight(), pos.getMaxBlockZ() + 1));
                        int entityCount = entities.size();
                        
                        // 计算优先级：实体数量越多，优先级越高
                        int priority = Math.min(entityCount, 100); // 最大优先级为100
                        
                        return new ChunkLoadInfo(pos, entityCount, priority);
                    } catch (Exception e) {
                        ArisLogger.debug("Failed to get entity count for chunk {}: {}", pos, e.getMessage());
                        return new ChunkLoadInfo(pos, 0, 0);
                    }
                })
                .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority())) // 降序排列
                .collect(Collectors.toList());
    }
    
    /**
     * 并行处理区块
     */
    private CompletableFuture<List<ChunkProcessingResult>> processChunksParallel(
            ServerLevel level, List<ChunkLoadInfo> chunks,
            Predicate<Entity> entityFilter, Function<Entity, Boolean> processor,
            ProcessingProgress progress) {
        
        // 限制并发数量以避免过载
        int maxConcurrency = Math.min(chunks.size(), Constants.AsyncProcessing.MAX_THREAD_POOL_SIZE);
        Semaphore concurrencyLimit = new Semaphore(maxConcurrency);
        
        List<CompletableFuture<ChunkProcessingResult>> chunkFutures = chunks.stream()
                .map(chunkInfo -> processChunkAsync(level, chunkInfo, entityFilter, processor, 
                                                  progress, concurrencyLimit))
                .collect(Collectors.toList());
        
        return CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> chunkFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }
    
    /**
     * 异步处理单个区块
     */
    private CompletableFuture<ChunkProcessingResult> processChunkAsync(
            ServerLevel level, ChunkLoadInfo chunkInfo,
            Predicate<Entity> entityFilter, Function<Entity, Boolean> processor,
            ProcessingProgress progress, Semaphore concurrencyLimit) {
        
        return asyncManager.submitCoreTask(() -> {
            try {
                concurrencyLimit.acquire();
                return processSingleChunk(level, chunkInfo, entityFilter, processor, progress);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ChunkProcessingResult.failure(chunkInfo.getPos(), 
                                                   "Processing interrupted", 0);
            } finally {
                concurrencyLimit.release();
            }
        });
    }
    
    /**
     * 处理单个区块
     */
    private ChunkProcessingResult processSingleChunk(
            ServerLevel level, ChunkLoadInfo chunkInfo,
            Predicate<Entity> entityFilter, Function<Entity, Boolean> processor,
            ProcessingProgress progress) {
        
        long startTime = System.currentTimeMillis();
        ChunkPos pos = chunkInfo.getPos();
        
        try {
            ArisLogger.debug("Processing chunk: {}", pos);
            
            // 获取区块
            LevelChunk chunk = level.getChunk(pos.x, pos.z);
            if (chunk == null) {
                return ChunkProcessingResult.failure(pos, "Chunk not loaded", 
                                                   System.currentTimeMillis() - startTime);
            }
            
            // 获取区块中的所有实体
            List<Entity> allEntities = new ArrayList<>();
            // 使用level.getEntities获取指定区块范围内的实体
            AABB chunkAABB = new AABB(
                pos.getMinBlockX(), level.getMinBuildHeight(), pos.getMinBlockZ(),
                pos.getMaxBlockX() + 1, level.getMaxBuildHeight(), pos.getMaxBlockZ() + 1
            );
            allEntities.addAll(level.getEntities(null, chunkAABB));
            
            if (allEntities.isEmpty()) {
                ChunkProcessingResult result = ChunkProcessingResult.success(pos, 0, 0, 
                                                                           System.currentTimeMillis() - startTime);
                progress.recordResult(result);
                return result;
            }
            
            // 过滤和处理实体
            int entitiesProcessed = 0;
            int entitiesRemoved = 0;
            
            for (Entity entity : allEntities) {
                try {
                    if (entityFilter.test(entity)) {
                        entitiesProcessed++;
                        Boolean processed = processor.apply(entity);
                        if (processed != null && processed) {
                            entitiesRemoved++;
                        }
                    }
                } catch (Exception e) {
                    ArisLogger.warn("Failed to process entity {} in chunk {}: {}", 
                              entity.getId(), pos, e.getMessage());
                }
            }
            
            ChunkProcessingResult result = ChunkProcessingResult.success(pos, entitiesProcessed, 
                                                                       entitiesRemoved, 
                                                                       System.currentTimeMillis() - startTime);
            progress.recordResult(result);
            
            ArisLogger.debug("Completed processing chunk {}: processed={}, removed={}", 
                        pos, entitiesProcessed, entitiesRemoved);
            
            return result;
            
        } catch (Exception e) {
            ArisLogger.error("Error processing chunk {}: {}", pos, e.getMessage(), e);
            ChunkProcessingResult result = ChunkProcessingResult.failure(pos, e.getMessage(), 
                                                                       System.currentTimeMillis() - startTime);
            progress.recordResult(result);
            return result;
        }
    }
    
    /**
     * 获取活动操作的进度信息
     */
    public Map<UUID, ProcessingProgress> getActiveOperationsProgress() {
        return new HashMap<>(activeOperations);
    }
    
    /**
     * 获取指定操作的进度
     */
    public ProcessingProgress getProgress(UUID operationId) {
        return activeOperations.get(operationId);
    }
    
    /**
     * 获取活动操作数量
     */
    public int getActiveOperationCount() {
        return activeOperations.size();
    }
    
    /**
     * 生成处理摘要报告
     */
    public String generateProcessingSummary() {
        StringBuilder summary = new StringBuilder("=== ChunkProcessor Summary ===\n");
        
        if (activeOperations.isEmpty()) {
            summary.append("No active operations.\n");
        } else {
            summary.append(String.format("Active operations: %d\n", activeOperations.size()));
            activeOperations.forEach((id, progress) -> {
                summary.append(String.format("  %s: %s\n", id.toString().substring(0, 8), progress));
            });
        }
        
        return summary.toString();
    }
}