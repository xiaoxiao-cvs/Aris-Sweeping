package com.xiaoxiao.arissweeping.adaptive;

import com.xiaoxiao.arissweeping.ArisSweeping;
import com.xiaoxiao.arissweeping.cleanup.ChainBasedEntityFilter;
import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.CleanupStats;
import com.xiaoxiao.arissweeping.util.LoggerUtil;
import com.xiaoxiao.arissweeping.util.TpsMonitor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自适应清理服务
 * 根据服务器性能动态调整清理策略和参数
 */
public class AdaptiveCleanupService {
    
    private final ArisSweeping plugin;
    private final ModConfig config;
    private final AdaptiveBatchManager batchManager;
    private final ChainBasedEntityFilter entityFilter;
    private final TpsMonitor tpsMonitor;
    
    // 任务管理
    private BukkitTask cleanupTask;
    private BukkitTask adaptiveTask;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    
    // 性能统计
    private final AtomicLong totalEntitiesProcessed = new AtomicLong(0);
    private final AtomicLong totalEntitiesCleaned = new AtomicLong(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    
    public AdaptiveCleanupService(ArisSweeping plugin) {
        this.plugin = plugin;
        this.config = plugin.getModConfig();
        this.tpsMonitor = new TpsMonitor(plugin);
        this.batchManager = new AdaptiveBatchManager(config, tpsMonitor);
        this.entityFilter = new ChainBasedEntityFilter(config);
    }
    
    /**
     * 启动自适应清理服务
     */
    public void start() {
        if (isRunning.get()) {
            LoggerUtil.warning("AdaptiveCleanupService", "服务已在运行中");
            return;
        }
        
        isRunning.set(true);
        
        // 启动TPS监控
        tpsMonitor.startMonitoring();
        
        // 启动自适应参数调整任务
        startAdaptiveTask();
        
        // 启动清理任务
        startCleanupTask();
        
        LoggerUtil.info("AdaptiveCleanupService", "自适应清理服务已启动");
    }
    
    /**
     * 停止自适应清理服务
     */
    public void stop() {
        if (!isRunning.get()) {
            return;
        }
        
        isRunning.set(false);
        
        // 停止任务
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        
        if (adaptiveTask != null) {
            adaptiveTask.cancel();
            adaptiveTask = null;
        }
        
        // 停止TPS监控
        tpsMonitor.stopMonitoring();
        
        LoggerUtil.info("AdaptiveCleanupService", "自适应清理服务已停止");
    }
    
    /**
     * 启动自适应参数调整任务
     */
    private void startAdaptiveTask() {
        adaptiveTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    batchManager.adjustParameters();
                } catch (Exception e) {
                    LoggerUtil.severe("AdaptiveCleanupService", "自适应参数调整失败", e);
                }
            }
        }.runTaskTimer(plugin, 600L, 600L); // 每30秒调整一次
    }
    
    /**
     * 启动清理任务
     */
    private void startCleanupTask() {
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isRunning.get()) {
                    return;
                }
                
                try {
                    performAdaptiveCleanup();
                } catch (Exception e) {
                    LoggerUtil.severe("AdaptiveCleanupService", "自适应清理执行失败", e);
                }
            }
        }.runTaskTimer(plugin, 200L, config.getCleanupInterval() * 20L);
    }
    
    /**
     * 执行自适应清理
     */
    private void performAdaptiveCleanup() {
        long startTime = System.currentTimeMillis();
        CleanupStats totalStats = new CleanupStats();
        
        // 获取当前批处理配置
        AdaptiveBatchManager.BatchConfiguration batchConfig = batchManager.getCurrentConfiguration();
        
        // 获取所有世界
        List<World> worlds = new ArrayList<>(Bukkit.getWorlds());
        
        for (World world : worlds) {
            if (!shouldCleanupWorld(world)) {
                continue;
            }
            
            CleanupStats worldStats = cleanupWorld(world, batchConfig);
            totalStats.merge(worldStats);
            
            // 在世界之间添加延迟
            if (batchConfig.getProcessingDelay() > 0) {
                try {
                    Thread.sleep(batchConfig.getProcessingDelay());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        // 更新统计信息
        long processingTime = System.currentTimeMillis() - startTime;
        updateStatistics(totalStats, processingTime);
        
        // 记录清理结果
        if (totalStats.getTotalCleaned() > 0) {
            LoggerUtil.info("AdaptiveCleanupService", 
                    "自适应清理完成 - 清理: %d, 处理时间: %dms, 批配置: %s",
                    totalStats.getTotalCleaned(), processingTime, batchConfig.toString());
        }
    }
    
    /**
     * 清理指定世界
     */
    private CleanupStats cleanupWorld(World world, AdaptiveBatchManager.BatchConfiguration batchConfig) {
        CleanupStats worldStats = new CleanupStats();
        
        try {
            Chunk[] loadedChunks = world.getLoadedChunks();
            int chunksProcessed = 0;
            
            for (Chunk chunk : loadedChunks) {
                if (chunksProcessed >= batchConfig.getChunkLimit()) {
                    break;
                }
                
                CleanupStats chunkStats = cleanupChunk(chunk, batchConfig);
                worldStats.merge(chunkStats);
                chunksProcessed++;
                
                // 检查是否需要中断
                if (!isRunning.get()) {
                    break;
                }
            }
            
        } catch (Exception e) {
            LoggerUtil.warning("AdaptiveCleanupService", "清理世界 %s 时发生异常: %s", 
                    world.getName(), e.getMessage());
        }
        
        return worldStats;
    }
    
    /**
     * 清理指定区块
     */
    private CleanupStats cleanupChunk(Chunk chunk, AdaptiveBatchManager.BatchConfiguration batchConfig) {
        CleanupStats chunkStats = new CleanupStats();
        
        try {
            Entity[] entities = chunk.getEntities();
            List<Entity> entitiesToProcess = new ArrayList<>();
            
            // 收集需要处理的实体
            for (Entity entity : entities) {
                if (entity != null && entity.isValid()) {
                    entitiesToProcess.add(entity);
                    
                    if (entitiesToProcess.size() >= batchConfig.getBatchSize()) {
                        // 处理当前批次
                        processBatch(entitiesToProcess, chunkStats);
                        entitiesToProcess.clear();
                    }
                }
            }
            
            // 处理剩余的实体
            if (!entitiesToProcess.isEmpty()) {
                processBatch(entitiesToProcess, chunkStats);
            }
            
        } catch (Exception e) {
            LoggerUtil.warning("AdaptiveCleanupService", "清理区块时发生异常: %s", e.getMessage());
        }
        
        return chunkStats;
    }
    
    /**
     * 处理实体批次
     */
    private void processBatch(List<Entity> entities, CleanupStats stats) {
        for (Entity entity : entities) {
            totalEntitiesProcessed.incrementAndGet();
            
            if (entityFilter.shouldCleanupEntity(entity)) {
                entity.remove();
                stats.incrementType(entity);
                totalEntitiesCleaned.incrementAndGet();
            }
        }
    }
    
    /**
     * 判断是否应该清理指定世界
     */
    private boolean shouldCleanupWorld(World world) {
        // 检查世界是否在清理列表中
        if (config.getCleanupWorlds() != null && !config.getCleanupWorlds().isEmpty()) {
            return config.getCleanupWorlds().contains(world.getName());
        }
        
        // 默认清理所有世界
        return true;
    }
    
    /**
     * 更新统计信息
     */
    private void updateStatistics(CleanupStats stats, long processingTime) {
        totalProcessingTime.addAndGet(processingTime);
    }
    
    /**
     * 重新加载配置
     */
    public void reload() {
        // 重新构建实体过滤器的责任链
        entityFilter.rebuildChain();
        
        // 重置批处理管理器
        batchManager.resetToDefaults();
        
        LoggerUtil.info("AdaptiveCleanupService", "配置已重新加载");
    }
    
    /**
     * 获取服务状态信息
     */
    public ServiceStatus getStatus() {
        return new ServiceStatus(
                isRunning.get(),
                totalEntitiesProcessed.get(),
                totalEntitiesCleaned.get(),
                totalProcessingTime.get(),
                batchManager.getCurrentConfiguration()
        );
    }
    
    /**
     * 服务状态类
     */
    public static class ServiceStatus {
        private final boolean running;
        private final long entitiesProcessed;
        private final long entitiesCleaned;
        private final long totalProcessingTime;
        private final AdaptiveBatchManager.BatchConfiguration batchConfig;
        
        public ServiceStatus(boolean running, long entitiesProcessed, long entitiesCleaned, 
                           long totalProcessingTime, AdaptiveBatchManager.BatchConfiguration batchConfig) {
            this.running = running;
            this.entitiesProcessed = entitiesProcessed;
            this.entitiesCleaned = entitiesCleaned;
            this.totalProcessingTime = totalProcessingTime;
            this.batchConfig = batchConfig;
        }
        
        public boolean isRunning() { return running; }
        public long getEntitiesProcessed() { return entitiesProcessed; }
        public long getEntitiesCleaned() { return entitiesCleaned; }
        public long getTotalProcessingTime() { return totalProcessingTime; }
        public AdaptiveBatchManager.BatchConfiguration getBatchConfig() { return batchConfig; }
        
        public double getCleanupRate() {
            return entitiesProcessed > 0 ? (double) entitiesCleaned / entitiesProcessed : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("ServiceStatus{running=%s, processed=%d, cleaned=%d, rate=%.2f%%, time=%dms, %s}",
                    running, entitiesProcessed, entitiesCleaned, getCleanupRate() * 100, 
                    totalProcessingTime, batchConfig.toString());
        }
    }
}