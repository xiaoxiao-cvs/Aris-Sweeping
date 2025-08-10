package com.xiaoxiao.arissweeping.command;

import com.xiaoxiao.arissweeping.ArisSweeping;
import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.CleanupStats;
import com.xiaoxiao.arissweeping.permission.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CleanupCommand implements CommandExecutor, TabCompleter {
    private final ArisSweeping plugin;
    private final ModConfig config;
    private final PermissionManager permissionManager;
    
    // 清理状态管理
    private volatile boolean isCleanupRunning = false;
    private volatile long lastCleanupTime = 0;
    private static final long CLEANUP_COOLDOWN = 5000; // 5秒冷却时间
    
    public CleanupCommand(ArisSweeping plugin) {
        this.plugin = plugin;
        this.config = plugin.getModConfig();
        this.permissionManager = plugin.getPermissionManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 检查基础权限
        if (!hasAnyPermission(sender)) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 老师~没有权限指挥爱丽丝哦~");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "cleanup":
            case "clean":
                handleCleanupCommand(sender, args);
                break;
            case "stats":
                handleStatsCommand(sender);
                break;
            case "tps":
                handleTpsCommand(sender);
                break;
            case "config":
                handleConfigCommand(sender, args);
                break;
            case "toggle":
            case "switch":
                handleToggleCommand(sender);
                break;
            case "permission":
            case "perm":
                handlePermissionCommand(sender, args);
                break;
            case "test":
                handleTestCommand(sender, args);
                break;
            case "livestock-stats":
            case "livestock":
                handleLivestockStatsCommand(sender);
                break;
            case "help":
            default:
                sendHelp(sender);
                break;
        }
        
        return true;
    }
    
    private void handleCleanupCommand(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PermissionManager.CLEANUP)) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 老师~没有执行清理的权限哦~");
            return;
        }
        
        // 检查全局开关
        if (!config.isPluginEnabled()) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 爱丽丝的清理功能已被禁用，请先启用插件哦~");
            return;
        }
        
        // 检查清理冷却时间
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime < CLEANUP_COOLDOWN) {
            long remainingTime = (CLEANUP_COOLDOWN - (currentTime - lastCleanupTime)) / 1000;
            sender.sendMessage(ChatColor.YELLOW + "[邦邦卡邦！] 爱丽丝还需要休息 " + remainingTime + " 秒才能再次清理哦~");
            return;
        }
        
        // 检查是否有清理正在进行
        if (isCleanupRunning) {
            sender.sendMessage(ChatColor.YELLOW + "[邦邦卡邦！] 爱丽丝正在努力清理中，请稍等一下哦~");
            return;
        }
        
        try {
            if (args.length == 1) {
                // 标准清理
                executeStandardCleanup(sender);
            } else {
                switch (args[1].toLowerCase()) {
                    case "items":
                        executeCleanupItems(sender);
                        break;
                    case "mobs":
                        executeCleanupMobs(sender);
                        break;
                    case "all":
                        executeCleanupAll(sender);
                        break;
                    default:
                        sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 爱丽丝不知道要清理什么呢~ 请使用: items, mobs, all");
                        break;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("清理命令执行时发生错误: " + e.getMessage());
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 清理过程中发生了错误，请查看控制台日志！");
            isCleanupRunning = false; // 确保状态重置
        }
    }
    
    private void executeStandardCleanup(CommandSender sender) {
        isCleanupRunning = true;
        lastCleanupTime = System.currentTimeMillis();
        
        try {
            // 强制清理模式，添加倒数提示
            if (config.isBroadcastCleanup()) {
                Bukkit.broadcastMessage(ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝即将开始强制清理，" + ChatColor.RED + "10秒" + ChatColor.WHITE + "后开始清理！");
            }
            
            plugin.getLogger().info("开始执行标准清理，执行者: " + sender.getName());
            
            // 10秒倒数
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (config.isBroadcastCleanup()) {
                        Bukkit.broadcastMessage(ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "强制清理倒数：" + ChatColor.RED + "5秒" + ChatColor.WHITE + "！请注意保护重要物品！");
                    }
                }
            }.runTaskLater(plugin, 100L); // 5秒后执行（10-5=5秒）
            
            // 执行强制清理
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        // 输出光呀！
                        if (config.isBroadcastCleanup()) {
                            Bukkit.broadcastMessage(ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.YELLOW + "光呀！");
                        }
                        CleanupStats stats = performForceCleanup(); // 使用强制清理方法
                        
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                try {
                                    if (stats.getTotalCleaned() > 0) {
                                        String message = String.format(
                                            ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝强制扫掉了 " + ChatColor.RED + "%d" + ChatColor.WHITE + " 个实体呢~ 老师~ (物品: " + ChatColor.YELLOW + "%d" + ChatColor.WHITE + ", 经验球: " + ChatColor.YELLOW + "%d" + ChatColor.WHITE + ", 箭矢: " + ChatColor.YELLOW + "%d" + ChatColor.WHITE + ", 掉落物: " + ChatColor.YELLOW + "%d" + ChatColor.WHITE + ", 生物: " + ChatColor.YELLOW + "%d" + ChatColor.WHITE + ")",
                                            stats.getTotalCleaned(),
                                            stats.getItemsCleaned(),
                                            stats.getExperienceOrbsCleaned(),
                                            stats.getArrowsCleaned(),
                                            stats.getFallingBlocksCleaned(),
                                            stats.getMobsCleaned()
                                        );
                                        if (config.isBroadcastCleanup()) {
                                            Bukkit.broadcastMessage(message);
                                        } else {
                                            sender.sendMessage(message);
                                        }
                                        plugin.getLogger().info("标准清理完成: " + stats.getTotalCleaned() + " 个实体被清理");
                                    } else {
                                        String message = ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝没有找到需要打扫的东西呢~";
                                        if (config.isBroadcastCleanup()) {
                                            Bukkit.broadcastMessage(message);
                                        } else {
                                            sender.sendMessage(message);
                                        }
                                        plugin.getLogger().info("标准清理完成: 没有找到需要清理的实体");
                                    }
                                } finally {
                                    isCleanupRunning = false;
                                }
                            }
                        }.runTask(plugin);
                    } catch (Exception e) {
                        plugin.getLogger().severe("执行强制清理时发生错误: " + e.getMessage());
                        e.printStackTrace();
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 清理过程中发生了错误，请查看控制台日志！");
                                isCleanupRunning = false;
                            }
                        }.runTask(plugin);
                    }
                }
            }.runTaskLaterAsynchronously(plugin, 200L); // 10秒后执行
        } catch (Exception e) {
            plugin.getLogger().severe("启动标准清理时发生错误: " + e.getMessage());
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 启动清理时发生了错误，请查看控制台日志！");
            isCleanupRunning = false;
        }
    }
    
    private void executeCleanupItems(CommandSender sender) {
        if (!config.isCleanupItems()) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 掉落物清理功能已被禁用哦~");
            return;
        }
        
        isCleanupRunning = true;
        lastCleanupTime = System.currentTimeMillis();
        
        try {
            plugin.getLogger().info("开始执行掉落物清理，执行者: " + sender.getName());
            
            CleanupStats stats = new CleanupStats();
            List<Entity> toRemove = new ArrayList<>();
            
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Item) {
                        // 保护有自定义名称的物品
                        if (((Item) entity).getCustomName() != null) {
                            continue;
                        }
                        toRemove.add(entity);
                        stats.incrementItems();
                    }
                }
            }
            
            // 在主线程中移除实体
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        int removed = 0;
                        for (Entity entity : toRemove) {
                            if (entity.isValid()) {
                                entity.remove();
                                removed++;
                            }
                        }
                        
                        String message = String.format(
                            ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝清理了 " + ChatColor.RED + "%d" + ChatColor.WHITE + " 个掉落物呢~",
                            removed
                        );
                        sender.sendMessage(message);
                        plugin.getLogger().info("掉落物清理完成: " + removed + " 个物品被清理");
                    } catch (Exception e) {
                        plugin.getLogger().severe("清理掉落物时发生错误: " + e.getMessage());
                        sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 清理掉落物时发生了错误，请查看控制台日志！");
                    } finally {
                        isCleanupRunning = false;
                    }
                }
            }.runTask(plugin);
        } catch (Exception e) {
            plugin.getLogger().severe("启动掉落物清理时发生错误: " + e.getMessage());
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 启动掉落物清理时发生了错误，请查看控制台日志！");
            isCleanupRunning = false;
        }
    }
    
    private void executeCleanupMobs(CommandSender sender) {
        if (!config.isCleanupHostileMobs()) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 敌对生物清理功能已被禁用哦~");
            return;
        }
        
        isCleanupRunning = true;
        lastCleanupTime = System.currentTimeMillis();
        
        try {
            plugin.getLogger().info("开始执行敌对生物清理，执行者: " + sender.getName());
            
            CleanupStats stats = new CleanupStats();
            List<Entity> toRemove = new ArrayList<>();
            
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Monster) {
                        // 保护有自定义名称的怪物
                        if (entity.getCustomName() != null) {
                            continue;
                        }
                        // 保护被拴住的怪物
                        if (entity instanceof LivingEntity livingEntity && livingEntity.isLeashed()) {
                            continue;
                        }
                        toRemove.add(entity);
                        stats.incrementMobs();
                    }
                }
            }
            
            // 在主线程中移除实体
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        int removed = 0;
                        for (Entity entity : toRemove) {
                            if (entity.isValid()) {
                                entity.remove();
                                removed++;
                            }
                        }
                        
                        String message = String.format(
                            ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝清理了 " + ChatColor.RED + "%d" + ChatColor.WHITE + " 个敌对生物呢~",
                            removed
                        );
                        sender.sendMessage(message);
                        plugin.getLogger().info("敌对生物清理完成: " + removed + " 个生物被清理");
                    } catch (Exception e) {
                        plugin.getLogger().severe("清理敌对生物时发生错误: " + e.getMessage());
                        sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 清理敌对生物时发生了错误，请查看控制台日志！");
                    } finally {
                        isCleanupRunning = false;
                    }
                }
            }.runTask(plugin);
        } catch (Exception e) {
            plugin.getLogger().severe("启动敌对生物清理时发生错误: " + e.getMessage());
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 启动敌对生物清理时发生了错误，请查看控制台日志！");
            isCleanupRunning = false;
        }
    }
    
    private void executeCleanupAll(CommandSender sender) {
        isCleanupRunning = true;
        lastCleanupTime = System.currentTimeMillis();
        
        try {
            plugin.getLogger().info("开始执行全部实体清理，执行者: " + sender.getName());
            
            CleanupStats stats = new CleanupStats();
            List<Entity> toRemove = new ArrayList<>();
            
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (!(entity instanceof Player) && shouldForceCleanup(entity)) {
                        toRemove.add(entity);
                        stats.incrementType(entity);
                    }
                }
            }
            
            // 在主线程中移除实体
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        int removed = 0;
                        for (Entity entity : toRemove) {
                            if (entity.isValid()) {
                                entity.remove();
                                removed++;
                            }
                        }
                        
                        String message = String.format(
                            ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝强制清理了 " + ChatColor.RED + "%d" + ChatColor.WHITE + " 个实体呢~",
                            removed
                        );
                        sender.sendMessage(message);
                        plugin.getLogger().info("全部实体清理完成: " + removed + " 个实体被清理");
                    } catch (Exception e) {
                        plugin.getLogger().severe("清理全部实体时发生错误: " + e.getMessage());
                        sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 清理全部实体时发生了错误，请查看控制台日志！");
                    } finally {
                        isCleanupRunning = false;
                    }
                }
            }.runTask(plugin);
        } catch (Exception e) {
            plugin.getLogger().severe("启动全部实体清理时发生错误: " + e.getMessage());
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 启动全部实体清理时发生了错误，请查看控制台日志！");
            isCleanupRunning = false;
        }
    }
    
    private void handleStatsCommand(CommandSender sender) {
        if (!hasPermission(sender, PermissionManager.STATS)) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 老师~没有查看统计的权限哦~");
            return;
        }
        // 使用Bukkit原生API获取服务器统计信息
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        int loadedWorlds = Bukkit.getWorlds().size();
        int totalChunks = 0;
        int totalEntities = 0;
        int items = 0;
        int hostileMobs = 0;
        int passiveMobs = 0;
        int experienceOrbs = 0;
        int arrows = 0;
        int fallingBlocks = 0;
        
        for (World world : Bukkit.getWorlds()) {
            totalChunks += world.getLoadedChunks().length;
            for (Entity entity : world.getEntities()) {
                totalEntities++;
                if (entity instanceof Item) items++;
                else if (entity instanceof Monster) hostileMobs++;
                else if (entity instanceof Animals) passiveMobs++;
                else if (entity instanceof ExperienceOrb) experienceOrbs++;
                else if (entity instanceof Arrow) arrows++;
                else if (entity instanceof org.bukkit.entity.FallingBlock) fallingBlocks++;
            }
        }
        
        // 获取服务器性能信息
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024; // MB
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024; // MB
        String serverVersion = Bukkit.getVersion();
        String bukkitVersion = Bukkit.getBukkitVersion();
        int maxPlayers = Bukkit.getMaxPlayers();
        long serverStartTime = System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().getUptime();
        long uptimeHours = ManagementFactory.getRuntimeMXBean().getUptime() / (1000 * 60 * 60);
        long uptimeMinutes = (ManagementFactory.getRuntimeMXBean().getUptime() / (1000 * 60)) % 60;
        
        // 获取TPS信息
        String tpsInfo = "";
        if (config.isTpsMonitorEnabled()) {
            double currentTps = plugin.getCleanupHandler().getTpsMonitor().getCurrentTps();
            String tpsStatus = plugin.getCleanupHandler().getTpsMonitor().getTpsStatus();
            ChatColor tpsColor = currentTps >= 18.0 ? ChatColor.GREEN : 
                               currentTps >= 15.0 ? ChatColor.YELLOW : ChatColor.RED;
            tpsInfo = ChatColor.WHITE + "当前TPS: " + tpsColor + String.format("%.2f", currentTps) + 
                     ChatColor.WHITE + " (" + tpsColor + tpsStatus + ChatColor.WHITE + ")\n";
        }
        
        sender.sendMessage(String.format(
            ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝为老师~统计了服务器情况~\n" +
            ChatColor.AQUA + "=== 服务器状态 ===\n" +
            ChatColor.WHITE + "服务器版本: " + ChatColor.YELLOW + "%s\n" +
            ChatColor.WHITE + "运行时间: " + ChatColor.YELLOW + "%d小时%d分钟\n" +
            tpsInfo +
            ChatColor.WHITE + "在线玩家: " + ChatColor.GREEN + "%d/%d\n" +
            ChatColor.WHITE + "已加载世界: " + ChatColor.YELLOW + "%d\n" +
            ChatColor.WHITE + "已加载区块: " + ChatColor.YELLOW + "%d\n" +
            ChatColor.WHITE + "内存使用: " + ChatColor.YELLOW + "%d/%dMB (%.1f%%)\n" +
            ChatColor.AQUA + "=== 实体统计 ===\n" +
            ChatColor.WHITE + "总实体数: " + ChatColor.YELLOW + "%d\n" +
            ChatColor.WHITE + "掉落物: " + ChatColor.YELLOW + "%d\n" +
            ChatColor.WHITE + "敌对生物: " + ChatColor.RED + "%d\n" +
            ChatColor.WHITE + "被动生物: " + ChatColor.GREEN + "%d\n" +
            ChatColor.WHITE + "经验球: " + ChatColor.YELLOW + "%d\n" +
            ChatColor.WHITE + "箭矢: " + ChatColor.YELLOW + "%d\n" +
            ChatColor.WHITE + "掉落方块: " + ChatColor.YELLOW + "%d",
            bukkitVersion, uptimeHours, uptimeMinutes, onlinePlayers, maxPlayers, loadedWorlds, totalChunks, 
            usedMemory, maxMemory, (double)usedMemory/maxMemory*100,
            totalEntities, items, hostileMobs, passiveMobs, experienceOrbs, arrows, fallingBlocks
        ));
    }
    
    private void handleTpsCommand(CommandSender sender) {
        if (!hasPermission(sender, PermissionManager.STATS)) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 老师~没有查看TPS的权限哦~");
            return;
        }
        if (!config.isTpsMonitorEnabled()) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] TPS监控功能未启用，请先在配置中启用哦~");
            return;
        }
        
        double currentTps = plugin.getCleanupHandler().getTpsMonitor().getCurrentTps();
        String tpsStatus = plugin.getCleanupHandler().getTpsMonitor().getTpsStatus();
        
        ChatColor tpsColor = currentTps >= 18.0 ? ChatColor.GREEN : 
                           currentTps >= 15.0 ? ChatColor.YELLOW : ChatColor.RED;
        
        sender.sendMessage(
            ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝为老师~检测了服务器性能~\n" +
            ChatColor.AQUA + "=== TPS 状态 ===\n" +
            ChatColor.WHITE + "当前TPS: " + tpsColor + String.format("%.2f", currentTps) + "\n" +
            ChatColor.WHITE + "状态: " + tpsColor + tpsStatus + "\n" +
            ChatColor.WHITE + "低TPS阈值: " + ChatColor.YELLOW + config.getLowTpsThreshold() + "\n" +
            ChatColor.WHITE + "紧急清理: " + (config.isEmergencyCleanupEnabled() ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用")
        );
    }
    
    private void handleConfigCommand(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PermissionManager.CONFIG)) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 老师~没有修改配置的权限哦~");
            return;
        }
        if (args.length < 2) {
            sendConfigHelp(sender);
            return;
        }
        
        switch (args[1].toLowerCase()) {
            case "interval":
                handleIntervalConfig(sender, args);
                break;
            case "enable":
                handleBooleanConfig(sender, args, "global.enabled", "插件总开关");
                break;
            case "cleanup-items":
                handleBooleanConfig(sender, args, "entity_cleanup.cleanupItems", "掉落物清理");
                break;
            case "cleanup-mobs":
                handleBooleanConfig(sender, args, "entity_cleanup.cleanupHostileMobs", "敌对生物清理");
                break;
            case "cleanup-animals":
                handleBooleanConfig(sender, args, "entity_cleanup.cleanupPassiveMobs", "被动生物清理");
                break;
            case "cleanup-arrows":
                handleBooleanConfig(sender, args, "entity_cleanup.cleanupArrows", "箭矢清理");
                break;
            case "cleanup-falling":
                handleBooleanConfig(sender, args, "entity_cleanup.cleanupFallingBlocks", "掉落物清理");
                break;
            case "livestock-check":
                handleBooleanConfig(sender, args, "livestock.enableDensityCheck", "畜牧业密度检测");
                break;
            case "livestock-limit":
                handleIntConfig(sender, args, "livestock.maxAnimalsPerChunk", "每区块最大动物数", 1, 100);
                break;
            case "warning-time":
                handleIntConfig(sender, args, "livestock.warningTime", "预警时间(分钟)", 1, 30);
                break;
            case "broadcast":
                handleBooleanConfig(sender, args, "messages.broadcastCleanup", "清理消息广播");
                break;
            case "show-stats":
                handleBooleanConfig(sender, args, "messages.showCleanupStats", "详细统计显示");
                break;
            case "item-age":
                handleIntConfig(sender, args, "thresholds.itemAgeThreshold", "掉落物年龄阈值(秒)", 30, 600);
                break;
            case "reload":
                config.reload();
                sender.sendMessage(ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝重新加载了配置~");
                break;
            case "list":
                sendConfigList(sender);
                break;
            default:
                sendConfigHelp(sender);
                break;
        }
    }
    
    private void handleIntervalConfig(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 用法: /arissweeping config interval <秒数>");
            return;
        }
        
        try {
            int interval = Integer.parseInt(args[2]);
            if (interval < 30 || interval > 3600) {
                sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 爱丽丝的清理间隔必须在30-3600秒之间哦~");
                return;
            }
            
            config.setCleanupInterval(interval);
            sender.sendMessage(String.format(
                ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝的清理间隔已设置为 " + ChatColor.YELLOW + "%d" + ChatColor.WHITE + " 秒呢~",
                interval
            ));
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 爱丽丝需要一个有效的数字哦~");
        }
    }
    
    private void handleBooleanConfig(CommandSender sender, String[] args, String configPath, String displayName) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 用法: /arissweeping config " + args[1] + " <true|false>");
            return;
        }
        
        boolean value = Boolean.parseBoolean(args[2]);
        config.getConfig().set(configPath, value);
        config.saveConfig();
        
        String message = String.format(
            ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝已将 " + ChatColor.YELLOW + "%s" + 
            ChatColor.WHITE + " 设置为 " + (value ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭") + ChatColor.WHITE + "~",
            displayName
        );
        
        sender.sendMessage(message);
        
        // 通知所有OP和权限用户
        permissionManager.notifyConfigChange(displayName, "未知", (value ? "开启" : "关闭"));
    }
    
    private void handleIntConfig(CommandSender sender, String[] args, String configPath, String displayName, int min, int max) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 用法: /arissweeping config " + args[1] + " <数值>");
            return;
        }
        
        try {
            int value = Integer.parseInt(args[2]);
            if (value < min || value > max) {
                sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 数值必须在 " + min + "-" + max + " 之间哦~");
                return;
            }
            
            config.getConfig().set(configPath, value);
            config.saveConfig();
            
            String message = String.format(
                ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝已将 " + ChatColor.YELLOW + "%s" + 
                ChatColor.WHITE + " 设置为 " + ChatColor.YELLOW + "%d" + ChatColor.WHITE + "~",
                displayName, value
            );
            
            sender.sendMessage(message);
            
            // 通知所有OP和权限用户
            permissionManager.notifyConfigChange(displayName, "未知", String.valueOf(value));
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 爱丽丝需要一个有效的数字哦~");
        }
    }
    
    private void sendConfigHelp(CommandSender sender) {
        sender.sendMessage(
            ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝的配置指令帮助~ 老师~\n" +
            ChatColor.YELLOW + "基础设置:\n" +
            ChatColor.WHITE + "  /arissweeping config enable <true|false> - 插件总开关\n" +
            ChatColor.WHITE + "  /arissweeping config interval <秒> - 清理间隔\n" +
            ChatColor.YELLOW + "清理设置:\n" +
            ChatColor.WHITE + "  /arissweeping config cleanup-items <true|false> - 掉落物清理\n" +
            ChatColor.WHITE + "  /arissweeping config cleanup-mobs <true|false> - 敌对生物清理\n" +
            ChatColor.WHITE + "  /arissweeping config cleanup-animals <true|false> - 被动生物清理\n" +
            ChatColor.WHITE + "  /arissweeping config cleanup-arrows <true|false> - 箭矢清理\n" +
            ChatColor.WHITE + "  /arissweeping config cleanup-falling <true|false> - 掉落物清理\n" +
            ChatColor.YELLOW + "畜牧业管理:\n" +
            ChatColor.WHITE + "  /arissweeping config livestock-check <true|false> - 密度检测\n" +
            ChatColor.WHITE + "  /arissweeping config livestock-limit <数量> - 每区块最大动物数\n" +
            ChatColor.WHITE + "  /arissweeping config warning-time <分钟> - 预警时间\n" +
            ChatColor.YELLOW + "阈值设置:\n" +
            ChatColor.WHITE + "  /arissweeping config item-age <秒> - 掉落物年龄阈值\n" +
            ChatColor.YELLOW + "其他:\n" +
            ChatColor.WHITE + "  /arissweeping config broadcast <true|false> - 清理消息广播\n" +
            ChatColor.WHITE + "  /arissweeping config show-stats <true|false> - 详细统计显示\n" +
            ChatColor.WHITE + "  /arissweeping config list - 查看当前配置\n" +
            ChatColor.WHITE + "  /arissweeping config reload - 重新加载配置"
        );
    }
    
    private void sendConfigList(CommandSender sender) {
        sender.sendMessage(
            ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝的当前配置~ 老师~\n" +
            ChatColor.YELLOW + "基础设置:\n" +
            ChatColor.WHITE + "  插件状态: " + (config.isPluginEnabled() ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭") + "\n" +
            ChatColor.WHITE + "  清理间隔: " + ChatColor.YELLOW + config.getCleanupInterval() + ChatColor.WHITE + " 秒\n" +
            ChatColor.YELLOW + "清理设置:\n" +
            ChatColor.WHITE + "  掉落物: " + (config.isCleanupItems() ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭") + "\n" +
            ChatColor.WHITE + "  敌对生物: " + (config.isCleanupHostileMobs() ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭") + "\n" +
            ChatColor.WHITE + "  被动生物: " + (config.isCleanupPassiveMobs() ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭") + "\n" +
            ChatColor.WHITE + "  箭矢: " + (config.isCleanupArrows() ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭") + "\n" +
            ChatColor.WHITE + "  掉落物: " + (config.isCleanupFallingBlocks() ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭") + "\n" +
            ChatColor.YELLOW + "畜牧业管理:\n" +
            ChatColor.WHITE + "  密度检测: " + (config.isLivestockDensityCheckEnabled() ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭") + "\n" +
            ChatColor.WHITE + "  每区块最大动物数: " + ChatColor.YELLOW + config.getMaxAnimalsPerChunk() + "\n" +
            ChatColor.WHITE + "  预警时间: " + ChatColor.YELLOW + config.getWarningTime() + ChatColor.WHITE + " 分钟\n" +
            ChatColor.YELLOW + "阈值设置:\n" +
            ChatColor.WHITE + "  掉落物年龄阈值: " + ChatColor.YELLOW + config.getItemAgeThreshold() + ChatColor.WHITE + " 秒"
        );
    }
    
    private void handleToggleCommand(CommandSender sender) {
        if (!hasPermission(sender, PermissionManager.CONFIG)) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 老师~没有切换插件状态的权限哦~");
            return;
        }
        boolean currentState = config.isPluginEnabled();
        boolean newState = !currentState;
        
        config.getConfig().set("global.enabled", newState);
        config.saveConfig();
        
        if (newState) {
            sender.sendMessage(
                ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.GREEN + "✅ 爱丽丝清理系统已启动！" + ChatColor.WHITE + "\n" +
                ChatColor.GRAY + "所有清理功能现在都可以正常工作了~"
            );
        } else {
            sender.sendMessage(
                ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.RED + "❌ 爱丽丝清理系统已暂停！" + ChatColor.WHITE + "\n" +
                ChatColor.GRAY + "所有自动清理功能已停止，手动清理仍可使用。"
            );
        }
        
        // 通知所有OP和权限用户
        permissionManager.notifyConfigChange("插件状态", (currentState ? "启用" : "禁用"), (newState ? "启用" : "禁用"));
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(
            ChatColor.GOLD + "═══════════════════════════════════════\n" +
            ChatColor.GOLD + "        " + ChatColor.YELLOW + "✨ 爱丽丝清理系统 ✨" + ChatColor.GOLD + "\n" +
            ChatColor.GOLD + "═══════════════════════════════════════\n" +
            ChatColor.WHITE + "\n" +
            ChatColor.AQUA + "🧹 基础指令:\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping" + ChatColor.GRAY + " - 显示此帮助信息\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping help" + ChatColor.GRAY + " - 显示此帮助信息\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping stats" + ChatColor.GRAY + " - 显示实体统计\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping tps" + ChatColor.GRAY + " - 显示TPS状态\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping toggle" + ChatColor.GRAY + " - 快速切换插件开关\n" +
            ChatColor.WHITE + "\n" +
            ChatColor.LIGHT_PURPLE + "🧽 清理指令:\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping cleanup" + ChatColor.GRAY + " - 执行标准清理\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping cleanup items" + ChatColor.GRAY + " - 清理所有掉落物\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping cleanup mobs" + ChatColor.GRAY + " - 清理敌对生物\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping cleanup all" + ChatColor.GRAY + " - 强制清理所有实体\n" +
            ChatColor.WHITE + "\n" +
            ChatColor.YELLOW + "⚙️ 配置管理:\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config help" + ChatColor.GRAY + " - 配置指令帮助\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config list" + ChatColor.GRAY + " - 查看当前配置\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config enable <true|false>" + ChatColor.GRAY + " - 插件总开关\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config interval <秒>" + ChatColor.GRAY + " - 设置清理间隔\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config item-age <秒>" + ChatColor.GRAY + " - 设置掉落物年龄阈值\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config reload" + ChatColor.GRAY + " - 重新加载配置\n" +
            ChatColor.WHITE + "\n" +
            ChatColor.RED + "🔥 清理功能开关:\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config cleanup-items <true|false>" + ChatColor.GRAY + " - 掉落物清理\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config cleanup-mobs <true|false>" + ChatColor.GRAY + " - 敌对生物清理\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config cleanup-animals <true|false>" + ChatColor.GRAY + " - 被动生物清理\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config cleanup-arrows <true|false>" + ChatColor.GRAY + " - 箭矢清理\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config cleanup-falling <true|false>" + ChatColor.GRAY + " - 掉落方块清理\n" +
            ChatColor.WHITE + "\n" +
            ChatColor.GOLD + "🐄 畜牧业管理:\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping livestock-stats" + ChatColor.GRAY + " - 查看世界畜牧业统计\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config livestock-check <true|false>" + ChatColor.GRAY + " - 密度检测\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config livestock-limit <数量>" + ChatColor.GRAY + " - 每区块动物限制\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config warning-time <分钟>" + ChatColor.GRAY + " - 预警时间\n" +
            ChatColor.WHITE + "\n" +
            ChatColor.AQUA + "📢 消息设置:\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config broadcast <true|false>" + ChatColor.GRAY + " - 清理消息广播\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config show-stats <true|false>" + ChatColor.GRAY + " - 详细统计显示\n" +
            ChatColor.WHITE + "\n" +
            (hasPermission(sender, PermissionManager.ADMIN) ? 
                ChatColor.DARK_PURPLE + "🔐 权限管理 (仅管理员):\n" +
                ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping permission give <玩家> <权限>" + ChatColor.GRAY + " - 授予权限\n" +
                ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping permission remove <玩家> <权限>" + ChatColor.GRAY + " - 移除权限\n" +
                ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping permission list [玩家]" + ChatColor.GRAY + " - 查看权限\n" +
                ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping permission reload" + ChatColor.GRAY + " - 重载权限\n" +
                ChatColor.WHITE + "\n" +
                ChatColor.DARK_RED + "🔧 测试工具 (仅管理员):\n" +
                ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping test timer" + ChatColor.GRAY + " - 测试定时清理\n" +
                ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping test status" + ChatColor.GRAY + " - 查看定时器状态\n" +
                ChatColor.WHITE + "\n" : "") +
            ChatColor.GRAY + "💡 提示: 可用权限包括 admin, cleanup, stats, config\n" +
            ChatColor.GRAY + "💡 使用 /arissweeping config help 查看详细配置说明\n" +
            ChatColor.WHITE + "\n" +
            ChatColor.GOLD + "═══════════════════════════════════════\n" +
            ChatColor.GRAY + "        由爱丽丝为老师~提供清理服务 💖\n" +
            ChatColor.GOLD + "═══════════════════════════════════════"
        );
    }
    
    private CleanupStats performStandardCleanup() {
        CleanupStats stats = new CleanupStats();
        List<Entity> toRemove = new ArrayList<>();
        
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (shouldCleanupEntity(entity)) {
                    toRemove.add(entity);
                    stats.incrementType(entity);
                }
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
        
        return stats;
    }
    
    private CleanupStats performForceCleanup() {
        CleanupStats stats = new CleanupStats();
        List<Entity> toRemove = new ArrayList<>();
        
        try {
            plugin.getLogger().info("开始执行强制清理扫描...");
            
            for (World world : Bukkit.getWorlds()) {
                try {
                    int worldEntityCount = 0;
                    for (Entity entity : world.getEntities()) {
                        if (shouldForceCleanupEntity(entity)) {
                            toRemove.add(entity);
                            stats.incrementType(entity);
                            worldEntityCount++;
                        }
                    }
                    if (worldEntityCount > 0) {
                        plugin.getLogger().info("世界 " + world.getName() + " 中找到 " + worldEntityCount + " 个待清理实体");
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("扫描世界 " + world.getName() + " 时发生错误: " + e.getMessage());
                }
            }
            
            plugin.getLogger().info("强制清理扫描完成，共找到 " + toRemove.size() + " 个实体待清理");
            
            // 在主线程中移除实体
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        int removed = 0;
                        for (Entity entity : toRemove) {
                            try {
                                if (entity.isValid()) {
                                    entity.remove();
                                    removed++;
                                }
                            } catch (Exception e) {
                                plugin.getLogger().warning("移除实体时发生错误: " + e.getMessage());
                            }
                        }
                        plugin.getLogger().info("强制清理完成，实际移除了 " + removed + " 个实体");
                    } catch (Exception e) {
                        plugin.getLogger().severe("强制清理实体移除过程中发生严重错误: " + e.getMessage());
                    }
                }
            }.runTask(plugin);
        } catch (Exception e) {
            plugin.getLogger().severe("强制清理过程中发生严重错误: " + e.getMessage());
        }
        
        return stats;
    }
    
    private boolean shouldForceCleanupEntity(Entity entity) {
        try {
            if (entity == null || !entity.isValid()) {
                return false;
            }
            
            if (entity instanceof Player) {
                return false;
            }
            
            // 强制清理模式：忽略年龄限制，只检查类型配置
            if (entity instanceof Item) {
                try {
                    Item item = (Item) entity;
                    // 保护有自定义名称的物品
                    if (item.getCustomName() != null) {
                        return false;
                    }
                    // 保护重要物品（如果有特殊标记）
                    if (item.getItemStack().hasItemMeta() && 
                        item.getItemStack().getItemMeta().hasDisplayName()) {
                        return false;
                    }
                    return config.isCleanupItems();
                } catch (Exception e) {
                    plugin.getLogger().warning("检查物品实体时发生错误: " + e.getMessage());
                    return false;
                }
            }
            
            if (entity instanceof ExperienceOrb) {
                return config.isCleanupExperienceOrbs();
            }
            
            if (entity instanceof Arrow) {
                try {
                    Arrow arrow = (Arrow) entity;
                    // 保护玩家射出的箭（如果配置要求）
                    if (arrow.getShooter() instanceof Player) {
                        // 可以根据配置决定是否清理玩家箭矢
                        return config.isCleanupArrows();
                    }
                    return config.isCleanupArrows();
                } catch (Exception e) {
                    plugin.getLogger().warning("检查箭矢实体时发生错误: " + e.getMessage());
                    return false;
                }
            }
            
            if (entity instanceof org.bukkit.entity.FallingBlock) {
                return config.isCleanupFallingBlocks();
            }
            
            // 排除特殊实体
            if (entity instanceof org.bukkit.entity.Minecart ||
                entity instanceof org.bukkit.entity.Boat ||
                entity instanceof org.bukkit.entity.ArmorStand ||
                entity instanceof org.bukkit.entity.ItemFrame ||
                entity instanceof org.bukkit.entity.Painting ||
                entity instanceof org.bukkit.entity.LeashHitch ||
                entity instanceof org.bukkit.entity.EnderCrystal) {
                return false;
            }
            
            // 敌对生物清理（保护有名字的怪物）
            if (entity instanceof Monster) {
                try {
                    Monster monster = (Monster) entity;
                    // 保护有自定义名称的怪物
                    if (monster.getCustomName() != null) {
                        return false;
                    }
                    // 保护被拴住的怪物
                    if (monster.isLeashed()) {
                        return false;
                    }
                    // 保护持有物品的怪物
                    if (monster.getEquipment() != null && 
                        (monster.getEquipment().getItemInMainHand().getType() != org.bukkit.Material.AIR ||
                         monster.getEquipment().getItemInOffHand().getType() != org.bukkit.Material.AIR)) {
                        return false;
                    }
                    return config.isCleanupHostileMobs();
                } catch (Exception e) {
                    plugin.getLogger().warning("检查敌对生物实体时发生错误: " + e.getMessage());
                    return false;
                }
            }
            
            // 被动生物不清理
            if (entity instanceof Animals) {
                return false;
            }
            
            // 保护村民和其他NPC
            if (entity instanceof org.bukkit.entity.Villager ||
                entity instanceof org.bukkit.entity.WanderingTrader) {
                return false;
            }
            
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("检查实体清理条件时发生错误: " + e.getMessage());
            return false;
        }
    }
    
    private boolean shouldCleanupEntity(Entity entity) {
        if (entity instanceof Player) return false;
        
        if (entity instanceof Item item) {
            if (!config.isCleanupItems()) return false;
            int ageThreshold = config.getItemAgeThreshold() * 20; // 转换为tick
            return item.getTicksLived() > ageThreshold;
        }
        
        if (entity instanceof ExperienceOrb) {
            return config.isCleanupExperienceOrbs();
        }
        
        if (entity instanceof Arrow) {
            return config.isCleanupArrows();
        }
        
        if (entity instanceof Monster) {
            return config.isCleanupHostileMobs() && entity.getCustomName() == null;
        }
        
        if (entity instanceof Animals) {
            return config.isCleanupPassiveMobs() && entity.getCustomName() == null;
        }
        
        return false;
    }
    
    private boolean shouldForceCleanup(Entity entity) {
        return !(entity instanceof Player) && entity.getCustomName() == null;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!hasAnyPermission(sender)) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            List<String> commands = new ArrayList<>();
            if (hasPermission(sender, PermissionManager.CLEANUP)) commands.add("cleanup");
            if (hasPermission(sender, PermissionManager.STATS)) {
                commands.add("stats");
                commands.add("tps");
                commands.add("livestock-stats");
                commands.add("livestock");
            }
            if (hasPermission(sender, PermissionManager.CONFIG)) {
                commands.add("config");
                commands.add("toggle");
            }
            if (hasPermission(sender, PermissionManager.ADMIN)) {
                commands.add("permission");
                commands.add("perm");
                commands.add("test");
            }
            commands.add("help");
            
            return commands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("cleanup") && hasPermission(sender, PermissionManager.CLEANUP)) {
                return Arrays.asList("items", "mobs", "all")
                        .stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("config") && hasPermission(sender, PermissionManager.CONFIG)) {
                return Arrays.asList("interval", "enable", "cleanup-items", "cleanup-mobs", 
                        "cleanup-animals", "cleanup-arrows", "cleanup-falling", "livestock-check", 
                        "livestock-limit", "warning-time", "broadcast", "show-stats", "item-age", "reload", "list")
                        .stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if ((args[0].equalsIgnoreCase("permission") || args[0].equalsIgnoreCase("perm")) && hasPermission(sender, PermissionManager.ADMIN)) {
                return Arrays.asList("give", "remove", "list", "reload")
                        .stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("test") && hasPermission(sender, PermissionManager.ADMIN)) {
                return Arrays.asList("timer", "status")
                        .stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        // 为permission的子命令提供补全
        if (args.length == 3 && (args[0].equalsIgnoreCase("permission") || args[0].equalsIgnoreCase("perm")) && hasPermission(sender, PermissionManager.ADMIN)) {
            String subCommand = args[1].toLowerCase();
            if (subCommand.equals("give") || subCommand.equals("remove") || subCommand.equals("list")) {
                // 补全在线玩家名
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        // 为permission give/remove提供权限节点补全
        if (args.length == 4 && (args[0].equalsIgnoreCase("permission") || args[0].equalsIgnoreCase("perm")) && hasPermission(sender, PermissionManager.ADMIN)) {
            String subCommand = args[1].toLowerCase();
            if (subCommand.equals("give") || subCommand.equals("remove")) {
                return Arrays.asList("arissweeping.admin", "arissweeping.cleanup", "arissweeping.stats", "arissweeping.config")
                        .stream()
                        .filter(perm -> perm.toLowerCase().startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        // 为config的子命令提供值补全
        if (args.length == 3 && args[0].equalsIgnoreCase("config")) {
            String subCommand = args[1].toLowerCase();
            if (subCommand.equals("enable") || subCommand.equals("cleanup-items") || 
                subCommand.equals("cleanup-mobs") || subCommand.equals("cleanup-animals") ||
                subCommand.equals("cleanup-arrows") || subCommand.equals("cleanup-falling") ||
                subCommand.equals("livestock-check") || subCommand.equals("broadcast") ||
                subCommand.equals("show-stats")) {
                return Arrays.asList("true", "false")
                        .stream()
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (subCommand.equals("interval")) {
                return Arrays.asList("60", "120", "300", "600")
                        .stream()
                        .filter(s -> s.startsWith(args[2]))
                        .collect(Collectors.toList());
            } else if (subCommand.equals("livestock-limit")) {
                return Arrays.asList("10", "15", "20", "25", "30")
                        .stream()
                        .filter(s -> s.startsWith(args[2]))
                        .collect(Collectors.toList());
            } else if (subCommand.equals("warning-time")) {
                return Arrays.asList("1", "3", "5", "10")
                        .stream()
                        .filter(s -> s.startsWith(args[2]))
                        .collect(Collectors.toList());
            } else if (subCommand.equals("item-age")) {
                return Arrays.asList("30", "60", "90", "120", "180", "300")
                        .stream()
                        .filter(s -> s.startsWith(args[2]))
                        .collect(Collectors.toList());
            }
        }
        
        return new ArrayList<>();
    }
    
    // 权限检查方法
    private boolean hasAnyPermission(CommandSender sender) {
        if (sender.isOp()) {
            return true;
        }
        
        if (!(sender instanceof Player)) {
            return true; // 控制台始终有权限
        }
        
        Player player = (Player) sender;
        return permissionManager.hasPermission(player.getName(), PermissionManager.ADMIN) ||
               permissionManager.hasPermission(player.getName(), PermissionManager.CLEANUP) ||
               permissionManager.hasPermission(player.getName(), PermissionManager.STATS) ||
               permissionManager.hasPermission(player.getName(), PermissionManager.CONFIG);
    }
    
    private boolean hasPermission(CommandSender sender, String permission) {
        if (sender.isOp()) {
            return true;
        }
        
        if (!(sender instanceof Player)) {
            return true; // 控制台始终有权限
        }
        
        Player player = (Player) sender;
        return permissionManager.hasPermission(player.getName(), permission);
    }
    
    // 权限管理指令处理
    private void handlePermissionCommand(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PermissionManager.ADMIN)) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 只有管理员才能管理权限哦~");
            return;
        }
        
        if (args.length < 2) {
            sendPermissionHelp(sender);
            return;
        }
        
        switch (args[1].toLowerCase()) {
            case "give":
            case "add":
                handleGivePermission(sender, args);
                break;
            case "remove":
            case "take":
                handleRemovePermission(sender, args);
                break;
            case "list":
                handleListPermissions(sender, args);
                break;
            case "reload":
                handleReloadPermissions(sender);
                break;
            default:
                sendPermissionHelp(sender);
                break;
        }
    }
    
    private void handleGivePermission(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 用法: /arissweeping permission give <玩家名> <权限>");
            sender.sendMessage(ChatColor.YELLOW + "可用权限: admin, cleanup, stats, config");
            return;
        }
        
        String playerName = args[2];
        String permission = "arissweeping." + args[3].toLowerCase();
        
        if (!PermissionManager.VALID_PERMISSIONS.contains(permission)) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 无效的权限节点: " + args[3]);
            sender.sendMessage(ChatColor.YELLOW + "可用权限: admin, cleanup, stats, config");
            return;
        }
        
        if (permissionManager.givePermission(playerName, permission)) {
            sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 成功给予玩家 " + ChatColor.YELLOW + playerName + 
                             ChatColor.GREEN + " 权限: " + ChatColor.AQUA + args[3]);
            
            // 通知目标玩家
            Player targetPlayer = Bukkit.getPlayer(playerName);
            if (targetPlayer != null) {
                targetPlayer.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 老师~获得了新权限: " + ChatColor.AQUA + args[3]);
            }
        } else {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 给予权限失败！");
        }
    }
    
    private void handleRemovePermission(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 用法: /arissweeping permission remove <玩家名> <权限>");
            sender.sendMessage(ChatColor.YELLOW + "可用权限: admin, cleanup, stats, config");
            return;
        }
        
        String playerName = args[2];
        String permission = "arissweeping." + args[3].toLowerCase();
        
        if (!PermissionManager.VALID_PERMISSIONS.contains(permission)) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 无效的权限节点: " + args[3]);
            sender.sendMessage(ChatColor.YELLOW + "可用权限: admin, cleanup, stats, config");
            return;
        }
        
        if (permissionManager.removePermission(playerName, permission)) {
            sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 成功移除玩家 " + ChatColor.YELLOW + playerName + 
                             ChatColor.GREEN + " 的权限: " + ChatColor.AQUA + args[3]);
            
            // 通知目标玩家
            Player targetPlayer = Bukkit.getPlayer(playerName);
            if (targetPlayer != null) {
                targetPlayer.sendMessage(ChatColor.RED + "[邦邦卡邦！] 老师~失去了权限: " + ChatColor.AQUA + args[3]);
            }
        } else {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 移除权限失败！该玩家可能没有此权限。");
        }
    }
    
    private void handleListPermissions(CommandSender sender, String[] args) {
        if (args.length >= 3) {
            // 查看特定玩家的权限
            String playerName = args[2];
            Set<String> permissions = permissionManager.getPlayerPermissions(playerName);
            
            sender.sendMessage(ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "玩家 " + 
                             ChatColor.YELLOW + playerName + ChatColor.WHITE + " 的权限:");
            
            if (permissions.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "  无任何权限");
            } else {
                for (String perm : permissions) {
                    String shortPerm = perm.replace("arissweeping.", "");
                    sender.sendMessage(ChatColor.GREEN + "  - " + ChatColor.AQUA + shortPerm);
                }
            }
        } else {
            // 列出所有有权限的玩家
            Map<String, Set<String>> allPermissions = permissionManager.getAllPlayerPermissions();
            
            sender.sendMessage(ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "所有权限用户:");
            
            if (allPermissions.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "  暂无权限用户");
            } else {
                for (Map.Entry<String, Set<String>> entry : allPermissions.entrySet()) {
                    String playerName = entry.getKey();
                    Set<String> permissions = entry.getValue();
                    
                    sender.sendMessage(ChatColor.YELLOW + "  " + playerName + ChatColor.WHITE + ": " + 
                                     ChatColor.AQUA + permissions.stream()
                                             .map(p -> p.replace("arissweeping.", ""))
                                             .collect(Collectors.joining(", ")));
                }
            }
        }
    }
    
    private void handleReloadPermissions(CommandSender sender) {
        permissionManager.reloadPermissions();
        sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 权限配置已重新加载！");
    }
    
    private void sendPermissionHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "权限管理帮助:");
        sender.sendMessage(ChatColor.YELLOW + "/arissweeping permission give <玩家名> <权限>" + ChatColor.WHITE + " - 给予权限");
        sender.sendMessage(ChatColor.YELLOW + "/arissweeping permission remove <玩家名> <权限>" + ChatColor.WHITE + " - 移除权限");
        sender.sendMessage(ChatColor.YELLOW + "/arissweeping permission list [玩家名]" + ChatColor.WHITE + " - 查看权限");
        sender.sendMessage(ChatColor.YELLOW + "/arissweeping permission reload" + ChatColor.WHITE + " - 重载权限");
        sender.sendMessage(ChatColor.GRAY + "可用权限: admin, cleanup, stats, config");
    }
    
    private void handleTestCommand(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PermissionManager.ADMIN)) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 老师~没有测试权限哦~");
            return;
        }
        
        if (args.length == 1) {
            sender.sendMessage(ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "测试命令帮助:");
            sender.sendMessage(ChatColor.YELLOW + "/arissweeping test timer" + ChatColor.WHITE + " - 测试定时清理");
            sender.sendMessage(ChatColor.YELLOW + "/arissweeping test status" + ChatColor.WHITE + " - 查看定时器状态");
            return;
        }
        
        switch (args[1].toLowerCase()) {
            case "timer":
                testTimer(sender);
                break;
            case "status":
                showTimerStatus(sender);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 未知的测试命令: " + args[1]);
                break;
        }
    }
    
    private void testTimer(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 正在测试定时清理...");
        
        // 重启清理任务以应用新配置
        plugin.getCleanupHandler().restartCleanupTask();
        
        sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 定时清理任务已重启，请查看控制台日志!");
    }
    
    private void showTimerStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "定时器状态:");
        sender.sendMessage(ChatColor.YELLOW + "插件启用: " + ChatColor.WHITE + config.isPluginEnabled());
        sender.sendMessage(ChatColor.YELLOW + "清理间隔: " + ChatColor.WHITE + config.getCleanupInterval() + " 秒");
        sender.sendMessage(ChatColor.YELLOW + "广播清理: " + ChatColor.WHITE + config.isBroadcastCleanup());
        sender.sendMessage(ChatColor.YELLOW + "异步清理: " + ChatColor.WHITE + config.isAsyncCleanup());
        sender.sendMessage(ChatColor.YELLOW + "TPS监控: " + ChatColor.WHITE + config.isTpsMonitorEnabled());
        
        if (plugin.getCleanupHandler().getTpsMonitor() != null) {
             double currentTps = plugin.getCleanupHandler().getTpsMonitor().getCurrentTps();
             sender.sendMessage(ChatColor.YELLOW + "当前TPS: " + ChatColor.WHITE + String.format("%.2f", currentTps));
         }
     }
     
     private void handleLivestockStatsCommand(CommandSender sender) {
         // 检查权限
         if (!hasPermission(sender, PermissionManager.STATS)) {
             sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 老师~没有权限查看畜牧业统计哦~");
             return;
         }
         
         // 异步执行统计，避免阻塞主线程
         new BukkitRunnable() {
             @Override
             public void run() {
                 final Map<String, Integer> worldStats = new HashMap<>();
                final Map<String, Map<String, Integer>> worldTypeStats = new HashMap<>();
                final int[] totalAnimals = {0};
                final int[] totalChunks = {0};
                final int[] violationChunks = {0};
                 
                 try {
                     for (World world : Bukkit.getWorlds()) {
                         if (world == null) continue;
                         
                         int worldAnimalCount = 0;
                         int worldChunkCount = 0;
                         int worldViolationCount = 0;
                         Map<String, Integer> typeCount = new HashMap<>();
                         
                         for (Chunk chunk : world.getLoadedChunks()) {
                             if (chunk == null) continue;
                             
                             worldChunkCount++;
                             int chunkAnimalCount = 0;
                             
                             for (Entity entity : chunk.getEntities()) {
                                 if (entity instanceof Animals) {
                                     chunkAnimalCount++;
                                     worldAnimalCount++;
                                     String type = entity.getType().name();
                                     typeCount.put(type, typeCount.getOrDefault(type, 0) + 1);
                                 }
                             }
                             
                             // 检查是否超标
                             if (chunkAnimalCount > config.getMaxAnimalsPerChunk()) {
                                 worldViolationCount++;
                             }
                         }
                         
                         worldStats.put(world.getName(), worldAnimalCount);
                         worldTypeStats.put(world.getName(), typeCount);
                         totalAnimals[0] += worldAnimalCount;
                         totalChunks[0] += worldChunkCount;
                         violationChunks[0] += worldViolationCount;
                     }
                     
                     // 回到主线程发送消息
                     new BukkitRunnable() {
                         @Override
                         public void run() {
                             StringBuilder message = new StringBuilder();
                             message.append(ChatColor.GOLD).append("═══════════════════════════════════════\n");
                             message.append(ChatColor.GOLD).append("        ").append(ChatColor.YELLOW).append("🐄 世界畜牧业统计 🐄").append(ChatColor.GOLD).append("\n");
                             message.append(ChatColor.GOLD).append("═══════════════════════════════════════\n");
                             message.append(ChatColor.WHITE).append("\n");
                             
                             // 总体统计
                             message.append(ChatColor.AQUA).append("📊 总体统计:\n");
                             message.append(ChatColor.WHITE).append("  总动物数量: ").append(ChatColor.YELLOW).append(totalAnimals[0]).append("\n");
                             message.append(ChatColor.WHITE).append("  已加载区块: ").append(ChatColor.YELLOW).append(totalChunks[0]).append("\n");
                             message.append(ChatColor.WHITE).append("  超标区块: ").append(violationChunks[0] > 0 ? ChatColor.RED : ChatColor.GREEN).append(violationChunks[0]).append("\n");
                             message.append(ChatColor.WHITE).append("  密度阈值: ").append(ChatColor.YELLOW).append(config.getMaxAnimalsPerChunk()).append(" 只/区块\n");
                             message.append(ChatColor.WHITE).append("\n");
                             
                             // 各世界统计
                             message.append(ChatColor.LIGHT_PURPLE).append("🌍 各世界详情:\n");
                             for (Map.Entry<String, Integer> entry : worldStats.entrySet()) {
                                 String worldName = entry.getKey();
                                 int animalCount = entry.getValue();
                                 Map<String, Integer> types = worldTypeStats.get(worldName);
                                 
                                 message.append(ChatColor.WHITE).append("  ").append(ChatColor.GREEN).append(worldName).append(ChatColor.WHITE).append(": ").append(ChatColor.YELLOW).append(animalCount).append(" 只\n");
                                 
                                 if (config.isDebugMode() && types != null && !types.isEmpty()) {
                                     message.append(ChatColor.GRAY).append("    类型分布: ");
                                     for (Map.Entry<String, Integer> typeEntry : types.entrySet()) {
                                         message.append(typeEntry.getKey()).append("(").append(typeEntry.getValue()).append(") ");
                                     }
                                     message.append("\n");
                                 }
                             }
                             
                             message.append(ChatColor.WHITE).append("\n");
                             message.append(ChatColor.GRAY).append("💡 提示: 使用 /arissweeping config livestock-limit <数量> 调整密度阈值\n");
                             message.append(ChatColor.GRAY).append("💡 提示: 使用 /arissweeping config livestock-check true 启用自动管理\n");
                             message.append(ChatColor.GOLD).append("═══════════════════════════════════════");
                             
                             sender.sendMessage(message.toString());
                         }
                     }.runTask(plugin);
                     
                 } catch (Exception e) {
                     plugin.getLogger().severe("[CleanupCommand] 获取畜牧业统计时发生异常: " + e.getMessage());
                     new BukkitRunnable() {
                         @Override
                         public void run() {
                             sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 获取畜牧业统计时发生错误，请查看控制台日志");
                         }
                     }.runTask(plugin);
                 }
             }
         }.runTaskAsynchronously(plugin);
     }
}