package com.xiaoxiao.arissweeping.handler;

import com.xiaoxiao.arissweeping.ArisSweeping;
import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.CleanupStats;
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
    private BukkitTask cleanupTask;
    private BukkitTask densityCheckTask;
    private final AtomicBoolean isCleanupRunning = new AtomicBoolean(false);
    
    public EntityCleanupHandler(ArisSweeping plugin) {
        this.plugin = plugin;
        this.config = plugin.getModConfig();
    }
    
    public void init() {
        // 启动定时清理任务
        startCleanupTask();
        
        // 启动实体密度检查任务（每秒检查一次）
        startDensityCheckTask();
        
        plugin.getLogger().info("Entity cleanup handler initialized with interval: " + config.getCleanupInterval() + " seconds");
    }
    
    private void startCleanupTask() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        
        long intervalTicks = config.getCleanupInterval() * 20L; // 转换为tick
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                // 检查全局开关
                if (!config.isPluginEnabled()) {
                    return;
                }
                
                if (!isCleanupRunning.get()) {
                    performCleanup();
                }
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
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
        for (World world : Bukkit.getWorlds()) {
            if (config.isAsyncCleanup()) {
                CompletableFuture.runAsync(() -> checkWorldEntityDensity(world));
            } else {
                checkWorldEntityDensity(world);
            }
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
        if (!isCleanupRunning.compareAndSet(false, true)) {
            return; // 已经在运行清理任务
        }
        
        try {
            CleanupStats stats = new CleanupStats();
            
            if (config.isAsyncCleanup()) {
                // 异步清理，分批处理以减少服务器卡顿
                CompletableFuture.runAsync(() -> {
                    for (World world : Bukkit.getWorlds()) {
                        cleanupWorldAsync(world, stats);
                        // 在世界之间添加小延迟，避免一次性处理过多实体
                        try {
                            Thread.sleep(config.getBatchDelay() * 5); // 世界间延迟为批次延迟的5倍
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    
                    // 在主线程中广播结果
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (config.isBroadcastCleanup() && stats.getTotalCleaned() > 0) {
                                broadcastCleanupMessage(stats);
                            }
                            plugin.getLogger().info("Async cleanup completed: " + stats.toString());
                        }
                    }.runTask(plugin);
                });
            } else {
                // 同步清理，分批处理
                for (World world : Bukkit.getWorlds()) {
                    cleanupWorldSync(world, stats);
                }
                
                // 广播清理结果
                if (config.isBroadcastCleanup() && stats.getTotalCleaned() > 0) {
                    broadcastCleanupMessage(stats);
                }
                
                plugin.getLogger().info("Sync cleanup completed: " + stats.toString());
            }
            
        } finally {
            if (!config.isAsyncCleanup()) {
                isCleanupRunning.set(false);
            } else {
                // 异步模式下延迟重置标志
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        isCleanupRunning.set(false);
                    }
                }.runTaskLater(plugin, 20L); // 1秒后重置
            }
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
        
        // 排除特殊实体：矿车、船只、盔甲架等
        if (entity instanceof org.bukkit.entity.Minecart ||
            entity instanceof org.bukkit.entity.Boat ||
            entity instanceof org.bukkit.entity.ArmorStand ||
            entity instanceof org.bukkit.entity.ItemFrame ||
            entity instanceof org.bukkit.entity.Painting) {
            return false;
        }
        
        // 掉落物清理
        if (entity instanceof Item item) {
            if (!config.isCleanupItems()) return false;
            
            // 检查物品年龄
            int ageThreshold = config.getItemAgeThreshold() * 20; // 转换为tick
            return item.getTicksLived() > ageThreshold;
        }
        
        // 经验球清理
        if (entity instanceof ExperienceOrb) {
            return config.isCleanupExperienceOrbs();
        }
        
        // 箭矢清理（只清理普通箭矢，不清理三叉戟等特殊投射物）
        if (entity instanceof Arrow) {
            return config.isCleanupArrows();
        }
        
        // 凋落物清理
        if (entity instanceof org.bukkit.entity.FallingBlock) {
            return config.isCleanupFallingBlocks();
        }
        
        // 敌对生物清理 - 只清理一半（随机50%概率）
        if (entity instanceof Monster) {
            if (!config.isCleanupHostileMobs() || entity.getCustomName() != null) {
                return false;
            }
            // 50%概率清理怪物
            return Math.random() < 0.5;
        }
        
        // 被动生物不清理（按用户要求）
        if (entity instanceof Animals) {
            return false;
        }
        
        return false;
    }
    
    private void broadcastCleanupMessage(CleanupStats stats) {
        String message;
        
        if (config.isShowCleanupStats()) {
            message = String.format(
                    ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝扫掉了 " + ChatColor.RED + "%d" + ChatColor.WHITE + " 个实体呢~ (物品: " + ChatColor.YELLOW + "%d" + ChatColor.WHITE + ", 经验球: " + ChatColor.YELLOW + "%d" + ChatColor.WHITE + ", 箭矢: " + ChatColor.YELLOW + "%d" + ChatColor.WHITE + ", 凋落物: " + ChatColor.YELLOW + "%d" + ChatColor.WHITE + ", 生物: " + ChatColor.YELLOW + "%d" + ChatColor.WHITE + ")",
                    stats.getTotalCleaned(),
                    stats.getItemsCleaned(),
                    stats.getExperienceOrbsCleaned(),
                    stats.getArrowsCleaned(),
                    stats.getFallingBlocksCleaned(),
                    stats.getMobsCleaned()
            );
        } else {
            message = String.format(
                    ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝扫掉了 " + ChatColor.RED + "%d" + ChatColor.WHITE + " 个实体呢~",
                    stats.getTotalCleaned()
            );
        }
        
        // 向所有在线玩家广播消息
        Bukkit.broadcastMessage(message);
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
        plugin.getLogger().info("Entity cleanup handler shutdown");
    }
}