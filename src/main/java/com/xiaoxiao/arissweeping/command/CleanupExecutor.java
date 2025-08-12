package com.xiaoxiao.arissweeping.command;

import com.xiaoxiao.arissweeping.ArisSweeping;
import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.CleanupStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 清理执行器 - 负责执行各种清理任务
 */
public class CleanupExecutor {
    private final ArisSweeping plugin;
    private final ModConfig config;
    private volatile boolean isCleanupRunning = false;
    private volatile long lastCleanupTime = 0;
    private static final long CLEANUP_COOLDOWN = 5000; // 5秒冷却时间

    public CleanupExecutor(ArisSweeping plugin) {
        this.plugin = plugin;
        this.config = plugin.getModConfig();
    }
    
    /**
     * 处理清理命令
     */
    public void handleCleanupCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "[邦邦卡邦！] 用法: /cleanup cleanup <now|items|mobs|all>");
            return;
        }
        
        String cleanupType = args[1].toLowerCase();
        switch (cleanupType) {
            case "now":
            case "all":
                executeStandardCleanup(sender);
                break;
            case "items":
                executeItemCleanup(sender);
                break;
            case "mobs":
                executeMobCleanup(sender);
                break;
            default:
                sender.sendMessage(ChatColor.YELLOW + "[邦邦卡邦！] 用法: /cleanup cleanup <now|items|mobs|all>");
                break;
        }
    }

    /**
     * 执行物品清理
     */
    public void executeItemCleanup(CommandSender sender) {
        if (isCleanupRunning) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 清理任务正在进行中，请稍候...");
            return;
        }
        
        isCleanupRunning = true;
        sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 开始清理掉落物品...");
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    CleanupStats stats = new CleanupStats();
                    for (World world : Bukkit.getWorlds()) {
                        for (Entity entity : world.getEntities()) {
                            if (entity instanceof Item && shouldCleanupEntity(entity)) {
                                entity.remove();
                                stats.incrementItems();
                            }
                        }
                    }
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 物品清理完成！清理了 " + stats.getItemsCleaned() + " 个掉落物品");
                        isCleanupRunning = false;
                    });
                } catch (Exception e) {
                    plugin.getLogger().severe("Item cleanup failed: " + e.getMessage());
                    e.printStackTrace();
                    isCleanupRunning = false;
                }
            }
        }.runTaskAsynchronously(plugin);
    }
    
    /**
     * 执行生物清理
     */
    public void executeMobCleanup(CommandSender sender) {
        if (isCleanupRunning) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 清理任务正在进行中，请稍候...");
            return;
        }
        
        isCleanupRunning = true;
        sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 开始清理生物...");
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    CleanupStats stats = new CleanupStats();
                    for (World world : Bukkit.getWorlds()) {
                        for (Entity entity : world.getEntities()) {
                            if ((entity instanceof Monster || entity instanceof Animals) && shouldCleanupEntity(entity)) {
                                entity.remove();
                                stats.incrementMobs();
                            }
                        }
                    }
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 生物清理完成！清理了 " + stats.getMobsCleaned() + " 个生物");
                        isCleanupRunning = false;
                    });
                } catch (Exception e) {
                    plugin.getLogger().severe("Mob cleanup failed: " + e.getMessage());
                    e.printStackTrace();
                    isCleanupRunning = false;
                }
            }
        }.runTaskAsynchronously(plugin);
    }
    
    /**
     * 执行标准清理
     */
    public void executeStandardCleanup(CommandSender sender) {
        // 检查冷却时间
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime < CLEANUP_COOLDOWN) {
            long remainingTime = (CLEANUP_COOLDOWN - (currentTime - lastCleanupTime)) / 1000;
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 清理冷却中，请等待 " + remainingTime + " 秒后再试！");
            return;
        }

        if (isCleanupRunning) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 清理任务正在进行中，请稍候...");
            return;
        }

        isCleanupRunning = true;
        lastCleanupTime = currentTime;

        if (config.isAsyncCleanup()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        CleanupStats stats = performStandardCleanup();
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (config.isBroadcastCleanup()) {
                                    Bukkit.broadcastMessage(ChatColor.GREEN + "[邦邦卡邦！] 清理完成！清理了 " + 
                                        ChatColor.YELLOW + stats.getItemsRemoved() + ChatColor.GREEN + " 个物品，" +
                                        ChatColor.YELLOW + stats.getExperienceOrbsRemoved() + ChatColor.GREEN + " 个经验球，" +
                                        ChatColor.YELLOW + stats.getArrowsRemoved() + ChatColor.GREEN + " 支箭矢！");
                                } else {
                                    sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 清理完成！清理了 " + 
                                        ChatColor.YELLOW + stats.getItemsRemoved() + ChatColor.GREEN + " 个物品，" +
                                        ChatColor.YELLOW + stats.getExperienceOrbsRemoved() + ChatColor.GREEN + " 个经验球，" +
                                        ChatColor.YELLOW + stats.getArrowsRemoved() + ChatColor.GREEN + " 支箭矢！");
                                }
                                isCleanupRunning = false;
                            }
                        }.runTask(plugin);
                    } catch (Exception e) {
                        plugin.getLogger().severe("[CleanupExecutor] 异步清理时发生异常: " + e.getMessage());
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 清理时发生错误，请查看控制台日志");
                                isCleanupRunning = false;
                            }
                        }.runTask(plugin);
                    }
                }
            }.runTaskAsynchronously(plugin);
        } else {
            try {
                CleanupStats stats = performStandardCleanup();
                if (config.isBroadcastCleanup()) {
                    Bukkit.broadcastMessage(ChatColor.GREEN + "[邦邦卡邦！] 清理完成！清理了 " + 
                        ChatColor.YELLOW + stats.getItemsRemoved() + ChatColor.GREEN + " 个物品，" +
                        ChatColor.YELLOW + stats.getExperienceOrbsRemoved() + ChatColor.GREEN + " 个经验球，" +
                        ChatColor.YELLOW + stats.getArrowsRemoved() + ChatColor.GREEN + " 支箭矢！");
                } else {
                    sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 清理完成！清理了 " + 
                        ChatColor.YELLOW + stats.getItemsRemoved() + ChatColor.GREEN + " 个物品，" +
                        ChatColor.YELLOW + stats.getExperienceOrbsRemoved() + ChatColor.GREEN + " 个经验球，" +
                        ChatColor.YELLOW + stats.getArrowsRemoved() + ChatColor.GREEN + " 支箭矢！");
                }
                isCleanupRunning = false;
            } catch (Exception e) {
                plugin.getLogger().severe("[CleanupExecutor] 同步清理时发生异常: " + e.getMessage());
                sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 清理时发生错误，请查看控制台日志");
                isCleanupRunning = false;
            }
        }
    }

    /**
     * 执行物品清理
     */
    public void executeCleanupItems(CommandSender sender) {
        if (isCleanupRunning) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 清理任务正在进行中，请稍候...");
            return;
        }

        isCleanupRunning = true;
        lastCleanupTime = System.currentTimeMillis();

        if (config.isAsyncCleanup()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    executeItemsCleanupTask(sender);
                }
            }.runTaskAsynchronously(plugin);
        } else {
            executeItemsCleanupTask(sender);
        }
    }

    private void executeItemsCleanupTask(CommandSender sender) {
        try {
            int itemsRemoved = 0;
            
            for (World world : Bukkit.getWorlds()) {
                if (world == null) continue;
                
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Item && shouldCleanupEntity(entity)) {
                        entity.remove();
                        itemsRemoved++;
                    }
                }
            }
            
            final int finalItemsRemoved = itemsRemoved;
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (config.isBroadcastCleanup()) {
                        Bukkit.broadcastMessage(ChatColor.GREEN + "[邦邦卡邦！] 物品清理完成！清理了 " + 
                            ChatColor.YELLOW + finalItemsRemoved + ChatColor.GREEN + " 个物品！");
                    } else {
                        sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 物品清理完成！清理了 " + 
                            ChatColor.YELLOW + finalItemsRemoved + ChatColor.GREEN + " 个物品！");
                    }
                    isCleanupRunning = false;
                }
            }.runTask(plugin);
            
        } catch (Exception e) {
            plugin.getLogger().severe("[CleanupExecutor] 物品清理时发生异常: " + e.getMessage());
            new BukkitRunnable() {
                @Override
                public void run() {
                    sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 物品清理时发生错误，请查看控制台日志");
                    isCleanupRunning = false;
                }
            }.runTask(plugin);
        }
    }

    /**
     * 执行生物清理
     */
    public void executeCleanupMobs(CommandSender sender) {
        if (isCleanupRunning) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 清理任务正在进行中，请稍候...");
            return;
        }

        isCleanupRunning = true;
        lastCleanupTime = System.currentTimeMillis();

        if (config.isAsyncCleanup()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    executeMobsCleanupTask(sender);
                }
            }.runTaskAsynchronously(plugin);
        } else {
            executeMobsCleanupTask(sender);
        }
    }

    private void executeMobsCleanupTask(CommandSender sender) {
        try {
            int mobsRemoved = 0;
            
            for (World world : Bukkit.getWorlds()) {
                if (world == null) continue;
                
                for (Entity entity : world.getEntities()) {
                    if ((entity instanceof Monster || entity instanceof Animals) && shouldCleanupEntity(entity)) {
                        entity.remove();
                        mobsRemoved++;
                    }
                }
            }
            
            final int finalMobsRemoved = mobsRemoved;
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (config.isBroadcastCleanup()) {
                        Bukkit.broadcastMessage(ChatColor.GREEN + "[邦邦卡邦！] 生物清理完成！清理了 " + 
                            ChatColor.YELLOW + finalMobsRemoved + ChatColor.GREEN + " 个生物！");
                    } else {
                        sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 生物清理完成！清理了 " + 
                            ChatColor.YELLOW + finalMobsRemoved + ChatColor.GREEN + " 个生物！");
                    }
                    isCleanupRunning = false;
                }
            }.runTask(plugin);
            
        } catch (Exception e) {
            plugin.getLogger().severe("[CleanupExecutor] 生物清理时发生异常: " + e.getMessage());
            new BukkitRunnable() {
                @Override
                public void run() {
                    sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 生物清理时发生错误，请查看控制台日志");
                    isCleanupRunning = false;
                }
            }.runTask(plugin);
        }
    }

    /**
     * 执行全部清理
     */
    public void executeCleanupAll(CommandSender sender) {
        if (isCleanupRunning) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 清理任务正在进行中，请稍候...");
            return;
        }

        isCleanupRunning = true;
        lastCleanupTime = System.currentTimeMillis();

        sender.sendMessage(ChatColor.YELLOW + "[邦邦卡邦！] 正在执行强制清理，这可能会影响服务器性能...");

        if (config.isAsyncCleanup()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    executeAllCleanupTask(sender);
                }
            }.runTaskAsynchronously(plugin);
        } else {
            executeAllCleanupTask(sender);
        }
    }

    private void executeAllCleanupTask(CommandSender sender) {
        try {
            CleanupStats stats = performForceCleanup();
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (config.isBroadcastCleanup()) {
                        Bukkit.broadcastMessage(ChatColor.GREEN + "[邦邦卡邦！] 强制清理完成！清理了 " + 
                            ChatColor.YELLOW + stats.getItemsRemoved() + ChatColor.GREEN + " 个物品，" +
                            ChatColor.YELLOW + stats.getExperienceOrbsRemoved() + ChatColor.GREEN + " 个经验球，" +
                            ChatColor.YELLOW + stats.getArrowsRemoved() + ChatColor.GREEN + " 支箭矢，" +
                            ChatColor.YELLOW + stats.getMobsRemoved() + ChatColor.GREEN + " 个生物！");
                    } else {
                        sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 强制清理完成！清理了 " + 
                            ChatColor.YELLOW + stats.getItemsRemoved() + ChatColor.GREEN + " 个物品，" +
                            ChatColor.YELLOW + stats.getExperienceOrbsRemoved() + ChatColor.GREEN + " 个经验球，" +
                            ChatColor.YELLOW + stats.getArrowsRemoved() + ChatColor.GREEN + " 支箭矢，" +
                            ChatColor.YELLOW + stats.getMobsRemoved() + ChatColor.GREEN + " 个生物！");
                    }
                    isCleanupRunning = false;
                }
            }.runTask(plugin);
            
        } catch (Exception e) {
            plugin.getLogger().severe("[CleanupExecutor] 强制清理时发生异常: " + e.getMessage());
            new BukkitRunnable() {
                @Override
                public void run() {
                    sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 强制清理时发生错误，请查看控制台日志");
                    isCleanupRunning = false;
                }
            }.runTask(plugin);
        }
    }

    /**
     * 执行标准清理逻辑
     */
    private CleanupStats performStandardCleanup() {
        CleanupStats stats = new CleanupStats();
        
        for (World world : Bukkit.getWorlds()) {
            if (world == null) continue;
            
            for (Chunk chunk : world.getLoadedChunks()) {
                if (chunk == null) continue;
                
                for (Entity entity : chunk.getEntities()) {
                    if (shouldCleanupEntity(entity)) {
                        if (entity instanceof Item) {
                            stats.incrementItemsRemoved();
                        } else if (entity instanceof ExperienceOrb) {
                            stats.incrementExperienceOrbsRemoved();
                        } else if (entity instanceof Arrow) {
                            stats.incrementArrowsRemoved();
                        }
                        entity.remove();
                    }
                }
            }
        }
        
        return stats;
    }

    /**
     * 执行强制清理逻辑
     */
    private CleanupStats performForceCleanup() {
        CleanupStats stats = new CleanupStats();
        
        for (World world : Bukkit.getWorlds()) {
            if (world == null) continue;
            
            for (Chunk chunk : world.getLoadedChunks()) {
                if (chunk == null) continue;
                
                for (Entity entity : chunk.getEntities()) {
                    if (shouldForceCleanupEntity(entity)) {
                        if (entity instanceof Item) {
                            stats.incrementItemsRemoved();
                        } else if (entity instanceof ExperienceOrb) {
                            stats.incrementExperienceOrbsRemoved();
                        } else if (entity instanceof Arrow) {
                            stats.incrementArrowsRemoved();
                        } else if (entity instanceof Monster || entity instanceof Animals) {
                            stats.incrementMobsRemoved();
                        }
                        entity.remove();
                    }
                }
            }
        }
        
        return stats;
    }

    /**
     * 判断实体是否应该被强制清理
     */
    private boolean shouldForceCleanupEntity(Entity entity) {
        // 永远不清理玩家
        if (entity instanceof Player) {
            return false;
        }
        
        // 不清理有自定义名称的实体
        if (entity.getCustomName() != null && !entity.getCustomName().isEmpty()) {
            return false;
        }
        
        // 不清理拴绳实体
        if (entity instanceof LeashHitch) {
            return false;
        }
        
        // 不清理画和物品展示框
        if (entity instanceof Painting || entity instanceof ItemFrame) {
            return false;
        }
        
        // 不清理盔甲架
        if (entity instanceof ArmorStand) {
            return false;
        }
        
        // 清理物品（强制清理忽略年龄）
        if (entity instanceof Item) {
            return config.isCleanupItems();
        }
        
        // 清理经验球
        if (entity instanceof ExperienceOrb) {
            return config.isCleanupExperienceOrbs();
        }
        
        // 清理箭矢
        if (entity instanceof Arrow) {
            return config.isCleanupArrows();
        }
        
        // 清理敌对生物
        if (entity instanceof Monster) {
            return config.isCleanupHostileMobs();
        }
        
        // 清理被动生物（强制清理忽略驯服状态）
        if (entity instanceof Animals) {
            return config.isCleanupPassiveMobs();
        }
        
        return false;
    }

    /**
     * 判断实体是否应该被清理（标准清理）
     */
    private boolean shouldCleanupEntity(Entity entity) {
        // 永远不清理玩家
        if (entity instanceof Player) {
            return false;
        }
        
        // 不清理有自定义名称的实体
        if (entity.getCustomName() != null && !entity.getCustomName().isEmpty()) {
            return false;
        }
        
        // 不清理拴绳实体
        if (entity instanceof LeashHitch) {
            return false;
        }
        
        // 不清理画和物品展示框
        if (entity instanceof Painting || entity instanceof ItemFrame) {
            return false;
        }
        
        // 不清理盔甲架
        if (entity instanceof ArmorStand) {
            return false;
        }
        
        // 清理物品
        if (entity instanceof Item) {
            if (!config.isCleanupItems()) return false;
            
            Item item = (Item) entity;
            // 检查物品年龄
            if (item.getTicksLived() < config.getItemMinAge()) {
                return false;
            }
            return true;
        }
        
        // 清理经验球
        if (entity instanceof ExperienceOrb) {
            if (!config.isCleanupExperienceOrbs()) return false;
            
            ExperienceOrb orb = (ExperienceOrb) entity;
            // 检查经验球年龄
            if (orb.getTicksLived() < config.getExperienceOrbMinAge()) {
                return false;
            }
            return true;
        }
        
        // 清理箭矢
        if (entity instanceof Arrow) {
            if (!config.isCleanupArrows()) return false;
            
            Arrow arrow = (Arrow) entity;
            // 不清理玩家射出的箭
            if (arrow.getShooter() instanceof Player) {
                return false;
            }
            // 检查箭矢年龄
            if (arrow.getTicksLived() < config.getArrowMinAge()) {
                return false;
            }
            return true;
        }
        
        // 清理敌对生物
        if (entity instanceof Monster) {
            return config.isCleanupHostileMobs();
        }
        
        // 清理被动生物
        if (entity instanceof Animals) {
            if (!config.isCleanupPassiveMobs()) return false;
            
            Animals animal = (Animals) entity;
            // 不清理已驯服的动物
            if (animal instanceof Tameable) {
                Tameable tameable = (Tameable) animal;
                if (tameable.isTamed()) {
                    return false;
                }
            }
            return true;
        }
        
        return false;
    }

    // Getter methods
    public boolean isCleanupRunning() {
        return isCleanupRunning;
    }

    public long getLastCleanupTime() {
        return lastCleanupTime;
    }
}