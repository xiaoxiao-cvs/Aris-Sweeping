package com.xiaoxiao.arissweeping.handler;

import com.xiaoxiao.arissweeping.ArisSweeping;
import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.CleanupStats;
import com.xiaoxiao.arissweeping.util.ClusterDetector;
import com.xiaoxiao.arissweeping.util.TpsMonitor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class EntityCleanupHandler implements Listener {
    private final ArisSweeping plugin;
    private final ModConfig config;
    private final TpsMonitor tpsMonitor;
    private BukkitTask cleanupTask;
    private BukkitTask densityCheckTask;
    private final AtomicBoolean isCleanupRunning = new AtomicBoolean(false);
    private final AtomicBoolean isEmergencyCleanupRunning = new AtomicBoolean(false);
    
    public EntityCleanupHandler(ArisSweeping plugin) {
        this.plugin = plugin;
        this.config = plugin.getModConfig();
        this.tpsMonitor = new TpsMonitor(plugin);
    }
    
    public void init() {
        plugin.getLogger().info("Initializing Entity Cleanup Handler...");
        
        try {
            // 检查全局开关
            if (!config.isPluginEnabled()) {
                plugin.getLogger().warning("Entity Cleanup Handler initialization skipped - plugin disabled in config");
                return;
            }
            
            // 验证配置
            validateConfiguration();
            
            // 初始化状态
            isCleanupRunning.set(false);
            isEmergencyCleanupRunning.set(false);
            
            // 启动定时清理任务
            plugin.getLogger().info("Starting cleanup task...");
            startCleanupTask();
            
            // 启动实体密度检查任务（每秒检查一次）
            plugin.getLogger().info("Starting density check task...");
            startDensityCheckTask();
            
            // 启动TPS监控
            if (config.isTpsMonitorEnabled()) {
                plugin.getLogger().info("Starting TPS monitor...");
                tpsMonitor.startMonitoring();
            } else {
                plugin.getLogger().info("TPS monitor disabled in config");
            }
            
            // 输出初始化状态
            logInitializationStatus();
            
            plugin.getLogger().info("Entity cleanup handler initialized successfully!");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize Entity Cleanup Handler: " + e.getMessage());
            e.printStackTrace();
            
            // 清理已启动的任务
            shutdown();
            throw new RuntimeException("Entity Cleanup Handler initialization failed", e);
        }
    }
    
    /**
     * 验证配置参数
     */
    private void validateConfiguration() {
        int cleanupInterval = config.getCleanupInterval();
        if (cleanupInterval < 10) {
            plugin.getLogger().warning("Cleanup interval is very short (" + cleanupInterval + "s), this may cause performance issues");
        } else if (cleanupInterval > 7200) {
            plugin.getLogger().warning("Cleanup interval is very long (" + cleanupInterval + "s), entities may accumulate significantly");
        }
        
        int densityThreshold = config.getEntityDensityThreshold();
        if (densityThreshold < 100) {
            plugin.getLogger().warning("Entity density threshold is very low (" + densityThreshold + "), this may cause frequent warnings");
        } else if (densityThreshold > 10000) {
            plugin.getLogger().warning("Entity density threshold is very high (" + densityThreshold + "), this may not effectively prevent lag");
        }
        
        if (config.isTpsMonitorEnabled()) {
            double lowTpsThreshold = config.getLowTpsThreshold();
            if (lowTpsThreshold < 10.0) {
                plugin.getLogger().warning("Low TPS threshold is very low (" + lowTpsThreshold + "), emergency cleanup may trigger too frequently");
            } else if (lowTpsThreshold > 19.0) {
                plugin.getLogger().warning("Low TPS threshold is very high (" + lowTpsThreshold + "), emergency cleanup may not trigger when needed");
            }
        }
        
        plugin.getLogger().info("Configuration validation completed");
    }
    
    /**
     * 输出初始化状态信息
     */
    private void logInitializationStatus() {
        plugin.getLogger().info("=== Entity Cleanup Handler Status ===");
        plugin.getLogger().info("Plugin Enabled: " + config.isPluginEnabled());
        plugin.getLogger().info("Cleanup Interval: " + config.getCleanupInterval() + " seconds");
        plugin.getLogger().info("Async Cleanup: " + config.isAsyncCleanup());
        plugin.getLogger().info("Broadcast Cleanup: " + config.isBroadcastCleanup());
        plugin.getLogger().info("Show Cleanup Stats: " + config.isShowCleanupStats());
        plugin.getLogger().info("Entity Density Threshold: " + config.getEntityDensityThreshold());
        plugin.getLogger().info("TPS Monitor Enabled: " + config.isTpsMonitorEnabled());
        if (config.isTpsMonitorEnabled()) {
            plugin.getLogger().info("Low TPS Threshold: " + config.getLowTpsThreshold());
            plugin.getLogger().info("Emergency Cleanup Enabled: " + config.isEmergencyCleanupEnabled());
        }
        plugin.getLogger().info("Cluster Cleanup Enabled: " + config.isClusterCleanupEnabled());
        plugin.getLogger().info("Debug Mode: " + config.isDebugMode());
        plugin.getLogger().info("Total Worlds: " + Bukkit.getWorlds().size());
        plugin.getLogger().info("Online Players: " + Bukkit.getOnlinePlayers().size());
        plugin.getLogger().info("=====================================");
    }
    
    private void startCleanupTask() {
        // 停止现有任务
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
            plugin.getLogger().info("Previous cleanup task cancelled");
        }
        
        // 检查全局开关
        if (!config.isPluginEnabled()) {
            plugin.getLogger().info("Cleanup task not started - plugin disabled in config");
            return;
        }
        
        int intervalSeconds = config.getCleanupInterval();
        long intervalTicks = intervalSeconds * 20L; // 转换为tick
        
        // 验证间隔设置
        if (intervalSeconds < 30) {
            plugin.getLogger().warning("Cleanup interval is very short (" + intervalSeconds + "s), this may cause performance issues");
        } else if (intervalSeconds > 3600) {
            plugin.getLogger().warning("Cleanup interval is very long (" + intervalSeconds + "s), entities may accumulate");
        }
        
        plugin.getLogger().info("Starting cleanup task with interval: " + intervalSeconds + " seconds (" + intervalTicks + " ticks)");
        plugin.getLogger().info("Cleanup configuration: async=" + config.isAsyncCleanup() + ", broadcast=" + config.isBroadcastCleanup() + ", showStats=" + config.isShowCleanupStats());
        
        try {
            // 参考EC项目的简单定时器实现
            cleanupTask = new BukkitRunnable() {
                private int executionCount = 0;
                
                @Override
                public void run() {
                    executionCount++;
                    
                    // 检查全局开关
                    if (!config.isPluginEnabled()) {
                        plugin.getLogger().info("Cleanup task #" + executionCount + " skipped - plugin disabled in config");
                        return;
                    }
                    
                    plugin.getLogger().info("Cleanup task #" + executionCount + " triggered (interval: " + intervalSeconds + "s)");
                    
                    if (!isCleanupRunning.get()) {
                        // 启动倒计时系统（仿照EC）
                        startCleanupCountdown();
                    } else {
                        plugin.getLogger().warning("Cleanup task #" + executionCount + " skipped - previous cleanup still running");
                    }
                }
            }.runTaskTimer(plugin, intervalTicks, intervalTicks);
            
            plugin.getLogger().info("Cleanup task started successfully. Next execution in " + intervalSeconds + " seconds");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to start cleanup task: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // 仿照EC项目的倒计时系统
    private void startCleanupCountdown() {
        // 检查全局开关
        if (!config.isPluginEnabled()) {
            plugin.getLogger().info("Cleanup countdown skipped - plugin disabled in config");
            return;
        }
        
        // 获取警告时间列表（仿照EC的warning-messages配置）
        List<Integer> warningTimes = getWarningTimes();
        if (warningTimes.isEmpty()) {
            // 如果没有警告时间，直接执行清理
            plugin.getLogger().info("No warning times configured, starting cleanup immediately");
            performCleanup();
            return;
        }
        
        int initialTime = warningTimes.get(0); // 最大的警告时间
        final int[] timeLeft = {initialTime};
        
        plugin.getLogger().info("Starting cleanup countdown: " + initialTime + " seconds");
        
        new BukkitRunnable() {
            @Override
            public void run() {
                // 检查全局开关
                if (!config.isPluginEnabled()) {
                    plugin.getLogger().info("Cleanup countdown cancelled - plugin disabled");
                    cancel();
                    return;
                }
                
                if (timeLeft[0] <= 0) {
                    // 倒计时结束，执行清理
                    plugin.getLogger().info("Countdown finished, starting cleanup");
                    // 输出光呀！
                    if (config.isBroadcastCleanup()) {
                        Bukkit.broadcastMessage(ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.YELLOW + "光呀！");
                    }
                    performCleanup();
                    cancel();
                    return;
                }
                
                // 检查是否需要发送警告消息
                if (warningTimes.contains(timeLeft[0])) {
                    sendCountdownMessage(timeLeft[0]);
                }
                
                timeLeft[0]--;
            }
        }.runTaskTimer(plugin, 0, 20L); // 每秒执行一次
    }
    
    // 获取警告时间列表（降序排列）
    private List<Integer> getWarningTimes() {
        List<Integer> times = new ArrayList<>();
        
        int interval = config.getCleanupInterval();
        
        // 根据间隔长度设置合适的警告时间
        if (interval >= 300) {
            // 5分钟以上：60秒、30秒、10秒、5秒警告
            times.add(60);
            times.add(30);
            times.add(10);
            times.add(5);
        } else if (interval >= 120) {
            // 2分钟以上：30秒、10秒、5秒警告
            times.add(30);
            times.add(10);
            times.add(5);
        } else if (interval >= 60) {
            // 1分钟以上：15秒、5秒警告
            times.add(15);
            times.add(5);
        } else if (interval >= 30) {
            // 30秒以上：10秒、3秒警告
            times.add(10);
            times.add(3);
        } else {
            // 短间隔：只在5秒前警告
            times.add(5);
        }
        
        // 降序排列（最大的时间在前）
        times.sort((a, b) -> b.compareTo(a));
        return times;
    }
    
    // 发送倒计时消息
    private void sendCountdownMessage(int timeLeft) {
        if (!config.isBroadcastCleanup()) {
            return;
        }
        
        String timeUnit = timeLeft >= 60 ? "分钟" : "秒";
        int displayTime = timeLeft >= 60 ? timeLeft / 60 : timeLeft;
        
        String message = ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝将在 " + 
                        ChatColor.RED + displayTime + timeUnit + ChatColor.WHITE + " 后开始定时清理，请注意保护重要物品！";
        
        Bukkit.broadcastMessage(message);
        plugin.getLogger().info("Cleanup warning sent: " + timeLeft + " seconds remaining");
    }
    
    private void startDensityCheckTask() {
        if (densityCheckTask != null) {
            densityCheckTask.cancel();
        }
        
        densityCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                // 检查全局开关
                if (!config.isPluginEnabled()) {
                    return;
                }
                
                checkEntityDensity();
            }
        }.runTaskTimer(plugin, 20L, 20L); // 每秒检查一次
    }
    
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // 检查全局开关
        if (!config.isPluginEnabled()) {
            return;
        }
        
        // 当区块加载时检查实体密度
        Chunk chunk = event.getChunk();
        new BukkitRunnable() {
            @Override
            public void run() {
                checkChunkEntityDensity(chunk);
            }
        }.runTaskLater(plugin, 20L); // 延迟1秒检查
    }
    
    private void checkEntityDensity() {
        if (!config.isPluginEnabled()) {
            plugin.getLogger().fine("Entity density check skipped - plugin disabled");
            return;
        }
        
        try {
            int totalEntities = 0;
            int worldsWithHighDensity = 0;
            List<String> highDensityWorlds = new ArrayList<>();
            
            for (World world : Bukkit.getWorlds()) {
                try {
                    List<Entity> entities = world.getEntities();
                    int entityCount = entities.size();
                    totalEntities += entityCount;
                    
                    int threshold = config.getEntityDensityThreshold();
                    
                    if (entityCount > threshold) {
                        worldsWithHighDensity++;
                        String worldInfo = world.getName() + "(" + entityCount + "/" + threshold + ")";
                        highDensityWorlds.add(worldInfo);
                        
                        plugin.getLogger().warning("High entity density detected in world " + world.getName() + ": " + 
                                                  entityCount + " entities (threshold: " + threshold + ", " + 
                                                  String.format("%.1f", (double)entityCount/threshold*100) + "% of threshold)");
                        
                        // 分析实体类型分布
                        if (config.isDebugMode()) {
                            Map<String, Integer> entityTypes = new HashMap<>();
                            for (Entity entity : entities) {
                                String type = entity.getType().name();
                                entityTypes.put(type, entityTypes.getOrDefault(type, 0) + 1);
                            }
                            
                            // 输出前5种最多的实体类型
                            entityTypes.entrySet().stream()
                                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                                .limit(5)
                                .forEach(entry -> plugin.getLogger().info("  " + entry.getKey() + ": " + entry.getValue()));
                        }
                    } else {
                        plugin.getLogger().fine("Entity density normal in world " + world.getName() + ": " + 
                                              entityCount + "/" + threshold + " (" + 
                                              String.format("%.1f", (double)entityCount/threshold*100) + "% of threshold)");
                    }
                    
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to check entity density for world " + world.getName() + ": " + e.getMessage());
                }
            }
            
            // 发送密度警告通知
            if (worldsWithHighDensity > 0) {
                handleDensityWarningNotification(worldsWithHighDensity, highDensityWorlds, totalEntities);
            } else {
                plugin.getLogger().fine("All worlds have normal entity density. Total entities: " + totalEntities);
            }
            
            // 继续执行原有的世界实体密度检查逻辑
            for (World world : Bukkit.getWorlds()) {
                if (config.isAsyncCleanup()) {
                    CompletableFuture.runAsync(() -> checkWorldEntityDensity(world));
                } else {
                    checkWorldEntityDensity(world);
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Entity density check failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 处理密度警告通知
     */
    private void handleDensityWarningNotification(int worldsWithHighDensity, List<String> highDensityWorlds, int totalEntities) {
        if (!config.isBroadcastCleanup()) {
            plugin.getLogger().info("Density warning notification skipped - broadcast disabled");
            return;
        }
        
        try {
            if (worldsWithHighDensity == 1) {
                // 单个世界密度过高
                String message = ChatColor.YELLOW + "[密度警告] " + ChatColor.WHITE + "世界 " + 
                               highDensityWorlds.get(0) + " 实体密度过高";
                Bukkit.broadcastMessage(message);
            } else {
                // 多个世界密度过高
                String message = ChatColor.YELLOW + "[密度警告] " + ChatColor.WHITE + "检测到 " + 
                               worldsWithHighDensity + " 个世界实体密度过高 (总计 " + totalEntities + " 个实体)";
                Bukkit.broadcastMessage(message);
                
                // 如果启用详细统计，显示具体世界信息
                if (config.isShowCleanupStats() && highDensityWorlds.size() <= 3) {
                    String detailMessage = ChatColor.GRAY + "高密度世界: " + String.join(", ", highDensityWorlds);
                    Bukkit.broadcastMessage(detailMessage);
                }
            }
            
            plugin.getLogger().info("Density warning notification sent to " + Bukkit.getOnlinePlayers().size() + " players");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to send density warning notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void checkWorldEntityDensity(World world) {
        Map<String, List<Entity>> chunkEntities = new HashMap<>();
        
        // 收集所有实体按区块分组
        for (Entity entity : world.getEntities()) {
            String chunkKey = entity.getLocation().getChunk().getX() + "," + entity.getLocation().getChunk().getZ();
            chunkEntities.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(entity);
        }
        
        // 检查每个区块的实体密度
        for (Map.Entry<String, List<Entity>> entry : chunkEntities.entrySet()) {
            List<Entity> entities = entry.getValue();
            
            // 统计不同类型的实体
            long itemCount = entities.stream().filter(e -> e instanceof Item).count();
            long totalEntityCount = entities.size();
            
            // 如果超过阈值，进行清理
            if (itemCount > config.getMaxItemsPerChunk() || 
                totalEntityCount > config.getMaxEntitiesPerChunk()) {
                
                cleanupChunkEntities(entities);
            }
        }
    }
    
    private void checkChunkEntityDensity(Chunk chunk) {
        List<Entity> entities = new ArrayList<>();
        
        // 收集区块中的所有实体
        for (Entity entity : chunk.getEntities()) {
            entities.add(entity);
        }
        
        // 统计不同类型的实体
        long itemCount = entities.stream().filter(e -> e instanceof Item).count();
        long totalEntityCount = entities.size();
        
        // 如果超过阈值，进行清理
        if (itemCount > config.getMaxItemsPerChunk() || 
            totalEntityCount > config.getMaxEntitiesPerChunk()) {
            
            cleanupChunkEntities(entities);
        }
    }
    
    private void performCleanup() {
        // 检查全局开关
        if (!config.isPluginEnabled()) {
            plugin.getLogger().info("Cleanup skipped - plugin disabled in config");
            return;
        }
        
        if (!isCleanupRunning.compareAndSet(false, true)) {
            plugin.getLogger().info("Cleanup already running, skipping");
            return;
        }
        
        try {
            plugin.getLogger().info("Starting cleanup process... (async: " + config.isAsyncCleanup() + ")");
            CleanupStats stats = new CleanupStats();
            
            if (config.isAsyncCleanup()) {
                // 异步清理
                CompletableFuture.runAsync(() -> {
                    plugin.getLogger().info("Starting async cleanup");
                    
                    try {
                        for (World world : Bukkit.getWorlds()) {
                            // 在异步任务中也要检查全局开关
                            if (!config.isPluginEnabled()) {
                                plugin.getLogger().info("Async cleanup cancelled - plugin disabled during execution");
                                return;
                            }
                            
                            cleanupWorldAsync(world, stats);
                            // 在世界之间添加小延迟，避免一次性处理过多实体
                            try {
                                Thread.sleep(config.getBatchDelay() * 5); // 世界间延迟为批次延迟的5倍
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                plugin.getLogger().warning("Async cleanup interrupted");
                                return;
                            }
                        }
                        
                        // 在主线程中处理通知和重置标志
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                try {
                                    plugin.getLogger().info("Async cleanup completed: " + stats.toString());
                                    handleCleanupNotification(stats);
                                } finally {
                                    isCleanupRunning.set(false);
                                }
                            }
                        }.runTask(plugin);
                        
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error during async cleanup: " + e.getMessage());
                        e.printStackTrace();
                        // 确保在异常情况下也重置标志
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                isCleanupRunning.set(false);
                            }
                        }.runTask(plugin);
                    }
                });
            } else {
                // 同步清理
                try {
                    for (World world : Bukkit.getWorlds()) {
                        // 在同步清理中也要检查全局开关
                        if (!config.isPluginEnabled()) {
                            plugin.getLogger().info("Sync cleanup cancelled - plugin disabled during execution");
                            return;
                        }
                        
                        cleanupWorldSync(world, stats);
                    }
                    
                    plugin.getLogger().info("Sync cleanup completed: " + stats.toString());
                    handleCleanupNotification(stats);
                    
                } catch (Exception e) {
                    plugin.getLogger().severe("Error during sync cleanup: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
        } finally {
            if (!config.isAsyncCleanup()) {
                isCleanupRunning.set(false);
            }
            // 异步模式下的标志重置已移到异步任务的回调中
        }
    }
    
    /**
     * 统一处理清理完成后的通知逻辑
     */
    private void handleCleanupNotification(CleanupStats stats) {
        // 再次检查全局开关和广播开关
        if (!config.isPluginEnabled()) {
            plugin.getLogger().info("Cleanup notification skipped - plugin disabled");
            return;
        }
        
        if (config.isBroadcastCleanup()) {
            if (stats.getTotalCleaned() > 0) {
                broadcastCleanupMessage(stats);
                plugin.getLogger().info("Cleanup notification sent - entities cleaned: " + stats.getTotalCleaned());
            } else {
                // 即使没有清理实体，也发送完成消息，显示检查的详细信息
                String completionMessage = ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝完成了定时清理检查，" +
                    "清理了 " + ChatColor.YELLOW + stats.getTotalCleaned() + ChatColor.WHITE + " 个实体，一切都很干净呢~ 老师~";
                Bukkit.broadcastMessage(completionMessage);
                plugin.getLogger().info("Cleanup completion notification sent - no entities cleaned");
            }
        } else {
            plugin.getLogger().info("Cleanup notification disabled in config");
        }
    }
    
    private void cleanupWorldAsync(World world, CleanupStats stats) {
        List<Entity> toRemove = new ArrayList<>();
        
        // 分批处理实体，避免一次性处理过多
        List<Entity> entities = new ArrayList<>(world.getEntities());
        int batchSize = Math.max(config.getBatchSize(), entities.size() / 10); // 使用配置的批次大小
        
        for (int i = 0; i < entities.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, entities.size());
            List<Entity> batch = entities.subList(i, endIndex);
            
            for (Entity entity : batch) {
                if (shouldCleanupEntity(entity)) {
                    toRemove.add(entity);
                    stats.incrementType(entity);
                }
            }
            
            // 分批移除实体，减少主线程压力
            if (!toRemove.isEmpty()) {
                List<Entity> currentBatch = new ArrayList<>(toRemove);
                toRemove.clear();
                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (Entity entity : currentBatch) {
                            if (entity.isValid()) {
                                entity.remove();
                            }
                        }
                    }
                }.runTask(plugin);
                
                // 批次间延迟，避免卡顿
                try {
                    Thread.sleep(config.getBatchDelay());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    private void cleanupWorldSync(World world, CleanupStats stats) {
        List<Entity> toRemove = new ArrayList<>();
        int processedCount = 0;
        int maxPerTick = config.getMaxChunksPerTick() * 50; // 每tick最大处理实体数
        
        for (Entity entity : world.getEntities()) {
            if (shouldCleanupEntity(entity)) {
                toRemove.add(entity);
                stats.incrementType(entity);
            }
            
            processedCount++;
            // 分批处理，避免单次处理过多实体
            if (processedCount >= maxPerTick) {
                removeEntitiesBatch(toRemove);
                toRemove.clear();
                processedCount = 0;
                
                // 让出CPU时间
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        // 处理剩余实体
        if (!toRemove.isEmpty()) {
            removeEntitiesBatch(toRemove);
        }
    }
    
    private void removeEntitiesBatch(List<Entity> entities) {
          for (Entity entity : entities) {
              if (entity.isValid()) {
                  entity.remove();
              }
          }
      }
    
    private void cleanupChunkEntities(List<Entity> entities) {
        List<Entity> toRemove = new ArrayList<>();
        
        for (Entity entity : entities) {
            if (shouldCleanupEntity(entity)) {
                toRemove.add(entity);
            }
        }
        
        // 在主线程中移除实体
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity entity : toRemove) {
                    if (entity.isValid()) {
                        entity.remove();
                    }
                }
            }
        }.runTask(plugin);
    }
    
    private boolean shouldCleanupEntity(Entity entity) {
        // 永远不清理玩家
        if (entity instanceof Player) {
            return false;
        }
        
        // 掉落物清理 - 参考EntityClearer，跳过所有检查直接清理
        if (entity instanceof Item item) {
            if (!config.isCleanupItems()) return false;
            
            // 保护有自定义名称的物品（如玩家重命名的装备）
            if (item.getCustomName() != null) {
                return false;
            }
            
            // 直接清理掉落物，不进行聚集检测和智能保护检查
            return true;
        }
        
        // 智能保护检查
        if (ClusterDetector.shouldProtectEntity(entity)) {
            return false;
        }
        
        // 排除特殊实体：矿车、船只、盔甲架等
        if (entity instanceof org.bukkit.entity.Minecart ||
            entity instanceof org.bukkit.entity.Boat ||
            entity instanceof org.bukkit.entity.ArmorStand ||
            entity instanceof org.bukkit.entity.ItemFrame ||
            entity instanceof org.bukkit.entity.Painting) {
            return false;
        }
        
        // 聚集清理检查
        if (config.isClusterCleanupEnabled()) {
            boolean isClustered = ClusterDetector.isEntityClustered(
                entity, 
                config.getClusterDetectionDistance(), 
                config.getMinClusterSize(), 
                config.isOnlyCountSameType()
            );
            
            // 如果不在聚集中，且启用了聚集清理，则不清理
            if (!isClustered) {
                return false;
            }
        }
        
        // 经验球清理
        if (entity instanceof ExperienceOrb) {
            return config.isCleanupExperienceOrbs();
        }
        
        // 箭矢清理（只清理普通箭矢，不清理三叉戟等特殊投射物）
        if (entity instanceof Arrow) {
            return config.isCleanupArrows();
        }
        
        // 掉落物（掉落方块）清理
        if (entity instanceof org.bukkit.entity.FallingBlock) {
            return config.isCleanupFallingBlocks();
        }
        
        // 敌对生物清理
        if (entity instanceof Monster) {
            // 保护有名字的怪物
            if (entity.getCustomName() != null) {
                return false;
            }
            
            // 只有在配置开启敌对生物清理时才清理
            if (config.isCleanupHostileMobs()) {
                // 50%概率清理敌对生物，避免过度清理
                return Math.random() < 0.5;
            }
            return false;
        }
        
        // 被动生物不清理（按用户要求）
        if (entity instanceof Animals) {
            return false;
        }
        
        return false;
    }
    
    private void broadcastCleanupMessage(CleanupStats stats) {
        // 再次检查配置，确保广播是启用的
        if (!config.isBroadcastCleanup()) {
            plugin.getLogger().info("Cleanup message broadcast skipped - disabled in config");
            return;
        }
        
        plugin.getLogger().info("Broadcasting cleanup message - Total cleaned: " + stats.getTotalCleaned());
        
        String message;
        
        if (config.isShowCleanupStats() && stats.getTotalCleaned() > 0) {
            // 详细统计消息 - 只显示有清理数量的类型
            StringBuilder detailBuilder = new StringBuilder();
            detailBuilder.append(ChatColor.GOLD).append("[邦邦卡邦！] ")
                        .append(ChatColor.WHITE).append("爱丽丝扫掉了 ")
                        .append(ChatColor.RED).append(stats.getTotalCleaned())
                        .append(ChatColor.WHITE).append(" 个实体呢~ 老师~ (");
            
            List<String> details = new ArrayList<>();
            if (stats.getItemsCleaned() > 0) {
                details.add("物品: " + ChatColor.YELLOW + stats.getItemsCleaned() + ChatColor.WHITE);
            }
            if (stats.getExperienceOrbsCleaned() > 0) {
                details.add("经验球: " + ChatColor.YELLOW + stats.getExperienceOrbsCleaned() + ChatColor.WHITE);
            }
            if (stats.getArrowsCleaned() > 0) {
                details.add("箭矢: " + ChatColor.YELLOW + stats.getArrowsCleaned() + ChatColor.WHITE);
            }
            if (stats.getFallingBlocksCleaned() > 0) {
                details.add("掉落物: " + ChatColor.YELLOW + stats.getFallingBlocksCleaned() + ChatColor.WHITE);
            }
            if (stats.getMobsCleaned() > 0) {
                details.add("生物: " + ChatColor.YELLOW + stats.getMobsCleaned() + ChatColor.WHITE);
            }
            
            if (!details.isEmpty()) {
                detailBuilder.append(String.join(", ", details));
            } else {
                detailBuilder.append("其他实体");
            }
            detailBuilder.append(")");
            
            message = detailBuilder.toString();
        } else {
            // 简单消息
            if (stats.getTotalCleaned() > 0) {
                message = ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝扫掉了 " + 
                         ChatColor.RED + stats.getTotalCleaned() + ChatColor.WHITE + " 个实体呢~ 老师~";
            } else {
                message = ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝完成了清理检查，一切都很干净呢~ 老师~";
            }
        }
        
        // 记录日志（移除颜色代码）
        String logMessage = message.replaceAll("§[0-9a-fk-or]", "");
        plugin.getLogger().info("Cleanup message: " + logMessage);
        
        // 向所有在线玩家广播消息
        try {
            Bukkit.broadcastMessage(message);
            plugin.getLogger().info("Cleanup message broadcasted successfully to " + Bukkit.getOnlinePlayers().size() + " players");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to broadcast cleanup message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    

    
    /**
     * 执行紧急清理（由TPS监控触发）
     */
    public void performEmergencyCleanup() {
        if (!config.isEmergencyCleanupEnabled()) {
            plugin.getLogger().info("Emergency cleanup skipped - emergency cleanup disabled in config");
            return;
        }
        
        // 检查全局开关
        if (!config.isPluginEnabled()) {
            plugin.getLogger().info("Emergency cleanup skipped - plugin disabled in config");
            return;
        }
        
        if (!isEmergencyCleanupRunning.compareAndSet(false, true)) {
            plugin.getLogger().info("Emergency cleanup skipped - already running");
            return; // 已经在运行紧急清理
        }
        
        plugin.getLogger().warning("Performing emergency cleanup due to low TPS (current: " + tpsMonitor.getCurrentTps() + ", threshold: " + config.getLowTpsThreshold() + ")");
        
        // 发送紧急清理开始通知
        if (config.isBroadcastCleanup()) {
            String startMessage = ChatColor.RED + "[紧急清理] " + ChatColor.YELLOW + "检测到服务器TPS过低，爱丽丝开始紧急清理实体...";
            Bukkit.broadcastMessage(startMessage);
        }
        
        try {
            CleanupStats stats = new CleanupStats();
            long startTime = System.currentTimeMillis();
            
            // 紧急清理使用更激进的策略
            for (World world : Bukkit.getWorlds()) {
                // 再次检查插件状态
                if (!config.isPluginEnabled()) {
                    plugin.getLogger().info("Emergency cleanup interrupted - plugin disabled");
                    break;
                }
                
                try {
                    List<Entity> entities = new ArrayList<>(world.getEntities());
                    
                    // 使用聚集清理算法获取清理候选
                    if (config.isClusterCleanupEnabled()) {
                        plugin.getLogger().info("Using cluster cleanup algorithm for emergency cleanup in world: " + world.getName());
                        List<Entity> candidates = ClusterDetector.getClusterCleanupCandidates(
                            entities,
                            config.getClusterDetectionDistance(),
                            config.getMinClusterSize(),
                            config.getClusterPreserveRatio()
                        );
                        
                        for (Entity entity : candidates) {
                            if (entity.isValid() && shouldCleanupEntity(entity)) {
                                entity.remove();
                                stats.incrementType(entity);
                            }
                        }
                    } else {
                        plugin.getLogger().info("Using standard cleanup algorithm for emergency cleanup in world: " + world.getName());
                        // 常规清理
                        for (Entity entity : entities) {
                            if (shouldCleanupEntity(entity)) {
                                entity.remove();
                                stats.incrementType(entity);
                            }
                        }
                    }
                    
                    plugin.getLogger().info("Emergency cleanup completed for world: " + world.getName());
                } catch (Exception e) {
                    plugin.getLogger().severe("Emergency cleanup failed for world " + world.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            plugin.getLogger().info("Emergency cleanup completed in " + duration + "ms, cleaned " + stats.getTotalCleaned() + " entities");
            
            // 统一处理紧急清理通知
            handleEmergencyCleanupNotification(stats, duration);
            
        } catch (Exception e) {
            plugin.getLogger().severe("Emergency cleanup failed: " + e.getMessage());
            e.printStackTrace();
            
            // 发送错误通知
            if (config.isBroadcastCleanup()) {
                String errorMessage = ChatColor.RED + "[紧急清理] " + ChatColor.WHITE + "清理过程中发生错误，请查看控制台日志";
                Bukkit.broadcastMessage(errorMessage);
            }
        } finally {
            isEmergencyCleanupRunning.set(false);
        }
    }
    
    /**
     * 处理紧急清理通知
     */
    private void handleEmergencyCleanupNotification(CleanupStats stats, long duration) {
        if (!config.isBroadcastCleanup()) {
            plugin.getLogger().info("Emergency cleanup notification skipped - broadcast disabled");
            return;
        }
        
        try {
            if (stats.getTotalCleaned() > 0) {
                if (config.isShowCleanupStats()) {
                    // 显示详细统计
                    StringBuilder detailMessage = new StringBuilder();
                    detailMessage.append(ChatColor.RED).append("[紧急清理] ").append(ChatColor.WHITE)
                               .append("已清理 ").append(stats.getTotalCleaned()).append(" 个实体 (耗时 ").append(duration).append("ms): ");
                    
                    List<String> details = new ArrayList<>();
                    if (stats.getItemsCleaned() > 0) {
                        details.add("物品×" + stats.getItemsCleaned());
                    }
                    if (stats.getExperienceOrbsCleaned() > 0) {
                        details.add("经验球×" + stats.getExperienceOrbsCleaned());
                    }
                    if (stats.getArrowsCleaned() > 0) {
                        details.add("箭矢×" + stats.getArrowsCleaned());
                    }
                    if (stats.getFallingBlocksCleaned() > 0) {
                        details.add("掉落物×" + stats.getFallingBlocksCleaned());
                    }
                    if (stats.getMobsCleaned() > 0) {
                        details.add("生物×" + stats.getMobsCleaned());
                    }
                    
                    if (!details.isEmpty()) {
                        detailMessage.append(String.join(", ", details));
                    }
                    
                    Bukkit.broadcastMessage(detailMessage.toString());
                } else {
                    // 显示简单统计
                    String simpleMessage = ChatColor.RED + "[紧急清理] " + ChatColor.WHITE + "爱丽丝紧急清理了 " + 
                                          ChatColor.YELLOW + stats.getTotalCleaned() + ChatColor.WHITE + " 个实体，服务器性能已恢复";
                    Bukkit.broadcastMessage(simpleMessage);
                }
            } else {
                // 无实体清理
                String noCleanupMessage = ChatColor.RED + "[紧急清理] " + ChatColor.WHITE + "未发现需要清理的实体，TPS问题可能由其他原因引起";
                Bukkit.broadcastMessage(noCleanupMessage);
            }
            
            plugin.getLogger().info("Emergency cleanup notification sent to " + Bukkit.getOnlinePlayers().size() + " players");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to send emergency cleanup notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取TPS监控器
     */
    public TpsMonitor getTpsMonitor() {
        return tpsMonitor;
    }
    
    public void restartCleanupTask() {
        startCleanupTask();
        plugin.getLogger().info("Cleanup task restarted with new interval: " + config.getCleanupInterval() + " seconds");
    }
    
    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        if (densityCheckTask != null) {
            densityCheckTask.cancel();
        }
        if (tpsMonitor != null) {
            tpsMonitor.stopMonitoring();
        }
        plugin.getLogger().info("Entity cleanup handler shutdown");
    }
}