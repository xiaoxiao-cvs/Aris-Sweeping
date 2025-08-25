package com.xiaoxiao.arissweeping.handler;

import com.xiaoxiao.arissweeping.ArisSweeping;
import com.xiaoxiao.arissweeping.cleanup.CleanupServiceManager;
import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.CleanupStats;
import com.xiaoxiao.arissweeping.util.ClusterDetector;
import com.xiaoxiao.arissweeping.util.TpsMonitor;
import com.xiaoxiao.arissweeping.util.CleanupStateManager;
import com.xiaoxiao.arissweeping.util.CleanupStateManager.CleanupType;
import com.xiaoxiao.arissweeping.util.EntityTypeUtils;
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
    private final CleanupStateManager stateManager;
    private final CleanupServiceManager serviceManager;
    private BukkitTask cleanupTask;
    private BukkitTask densityCheckTask;
    
    public EntityCleanupHandler(ArisSweeping plugin) {
        this.plugin = plugin;
        this.config = plugin.getModConfig();
        this.tpsMonitor = new TpsMonitor(plugin);
        this.stateManager = CleanupStateManager.getInstance();
        this.serviceManager = new CleanupServiceManager(plugin);
    }
    
    public void init() {
        plugin.getLogger().info("Initializing Entity Cleanup Handler...");
        
        // 检查全局开关
        if (!config.isPluginEnabled()) {
            plugin.getLogger().warning("Entity Cleanup Handler initialization skipped - plugin disabled in config");
            return;
        }
        
        // 验证配置
        try {
            validateConfiguration();
        } catch (Exception e) {
            plugin.getLogger().warning("Configuration validation failed, using default values: " + e.getMessage());
        }
        
        // 初始化状态管理器
        // 状态管理现在由CleanupStateManager统一处理
        
        // 启动定时清理任务
        try {
            plugin.getLogger().info("Starting cleanup task...");
            startCleanupTask();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to start cleanup task: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 启动实体密度检查任务（每秒检查一次）
        try {
            plugin.getLogger().info("Starting density check task...");
            startDensityCheckTask();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to start density check task: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 启动TPS监控
        try {
            if (config.isTpsMonitorEnabled()) {
                plugin.getLogger().info("Starting TPS monitor...");
                tpsMonitor.startMonitoring();
            } else {
                plugin.getLogger().info("TPS monitor disabled in config");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to start TPS monitor: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 输出初始化状态
        try {
            logInitializationStatus();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to log initialization status: " + e.getMessage());
        }
        
        plugin.getLogger().info("Entity cleanup handler initialization completed!");
    }
    
    /**
     * 验证配置参数
     */
    private void validateConfiguration() {
        boolean hasErrors = false;
        
        try {
            // 验证清理间隔
            int cleanupInterval = config.getCleanupInterval();
            if (cleanupInterval <= 0) {
                plugin.getLogger().severe("Invalid cleanup interval: " + cleanupInterval + ". Must be positive.");
                hasErrors = true;
            } else if (cleanupInterval < 10) {
                plugin.getLogger().warning("Cleanup interval is very short (" + cleanupInterval + "s), this may cause performance issues");
            } else if (cleanupInterval > 7200) {
                plugin.getLogger().warning("Cleanup interval is very long (" + cleanupInterval + "s), entities may accumulate significantly");
            }
            
            // 验证实体密度阈值
            int densityThreshold = config.getEntityDensityThreshold();
            if (densityThreshold <= 0) {
                plugin.getLogger().severe("Invalid entity density threshold: " + densityThreshold + ". Must be positive.");
                hasErrors = true;
            } else if (densityThreshold < 100) {
                plugin.getLogger().warning("Entity density threshold is very low (" + densityThreshold + "), this may cause frequent warnings");
            } else if (densityThreshold > 10000) {
                plugin.getLogger().warning("Entity density threshold is very high (" + densityThreshold + "), this may not effectively prevent lag");
            }
            
            // 验证低TPS阈值
            if (config.isTpsMonitorEnabled()) {
                double lowTpsThreshold = config.getLowTpsThreshold();
                if (lowTpsThreshold <= 0.0 || lowTpsThreshold > 20.0) {
                    plugin.getLogger().severe("Invalid low TPS threshold: " + lowTpsThreshold + ". Must be between 0.1 and 20.0.");
                    hasErrors = true;
                } else if (lowTpsThreshold < 10.0) {
                    plugin.getLogger().warning("Low TPS threshold is very low (" + lowTpsThreshold + "), emergency cleanup may trigger too frequently");
                } else if (lowTpsThreshold > 19.0) {
                    plugin.getLogger().warning("Low TPS threshold is very high (" + lowTpsThreshold + "), emergency cleanup may not trigger when needed");
                }
            }
            
            // 验证其他关键配置
            if (config.getMaxEntitiesPerChunk() <= 0) {
                plugin.getLogger().severe("Invalid max entities per chunk: " + config.getMaxEntitiesPerChunk() + ". Must be positive.");
                hasErrors = true;
            }
            
            if (config.getMaxItemsPerChunk() <= 0) {
                plugin.getLogger().severe("Invalid max items per chunk: " + config.getMaxItemsPerChunk() + ". Must be positive.");
                hasErrors = true;
            }
            
            if (config.getBatchSize() <= 0) {
                plugin.getLogger().severe("Invalid batch size: " + config.getBatchSize() + ". Must be positive.");
                hasErrors = true;
            }
            
            if (config.getBatchDelay() < 0) {
                plugin.getLogger().severe("Invalid batch delay: " + config.getBatchDelay() + ". Must be non-negative.");
                hasErrors = true;
            }
            
            if (hasErrors) {
                String errorMessage = "配置验证失败，某些功能可能无法正常工作";
                plugin.getLogger().severe(errorMessage);
                
                // 向管理员公屏通知配置错误
                notifyConfigError(errorMessage);
                
                throw new IllegalStateException("Invalid configuration detected");
            }
            
            plugin.getLogger().info("Configuration validation completed successfully");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Configuration validation failed: " + e.getMessage());
            throw new RuntimeException("Configuration validation error", e);
        }
    }
    
    /**
     * 向管理员公屏通知配置错误
     */
    private void notifyConfigError(String errorMessage) {
        try {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    String message = ChatColor.RED + "[配置错误] " + 
                                   ChatColor.WHITE + errorMessage;
                    
                    // 向所有在线的OP发送消息
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.isOp()) {
                            player.sendMessage(message);
                        }
                    }
                    
                    // 同时在控制台输出
                    plugin.getLogger().severe("[配置错误通知] " + errorMessage);
                    
                } catch (Exception e) {
                    plugin.getLogger().severe("发送配置错误通知时发生异常: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            plugin.getLogger().severe("安排配置错误通知任务时发生异常: " + e.getMessage());
        }
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
    
    public void startCleanupTask() {
        try {
            // 停止现有任务
            try {
                if (cleanupTask != null && !cleanupTask.isCancelled()) {
                    cleanupTask.cancel();
                    plugin.getLogger().info("Previous cleanup task cancelled");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to cancel previous cleanup task: " + e.getMessage());
            }
            
            // 检查全局开关
            if (!config.isPluginEnabled()) {
                plugin.getLogger().info("Cleanup task not started - plugin disabled in config");
                return;
            }
            
            int tempInterval;
            try {
                tempInterval = config.getCleanupInterval();
                if (tempInterval <= 0) {
                    plugin.getLogger().warning("Invalid cleanup interval: " + tempInterval + ", using default 300 seconds");
                    tempInterval = 300;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to get cleanup interval from config: " + e.getMessage());
                tempInterval = 300; // 默认5分钟
            }
            final int intervalSeconds = tempInterval;
            
            long intervalTicks = intervalSeconds * 20L; // 转换为tick
            
            // 验证间隔设置
            if (intervalSeconds < 30) {
                plugin.getLogger().warning("Cleanup interval is very short (" + intervalSeconds + "s), this may cause performance issues");
            } else if (intervalSeconds > 3600) {
                plugin.getLogger().warning("Cleanup interval is very long (" + intervalSeconds + "s), entities may accumulate");
            }
            
            plugin.getLogger().info("Starting cleanup task with interval: " + intervalSeconds + " seconds (" + intervalTicks + " ticks)");
            
            try {
                plugin.getLogger().info("Cleanup configuration: async=" + config.isAsyncCleanup() + ", broadcast=" + config.isBroadcastCleanup() + ", showStats=" + config.isShowCleanupStats());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to log cleanup configuration: " + e.getMessage());
            }
            
            try {
                // 参考EC项目的简单定时器实现
                cleanupTask = new BukkitRunnable() {
                    private int executionCount = 0;
                    
                    @Override
                    public void run() {
                        try {
                            executionCount++;
                            
                            // 检查全局开关
                            try {
                                if (!config.isPluginEnabled()) {
                                    plugin.getLogger().info("Cleanup task #" + executionCount + " skipped - plugin disabled in config");
                                    return;
                                }
                            } catch (Exception e) {
                                plugin.getLogger().warning("Failed to check plugin enabled status in cleanup task: " + e.getMessage());
                                return;
                            }
                            
                            plugin.getLogger().info("Cleanup task #" + executionCount + " triggered (interval: " + intervalSeconds + "s)");
                            
                            try {
                                if (!stateManager.isCleanupRunning(CleanupStateManager.CleanupType.STANDARD)) {
                                    // 启动倒计时系统（仿照EC）
                                    startCleanupCountdown();
                                } else {
                                    plugin.getLogger().warning("Cleanup task #" + executionCount + " skipped - previous cleanup still running");
                                }
                            } catch (Exception e) {
                                plugin.getLogger().severe("Failed to start cleanup countdown in task #" + executionCount + ": " + e.getMessage());
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            plugin.getLogger().severe("Unexpected error in cleanup task #" + executionCount + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }.runTaskTimer(plugin, intervalTicks, intervalTicks);
                
                plugin.getLogger().info("Cleanup task started successfully. Next execution in " + intervalSeconds + " seconds");
                
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create and start cleanup task: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
            
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
        try {
            // 停止现有任务
            try {
                if (densityCheckTask != null) {
                    densityCheckTask.cancel();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to cancel previous density check task: " + e.getMessage());
            }
            
            try {
                densityCheckTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            // 检查全局开关
                            try {
                                if (!config.isPluginEnabled()) {
                                    return;
                                }
                            } catch (Exception e) {
                                plugin.getLogger().warning("Failed to check plugin enabled status in density check task: " + e.getMessage());
                                return;
                            }
                            
                            try {
                                checkEntityDensity();
                            } catch (Exception e) {
                                plugin.getLogger().severe("Failed to check entity density: " + e.getMessage());
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            plugin.getLogger().severe("Unexpected error in density check task: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }.runTaskTimer(plugin, 20L, 200L); // 每10秒检查一次，降低频率
                
                plugin.getLogger().info("Density check task started successfully");
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create and start density check task: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to start density check task: " + e.getMessage());
            e.printStackTrace();
        }
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
            Map<World, List<Entity>> worldEntitiesCache = new HashMap<>();
            
            // 一次性获取所有世界的实体，避免重复调用
            for (World world : Bukkit.getWorlds()) {
                try {
                    if (world == null) {
                        plugin.getLogger().warning("Skipping null world in density check");
                        continue;
                    }
                    
                    List<Entity> entities = world.getEntities();
                    if (entities == null) {
                        plugin.getLogger().warning("World " + world.getName() + " returned null entities list");
                        continue;
                    }
                    
                    worldEntitiesCache.put(world, entities);
                    int entityCount = entities.size();
                    totalEntities += entityCount;
                    
                    int threshold = config.getEntityDensityThreshold();
                    if (threshold == -1) {
                        // 密度检查已禁用
                        plugin.getLogger().fine("Entity density check disabled for world " + world.getName());
                        continue;
                    }
                    if (threshold <= 0) {
                        plugin.getLogger().warning("Invalid entity density threshold: " + threshold + ", skipping density check");
                        continue;
                    }
                    
                    if (entityCount > threshold) {
                        worldsWithHighDensity++;
                        String worldInfo = world.getName() + "(" + entityCount + "/" + threshold + ")";
                        highDensityWorlds.add(worldInfo);
                        
                        plugin.getLogger().warning("High entity density detected in world " + world.getName() + ": " + 
                                                  entityCount + " entities (threshold: " + threshold + ", " + 
                                                  String.format("%.1f", (double)entityCount/threshold*100) + "% of threshold)");
                        
                        // 分析实体类型分布（仅在调试模式下）
                        if (config.isDebugMode()) {
                            try {
                                Map<String, Integer> entityTypes = new HashMap<>();
                                for (Entity entity : entities) {
                                    if (entity != null && entity.getType() != null) {
                                        String type = entity.getType().name();
                                        entityTypes.put(type, entityTypes.getOrDefault(type, 0) + 1);
                                    }
                                }
                                
                                // 输出前5种最多的实体类型
                                entityTypes.entrySet().stream()
                                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                                    .limit(5)
                                    .forEach(entry -> plugin.getLogger().info("  " + entry.getKey() + ": " + entry.getValue()));
                            } catch (Exception debugException) {
                                plugin.getLogger().warning("Failed to analyze entity types for world " + world.getName() + ": " + debugException.getMessage());
                            }
                        }
                    } else {
                        plugin.getLogger().fine("Entity density normal in world " + world.getName() + ": " + 
                                              entityCount + "/" + threshold + " (" + 
                                              String.format("%.1f", (double)entityCount/threshold*100) + "% of threshold)");
                    }
                    
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to check entity density for world " + (world != null ? world.getName() : "unknown") + ": " + e.getMessage());
                    // 继续检查其他世界，不中断整个流程
                }
            }
            
            // 发送密度警告通知
            try {
                if (worldsWithHighDensity > 0) {
                    handleDensityWarningNotification(worldsWithHighDensity, highDensityWorlds, totalEntities);
                } else {
                    plugin.getLogger().fine("All worlds have normal entity density. Total entities: " + totalEntities);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send density warning notification: " + e.getMessage());
            }
            
            // 使用缓存的实体数据进行世界密度检查，避免重复获取
            for (Map.Entry<World, List<Entity>> entry : worldEntitiesCache.entrySet()) {
                World world = entry.getKey();
                try {
                    if (world == null) continue;
                    
                    if (config.isAsyncCleanup()) {
                        CompletableFuture.runAsync(() -> {
                            try {
                                checkWorldEntityDensityWithCache(world, entry.getValue());
                            } catch (Exception e) {
                                plugin.getLogger().warning("Async world density check failed for " + world.getName() + ": " + e.getMessage());
                            }
                        });
                    } else {
                        checkWorldEntityDensityWithCache(world, entry.getValue());
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to start world density check for " + (world != null ? world.getName() : "unknown") + ": " + e.getMessage());
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
        try {
            if (world == null) {
                plugin.getLogger().warning("检查世界实体密度时世界为空");
                return;
            }
            
            checkWorldEntityDensityWithCache(world, world.getEntities());
        } catch (Exception e) {
            plugin.getLogger().severe("检查世界 " + (world != null ? world.getName() : "unknown") + " 实体密度时发生严重错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void checkWorldEntityDensityWithCache(World world, List<Entity> worldEntities) {
        try {
            if (world == null) {
                plugin.getLogger().warning("检查世界实体密度时世界为空");
                return;
            }
            
            if (worldEntities == null || worldEntities.isEmpty()) {
                return;
            }
            
            Map<String, List<Entity>> chunkEntities = new HashMap<>();
            
            // 收集所有实体按区块分组，使用缓存的实体数据
            for (Entity entity : worldEntities) {
                try {
                    if (entity != null && entity.isValid()) {
                        String chunkKey = entity.getLocation().getChunk().getX() + "," + entity.getLocation().getChunk().getZ();
                        chunkEntities.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(entity);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("处理实体时发生错误: " + e.getMessage());
                }
            }
            
            // 检查每个区块的实体密度
            for (Map.Entry<String, List<Entity>> entry : chunkEntities.entrySet()) {
                try {
                    List<Entity> entities = entry.getValue();
                    
                    // 统计不同类型的实体
                    long itemCount = entities.stream().filter(e -> e instanceof Item).count();
                    long totalEntityCount = entities.size();
                    
                    // 如果超过阈值，进行清理
                    if (itemCount > config.getMaxItemsPerChunk() || 
                        totalEntityCount > config.getMaxEntitiesPerChunk()) {
                        
                        cleanupChunkEntities(entities);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("检查区块 " + entry.getKey() + " 实体密度时发生错误: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("检查世界 " + (world != null ? world.getName() : "unknown") + " 实体密度时发生严重错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void checkChunkEntityDensity(Chunk chunk) {
        try {
            if (chunk == null) {
                plugin.getLogger().warning("检查区块实体密度时区块为空");
                return;
            }
            
            Entity[] chunkEntities = chunk.getEntities();
            if (chunkEntities == null || chunkEntities.length == 0) {
                return;
            }
            
            int maxEntitiesPerChunk = config.getMaxEntitiesPerChunk();
            int maxItemsPerChunk = config.getMaxItemsPerChunk();
            
            // 快速检查：如果总实体数量没有超过阈值，只需要检查物品数量
            if (chunkEntities.length <= maxEntitiesPerChunk) {
                int itemCount = 0;
                for (Entity entity : chunkEntities) {
                    if (entity instanceof Item) {
                        itemCount++;
                        // 如果物品数量已经超标，可以提前退出
                        if (itemCount > maxItemsPerChunk) {
                            break;
                        }
                    }
                }
                
                // 如果物品数量也没超标，直接返回
                if (itemCount <= maxItemsPerChunk) {
                    return;
                }
            }
            
            // 只有在需要清理时才收集有效实体
            List<Entity> validEntities = new ArrayList<>();
            for (Entity entity : chunkEntities) {
                try {
                    if (entity != null && entity.isValid()) {
                        validEntities.add(entity);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("处理区块实体时发生错误: " + e.getMessage());
                }
            }
            
            // 执行清理
            if (!validEntities.isEmpty()) {
                cleanupChunkEntities(validEntities);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("检查区块实体密度时发生严重错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void performCleanup() {
        // 检查全局开关
        if (!config.isPluginEnabled()) {
            plugin.getLogger().info("Cleanup skipped - plugin disabled in config");
            return;
        }
        
        // 使用新的服务架构执行清理
        serviceManager.executeCleanup(CleanupType.STANDARD, null, config.isAsyncCleanup())
            .thenAccept(stats -> {
                // 在主线程中处理通知
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        handleCleanupNotification(stats);
                    }
                }.runTask(plugin);
            })
            .exceptionally(throwable -> {
                plugin.getLogger().severe("Error during cleanup: " + throwable.getMessage());
                throwable.printStackTrace();
                return null;
            });
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
        try {
            if (world == null) {
                plugin.getLogger().warning("异步清理世界时世界为空");
                return;
            }
            
            List<Entity> toRemove = new ArrayList<>();
            
            // 分批处理实体，避免一次性处理过多
            List<Entity> entities = new ArrayList<>(world.getEntities());
            int batchSize = Math.max(config.getBatchSize(), entities.size() / 10); // 使用配置的批次大小
            
            for (int i = 0; i < entities.size(); i += batchSize) {
                try {
                    int endIndex = Math.min(i + batchSize, entities.size());
                    List<Entity> batch = entities.subList(i, endIndex);
                    
                    for (Entity entity : batch) {
                        try {
                            if (entity != null && entity.isValid() && shouldCleanupEntity(entity)) {
                                toRemove.add(entity);
                                stats.incrementType(entity);
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("处理实体时发生错误: " + e.getMessage());
                        }
                    }
                    
                    // 分批移除实体，减少主线程压力
                    if (!toRemove.isEmpty()) {
                        List<Entity> currentBatch = new ArrayList<>(toRemove);
                        toRemove.clear();
                        
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                try {
                                    for (Entity entity : currentBatch) {
                                        try {
                                            if (entity != null && entity.isValid()) {
                                                entity.remove();
                                            }
                                        } catch (Exception e) {
                                            plugin.getLogger().warning("移除实体时发生错误: " + e.getMessage());
                                        }
                                    }
                                } catch (Exception e) {
                                    plugin.getLogger().warning("批量移除实体时发生错误: " + e.getMessage());
                                }
                            }
                        }.runTask(plugin);
                        
                        // 批次间延迟，避免卡顿
                        if (config.getBatchDelay() > 0) {
                            try {
                                Thread.sleep(config.getBatchDelay());
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("处理批次 " + (i / batchSize + 1) + " 时发生错误: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("异步清理世界 " + (world != null ? world.getName() : "unknown") + " 时发生严重错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void cleanupWorldSync(World world, CleanupStats stats) {
        try {
            if (world == null) {
                plugin.getLogger().warning("同步清理世界时世界为空");
                return;
            }
            
            List<Entity> toRemove = new ArrayList<>();
            int processedCount = 0;
            int maxPerTick = config.getMaxChunksPerTick() * 50; // 每tick最大处理实体数
            
            for (Entity entity : world.getEntities()) {
                try {
                    if (entity != null && entity.isValid() && shouldCleanupEntity(entity)) {
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
                } catch (Exception e) {
                    plugin.getLogger().warning("处理实体时发生错误: " + e.getMessage());
                }
            }
            
            // 处理剩余实体
            if (!toRemove.isEmpty()) {
                removeEntitiesBatch(toRemove);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("同步清理世界 " + (world != null ? world.getName() : "unknown") + " 时发生严重错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void removeEntitiesBatch(List<Entity> entities) {
        try {
            if (entities == null || entities.isEmpty()) {
                return;
            }
            
            for (Entity entity : entities) {
                try {
                    if (entity != null && entity.isValid()) {
                        entity.remove();
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("移除单个实体时发生错误: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("批量移除实体时发生严重错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void cleanupChunkEntities(List<Entity> entities) {
        try {
            if (entities == null || entities.isEmpty()) {
                return;
            }
            
            List<Entity> toRemove = new ArrayList<>();
            
            for (Entity entity : entities) {
                try {
                    if (entity != null && entity.isValid() && shouldCleanupEntity(entity)) {
                        toRemove.add(entity);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("检查区块实体清理条件时发生错误: " + e.getMessage());
                }
            }
            
            // 在主线程中移除实体
            if (!toRemove.isEmpty()) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            for (Entity entity : toRemove) {
                                try {
                                    if (entity != null && entity.isValid()) {
                                        entity.remove();
                                    }
                                } catch (Exception e) {
                                    plugin.getLogger().warning("移除区块实体时发生错误: " + e.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("批量移除区块实体时发生错误: " + e.getMessage());
                        }
                    }
                }.runTask(plugin);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("清理区块实体时发生严重错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private boolean shouldCleanupEntity(Entity entity) {
        try {
            if (entity == null || !entity.isValid()) {
                return false;
            }
            
            // 永远不清理玩家
            if (entity instanceof Player) {
                return false;
            }
            
            // 掉落物清理 - 参考EntityClearer，跳过所有检查直接清理
            if (entity instanceof Item item) {
                try {
                    if (!config.isCleanupItems()) return false;
                    
                    // 保护有自定义名称的物品（如玩家重命名的装备）
                    if (item.getCustomName() != null) {
                        return false;
                    }
                    
                    // 直接清理掉落物，不进行聚集检测和智能保护检查
                    return true;
                } catch (Exception e) {
                    plugin.getLogger().warning("检查掉落物清理条件时发生错误: " + e.getMessage());
                    return false;
                }
            }
            
            // 智能保护检查
            try {
                if (ClusterDetector.shouldProtectEntity(entity)) {
                    return false;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("智能保护检查时发生错误: " + e.getMessage());
                return false;
            }
            
            // 使用统一的保护实体判断
            if (EntityTypeUtils.isProtectedEntity(entity)) {
                return false;
            }
            
            // 聚集清理检查
            try {
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
            } catch (Exception e) {
                plugin.getLogger().warning("聚集清理检查时发生错误: " + e.getMessage());
                return false;
            }
            
            // 经验球清理
            if (EntityTypeUtils.isCleanableExperienceOrb(entity)) {
                return config.isCleanupExperienceOrbs();
            }
            
            // 箭矢清理（只清理普通箭矢，不清理三叉戟等特殊投射物）
            if (EntityTypeUtils.isCleanableArrow(entity)) {
                return config.isCleanupArrows();
            }
            
            // 掉落物（掉落方块）清理
            if (EntityTypeUtils.isCleanableFallingBlock(entity)) {
                return config.isCleanupFallingBlocks();
            }
            
            // 敌对生物清理
            if (entity instanceof Monster) {
                try {
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
                } catch (Exception e) {
                    plugin.getLogger().warning("检查敌对生物清理条件时发生错误: " + e.getMessage());
                    return false;
                }
            }
            
            // 被动生物不清理（按用户要求）
            if (entity instanceof Animals) {
                return false;
            }
            
            return false;
        } catch (Exception e) {
            plugin.getLogger().severe("检查实体清理条件时发生严重错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
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
        try {
            if (!config.isEmergencyCleanupEnabled()) {
                plugin.getLogger().info("Emergency cleanup skipped - emergency cleanup disabled in config");
                return;
            }
            
            // 检查全局开关
            if (!config.isPluginEnabled()) {
                plugin.getLogger().info("Emergency cleanup skipped - plugin disabled in config");
                return;
            }
            
            if (!stateManager.tryStartCleanup(CleanupStateManager.CleanupType.EMERGENCY, "TPS_MONITOR")) {
                plugin.getLogger().info("Emergency cleanup skipped - already running");
                return; // 已经在运行紧急清理
            }
            
            plugin.getLogger().warning("Performing emergency cleanup due to low TPS (current: " + tpsMonitor.getCurrentTps() + ", threshold: " + config.getLowTpsThreshold() + ")");
            
            // 发送紧急清理开始通知
            try {
                if (config.isBroadcastCleanup()) {
                    String startMessage = ChatColor.RED + "[紧急清理] " + ChatColor.YELLOW + "检测到服务器TPS过低，爱丽丝开始紧急清理实体...";
                    Bukkit.broadcastMessage(startMessage);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to broadcast emergency cleanup start message: " + e.getMessage());
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
                        if (world == null) {
                            plugin.getLogger().warning("Skipping emergency cleanup for invalid world");
                            continue;
                        }
                        
                        List<Entity> entities = new ArrayList<>(world.getEntities());
                        
                        // 使用聚集清理算法获取清理候选
                        if (config.isClusterCleanupEnabled()) {
                            plugin.getLogger().info("Using cluster cleanup algorithm for emergency cleanup in world: " + world.getName());
                            try {
                                List<Entity> candidates = ClusterDetector.getClusterCleanupCandidates(
                                    entities,
                                    config.getClusterDetectionDistance(),
                                    config.getMinClusterSize(),
                                    config.getClusterPreserveRatio()
                                );
                                
                                for (Entity entity : candidates) {
                                    try {
                                        if (entity != null && entity.isValid() && shouldCleanupEntity(entity)) {
                                            entity.remove();
                                            stats.incrementType(entity);
                                        }
                                    } catch (Exception e) {
                                        plugin.getLogger().warning("Failed to remove entity during emergency cluster cleanup: " + e.getMessage());
                                    }
                                }
                            } catch (Exception e) {
                                plugin.getLogger().severe("Emergency cluster cleanup failed for world " + world.getName() + ": " + e.getMessage());
                                e.printStackTrace();
                            }
                        } else {
                            plugin.getLogger().info("Using standard cleanup algorithm for emergency cleanup in world: " + world.getName());
                            // 常规清理
                            try {
                                for (Entity entity : entities) {
                                    try {
                                        if (entity != null && entity.isValid() && shouldCleanupEntity(entity)) {
                                            entity.remove();
                                            stats.incrementType(entity);
                                        }
                                    } catch (Exception e) {
                                        plugin.getLogger().warning("Failed to remove entity during emergency standard cleanup: " + e.getMessage());
                                    }
                                }
                            } catch (Exception e) {
                                plugin.getLogger().severe("Emergency standard cleanup failed for world " + world.getName() + ": " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                        
                        plugin.getLogger().info("Emergency cleanup completed for world: " + world.getName());
                    } catch (Exception e) {
                        plugin.getLogger().severe("Emergency cleanup failed for world " + (world != null ? world.getName() : "null") + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                long duration = System.currentTimeMillis() - startTime;
                plugin.getLogger().info("Emergency cleanup completed in " + duration + "ms, cleaned " + stats.getTotalCleaned() + " entities");
                
                // 统一处理紧急清理通知
                try {
                    handleEmergencyCleanupNotification(stats, duration);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to handle emergency cleanup notification: " + e.getMessage());
                }
                
            } catch (Exception e) {
                plugin.getLogger().severe("Emergency cleanup failed: " + e.getMessage());
                e.printStackTrace();
                
                // 发送错误通知
                try {
                    if (config.isBroadcastCleanup()) {
                        String errorMessage = ChatColor.RED + "[紧急清理] " + ChatColor.WHITE + "清理过程中发生错误，请查看控制台日志";
                        Bukkit.broadcastMessage(errorMessage);
                    }
                } catch (Exception broadcastException) {
                    plugin.getLogger().warning("Failed to broadcast emergency cleanup error message: " + broadcastException.getMessage());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Critical error in performEmergencyCleanup: " + e.getMessage());
            e.printStackTrace();
        } finally {
            stateManager.completeCleanup(CleanupStateManager.CleanupType.EMERGENCY);
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
    
    public void stopCleanupTask() {
        try {
            if (cleanupTask != null && !cleanupTask.isCancelled()) {
                cleanupTask.cancel();
                plugin.getLogger().info("Cleanup task stopped");
            }
            if (densityCheckTask != null && !densityCheckTask.isCancelled()) {
                densityCheckTask.cancel();
                plugin.getLogger().info("Density check task stopped");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to stop cleanup tasks: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void restartCleanupTask() {
        stopCleanupTask();
        startCleanupTask();
        plugin.getLogger().info("Cleanup task restarted with new interval: " + config.getCleanupInterval() + " seconds");
    }
    
    /**
     * 获取任务状态信息
     */
    public String getTaskStatusInfo() {
        StringBuilder status = new StringBuilder();
        status.append("EntityCleanupHandler 任务状态:\n");
        
        // 清理任务状态
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            status.append("- 清理任务: 运行中\n");
        } else {
            status.append("- 清理任务: 已停止\n");
        }
        
        // 密度检查任务状态
        if (densityCheckTask != null && !densityCheckTask.isCancelled()) {
            status.append("- 密度检查任务: 运行中\n");
        } else {
            status.append("- 密度检查任务: 已停止\n");
        }
        
        // TPS监控状态
        if (tpsMonitor != null) {
            status.append("- TPS监控: ").append(config.isTpsMonitorEnabled() ? "启用" : "禁用").append("\n");
        } else {
            status.append("- TPS监控: 未初始化\n");
        }
        
        // 清理状态
        status.append("- 清理进行中: ").append(stateManager.isCleanupRunning(CleanupStateManager.CleanupType.STANDARD) ? "是" : "否").append("\n");
        status.append("- 紧急清理进行中: ").append(stateManager.isCleanupRunning(CleanupStateManager.CleanupType.EMERGENCY) ? "是" : "否");
        
        return status.toString();
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