package com.xiaoxiao.arissweeping.service;

import com.xiaoxiao.arissweeping.ArisSweeping;
import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.LoggerUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 密度监控服务 - 负责监控实体密度并发出警告
 * 从EntityCleanupHandler中分离出来，遵循单一职责原则
 */
public class DensityMonitorService implements Listener {
    private final ArisSweeping plugin;
    private final ModConfig config;
    
    private BukkitTask densityCheckTask;
    private final Map<String, List<Entity>> worldEntityCache = new HashMap<>();
    private long lastCacheUpdate = 0;
    private static final long CACHE_DURATION = 30000; // 30秒缓存
    
    public DensityMonitorService(ArisSweeping plugin) {
        this.plugin = plugin;
        this.config = plugin.getModConfig();
    }
    
    /**
     * 启动密度检查任务
     */
    public void startDensityCheckTask() {
        try {
            // 停止现有任务
            stopDensityCheckTask();
            
            if (!config.isPluginEnabled() || !config.isDensityCheckEnabled()) {
                LoggerUtil.info("密度检查任务未启动 - 功能已禁用");
                return;
            }
            
            int intervalSeconds = config.getDensityCheckInterval();
            if (intervalSeconds <= 0) {
                LoggerUtil.warning("密度检查任务未启动 - 检查间隔无效: " + intervalSeconds);
                return;
            }
            
            densityCheckTask = new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        if (!config.isPluginEnabled() || !config.isDensityCheckEnabled()) {
                            LoggerUtil.info("密度检查跳过 - 功能已禁用");
                            return;
                        }
                        
                        checkEntityDensity();
                    } catch (Exception e) {
                        LoggerUtil.severe("密度检查任务执行时发生错误: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }.runTaskTimerAsynchronously(plugin, 20L * intervalSeconds, 20L * intervalSeconds);
            
            LoggerUtil.info("密度检查任务已启动，间隔: " + intervalSeconds + " 秒");
            
        } catch (Exception e) {
            LoggerUtil.severe("启动密度检查任务时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 检查实体密度
     */
    private void checkEntityDensity() {
        try {
            List<String> highDensityWorlds = new ArrayList<>();
            int totalEntities = 0;
            int worldsWithHighDensity = 0;
            
            for (World world : Bukkit.getWorlds()) {
                if (world == null) {
                    continue;
                }
                
                try {
                    // 使用缓存或重新获取实体列表
                    List<Entity> worldEntities = getWorldEntitiesWithCache(world);
                    int entityCount = worldEntities.size();
                    totalEntities += entityCount;
                    
                    // 检查世界级别的密度
                    if (entityCount > config.getEntityDensityThreshold()) {
                        highDensityWorlds.add(world.getName() + "(" + entityCount + ")");
                        worldsWithHighDensity++;
                        
                        LoggerUtil.warning("世界 " + world.getName() + " 实体密度过高: " + 
                                         entityCount + " > " + config.getEntityDensityThreshold());
                        
                        // 检查具体的区块密度
                        checkWorldEntityDensityWithCache(world, worldEntities);
                    }
                    
                } catch (Exception e) {
                    LoggerUtil.warning("检查世界 " + world.getName() + " 密度时发生错误: " + e.getMessage());
                }
            }
            
            // 发送密度警告通知
            if (worldsWithHighDensity > 0) {
                handleDensityWarningNotification(worldsWithHighDensity, highDensityWorlds, totalEntities);
            } else {
                LoggerUtil.info("密度检查完成 - 所有世界密度正常 (总实体数: " + totalEntities + ")");
            }
            
        } catch (Exception e) {
            LoggerUtil.severe("检查实体密度时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 使用缓存获取世界实体列表
     */
    private List<Entity> getWorldEntitiesWithCache(World world) {
        String worldName = world.getName();
        long currentTime = System.currentTimeMillis();
        
        // 检查缓存是否有效
        if (currentTime - lastCacheUpdate < CACHE_DURATION && worldEntityCache.containsKey(worldName)) {
            return worldEntityCache.get(worldName);
        }
        
        // 更新缓存
        List<Entity> entities = new ArrayList<>(world.getEntities());
        worldEntityCache.put(worldName, entities);
        lastCacheUpdate = currentTime;
        
        return entities;
    }
    
    /**
     * 检查世界实体密度（使用缓存）
     */
    private void checkWorldEntityDensityWithCache(World world, List<Entity> worldEntities) {
        try {
            Map<String, Integer> chunkEntityCount = new HashMap<>();
            
            // 统计每个区块的实体数量
            for (Entity entity : worldEntities) {
                if (entity != null && entity.isValid()) {
                    Chunk chunk = entity.getLocation().getChunk();
                    String chunkKey = chunk.getX() + "," + chunk.getZ();
                    chunkEntityCount.merge(chunkKey, 1, Integer::sum);
                }
            }
            
            // 检查高密度区块
            int chunkThreshold = config.getChunkEntityThreshold();
            List<String> highDensityChunks = new ArrayList<>();
            
            for (Map.Entry<String, Integer> entry : chunkEntityCount.entrySet()) {
                if (entry.getValue() > chunkThreshold) {
                    highDensityChunks.add(entry.getKey() + "(" + entry.getValue() + ")");
                }
            }
            
            if (!highDensityChunks.isEmpty()) {
                LoggerUtil.warning("世界 " + world.getName() + " 发现 " + highDensityChunks.size() + 
                                 " 个高密度区块: " + String.join(", ", highDensityChunks));
            }
            
        } catch (Exception e) {
            LoggerUtil.warning("检查世界 " + world.getName() + " 区块密度时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 处理密度警告通知
     */
    private void handleDensityWarningNotification(int worldsWithHighDensity, List<String> highDensityWorlds, int totalEntities) {
        try {
            // 在主线程中发送通知
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    if (config.isBroadcastCleanup()) {
                        String message = ChatColor.YELLOW + "[密度警告] " + ChatColor.WHITE + 
                                       "检测到 " + ChatColor.RED + worldsWithHighDensity + ChatColor.WHITE + 
                                       " 个世界实体密度过高 (总计: " + totalEntities + " 个实体)";
                        
                        Bukkit.broadcastMessage(message);
                        
                        // 如果启用详细信息，显示具体世界
                        if (config.isShowCleanupStats() && highDensityWorlds.size() <= 5) {
                            String detailMessage = ChatColor.GRAY + "高密度世界: " + 
                                                 String.join(", ", highDensityWorlds);
                            Bukkit.broadcastMessage(detailMessage);
                        }
                    }
                    
                    LoggerUtil.warning("密度警告已发送 - " + worldsWithHighDensity + " 个世界密度过高");
                    
                } catch (Exception e) {
                    LoggerUtil.severe("发送密度警告通知时发生错误: " + e.getMessage());
                }
            });
            
        } catch (Exception e) {
            LoggerUtil.severe("处理密度警告通知时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 检查单个区块的实体密度
     */
    public void checkChunkEntityDensity(Chunk chunk) {
        try {
            if (!config.isDensityCheckEnabled()) {
                return;
            }
            
            Entity[] entities = chunk.getEntities();
            int entityCount = entities.length;
            int threshold = config.getChunkEntityThreshold();
            
            if (entityCount > threshold) {
                LoggerUtil.warning("区块 [" + chunk.getX() + ", " + chunk.getZ() + "] 在世界 " + 
                                 chunk.getWorld().getName() + " 中实体密度过高: " + entityCount + " > " + threshold);
                
                // 可以在这里触发局部清理或其他处理
                if (config.isAutoCleanupOnHighDensity()) {
                    LoggerUtil.info("触发区块自动清理: [" + chunk.getX() + ", " + chunk.getZ() + "]");
                    // 这里可以调用清理服务
                }
            }
            
        } catch (Exception e) {
            LoggerUtil.warning("检查区块密度时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 区块加载事件处理
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        try {
            if (!config.isPluginEnabled() || !config.isDensityCheckEnabled()) {
                return;
            }
            
            // 异步检查区块密度，避免阻塞主线程
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                checkChunkEntityDensity(event.getChunk());
            });
            
        } catch (Exception e) {
            LoggerUtil.warning("处理区块加载事件时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 停止密度检查任务
     */
    public void stopDensityCheckTask() {
        try {
            if (densityCheckTask != null && !densityCheckTask.isCancelled()) {
                densityCheckTask.cancel();
                LoggerUtil.info("密度检查任务已停止");
            }
        } catch (Exception e) {
            LoggerUtil.severe("停止密度检查任务时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 重启密度检查任务
     */
    public void restartDensityCheckTask() {
        stopDensityCheckTask();
        startDensityCheckTask();
        LoggerUtil.info("密度检查任务已重启");
    }
    
    /**
     * 清除缓存
     */
    public void clearCache() {
        worldEntityCache.clear();
        lastCacheUpdate = 0;
        LoggerUtil.info("密度监控缓存已清除");
    }
    
    /**
     * 获取密度监控状态信息
     */
    public String getStatusInfo() {
        StringBuilder status = new StringBuilder();
        status.append("密度监控服务状态:\n");
        
        // 任务状态
        if (densityCheckTask != null && !densityCheckTask.isCancelled()) {
            status.append("- 密度检查任务: 运行中\n");
        } else {
            status.append("- 密度检查任务: 已停止\n");
        }
        
        // 配置信息
        status.append("- 检查间隔: ").append(config.getDensityCheckInterval()).append(" 秒\n");
        status.append("- 实体密度阈值: ").append(config.getEntityDensityThreshold()).append("\n");
        status.append("- 区块密度阈值: ").append(config.getChunkEntityThreshold()).append("\n");
        
        // 缓存信息
        status.append("- 缓存世界数: ").append(worldEntityCache.size()).append("\n");
        status.append("- 缓存更新时间: ");
        if (lastCacheUpdate > 0) {
            long cacheAge = (System.currentTimeMillis() - lastCacheUpdate) / 1000;
            status.append(cacheAge).append(" 秒前");
        } else {
            status.append("未更新");
        }
        
        return status.toString();
    }
    
    /**
     * 检查任务是否正在运行
     */
    public boolean isTaskRunning() {
        return densityCheckTask != null && !densityCheckTask.isCancelled();
    }
    
    /**
     * 关闭密度监控服务
     */
    public void shutdown() {
        stopDensityCheckTask();
        clearCache();
        LoggerUtil.info("密度监控服务已关闭");
    }
}