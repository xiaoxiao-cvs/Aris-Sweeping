package com.xiaoxiao.arissweeping.command;

import com.xiaoxiao.arissweeping.ArisSweeping;
import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.CleanupStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CleanupCommand implements CommandExecutor, TabCompleter {
    private final ArisSweeping plugin;
    private final ModConfig config;
    
    public CleanupCommand(ArisSweeping plugin) {
        this.plugin = plugin;
        this.config = plugin.getModConfig();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("arissweeping.admin")) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 老师，您没有权限指挥爱丽丝哦~");
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
            case "config":
                handleConfigCommand(sender, args);
                break;
            case "toggle":
            case "switch":
                handleToggleCommand(sender);
                break;
            case "help":
            default:
                sendHelp(sender);
                break;
        }
        
        return true;
    }
    
    private void handleCleanupCommand(CommandSender sender, String[] args) {
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
    }
    
    private void executeStandardCleanup(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝开始认真打扫了~");
        
        new BukkitRunnable() {
            @Override
            public void run() {
                CleanupStats stats = performStandardCleanup();
                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (stats.getTotalCleaned() > 0) {
                            String message = String.format(
                                ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝扫掉了 " + ChatColor.RED + "%d" + ChatColor.WHITE + " 个实体呢~ (物品: " + ChatColor.YELLOW + "%d" + ChatColor.WHITE + ", 经验球: " + ChatColor.YELLOW + "%d" + ChatColor.WHITE + ", 箭矢: " + ChatColor.YELLOW + "%d" + ChatColor.WHITE + ", 凋落物: " + ChatColor.YELLOW + "%d" + ChatColor.WHITE + ", 生物: " + ChatColor.YELLOW + "%d" + ChatColor.WHITE + ")",
                                stats.getTotalCleaned(),
                                stats.getItemsCleaned(),
                                stats.getExperienceOrbsCleaned(),
                                stats.getArrowsCleaned(),
                                stats.getFallingBlocksCleaned(),
                                stats.getMobsCleaned()
                            );
                            sender.sendMessage(message);
                            
                            if (config.isBroadcastCleanup()) {
                                Bukkit.broadcastMessage(message);
                            }
                        } else {
                            sender.sendMessage(ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝没有找到需要打扫的东西呢~");
                        }
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }
    
    private void executeCleanupItems(CommandSender sender) {
        CleanupStats stats = new CleanupStats();
        List<Entity> toRemove = new ArrayList<>();
        
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item) {
                    toRemove.add(entity);
                    stats.incrementItems();
                }
            }
        }
        
        for (Entity entity : toRemove) {
            entity.remove();
        }
        
        sender.sendMessage(String.format(
            ChatColor.GOLD + "[爱丽丝扫地] " + ChatColor.WHITE + "清理了 " + ChatColor.RED + "%d" + ChatColor.WHITE + " 个掉落物",
            stats.getItemsCleaned()
        ));
    }
    
    private void executeCleanupMobs(CommandSender sender) {
        CleanupStats stats = new CleanupStats();
        List<Entity> toRemove = new ArrayList<>();
        
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Monster && entity.getCustomName() == null) {
                    toRemove.add(entity);
                    stats.incrementMobs();
                }
            }
        }
        
        for (Entity entity : toRemove) {
            entity.remove();
        }
        
        sender.sendMessage(String.format(
            ChatColor.GOLD + "[爱丽丝扫地] " + ChatColor.WHITE + "清理了 " + ChatColor.RED + "%d" + ChatColor.WHITE + " 个敌对生物",
            stats.getMobsCleaned()
        ));
    }
    
    private void executeCleanupAll(CommandSender sender) {
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
        
        for (Entity entity : toRemove) {
            entity.remove();
        }
        
        sender.sendMessage(String.format(
            ChatColor.GOLD + "[爱丽丝扫地] " + ChatColor.WHITE + "强制清理了 " + ChatColor.RED + "%d" + ChatColor.WHITE + " 个实体",
            stats.getTotalCleaned()
        ));
    }
    
    private void handleStatsCommand(CommandSender sender) {
        int totalEntities = 0;
        int players = 0;
        int items = 0;
        int mobs = 0;
        int experienceOrbs = 0;
        int arrows = 0;
        int fallingBlocks = 0;
        
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                totalEntities++;
                if (entity instanceof Item) items++;
                else if (entity instanceof Mob) mobs++;
                else if (entity instanceof ExperienceOrb) experienceOrbs++;
                else if (entity instanceof Arrow) arrows++;
                else if (entity instanceof org.bukkit.entity.FallingBlock) fallingBlocks++;
                else if (entity instanceof Player) players++;
            }
        }
        
        sender.sendMessage(String.format(
            ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝为老师统计了实体情况~\n" +
            ChatColor.WHITE + "总实体数: " + ChatColor.YELLOW + "%d\n" +
            ChatColor.WHITE + "玩家: " + ChatColor.GREEN + "%d\n" +
            ChatColor.WHITE + "掉落物: " + ChatColor.YELLOW + "%d\n" +
            ChatColor.WHITE + "敌对生物: " + ChatColor.RED + "%d\n" +
            ChatColor.WHITE + "经验球: " + ChatColor.YELLOW + "%d\n" +
            ChatColor.WHITE + "箭矢: " + ChatColor.YELLOW + "%d\n" +
            ChatColor.WHITE + "凋落物: " + ChatColor.YELLOW + "%d\n" +
            ChatColor.WHITE + "清理间隔: " + ChatColor.YELLOW + "%d" + ChatColor.WHITE + "秒",
            totalEntities, players, items, mobs, experienceOrbs, arrows, fallingBlocks,
            config.getCleanupInterval()
        ));
    }
    
    private void handleConfigCommand(CommandSender sender, String[] args) {
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
                handleBooleanConfig(sender, args, "entity_cleanup.cleanupFallingBlocks", "凋落物清理");
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
        
        sender.sendMessage(String.format(
            ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝已将 " + ChatColor.YELLOW + "%s" + 
            ChatColor.WHITE + " 设置为 " + (value ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭") + ChatColor.WHITE + "~",
            displayName
        ));
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
            
            sender.sendMessage(String.format(
                ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝已将 " + ChatColor.YELLOW + "%s" + 
                ChatColor.WHITE + " 设置为 " + ChatColor.YELLOW + "%d" + ChatColor.WHITE + "~",
                displayName, value
            ));
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 爱丽丝需要一个有效的数字哦~");
        }
    }
    
    private void sendConfigHelp(CommandSender sender) {
        sender.sendMessage(
            ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝的配置指令帮助~\n" +
            ChatColor.YELLOW + "基础设置:\n" +
            ChatColor.WHITE + "  /arissweeping config enable <true|false> - 插件总开关\n" +
            ChatColor.WHITE + "  /arissweeping config interval <秒> - 清理间隔\n" +
            ChatColor.YELLOW + "清理设置:\n" +
            ChatColor.WHITE + "  /arissweeping config cleanup-items <true|false> - 掉落物清理\n" +
            ChatColor.WHITE + "  /arissweeping config cleanup-mobs <true|false> - 敌对生物清理\n" +
            ChatColor.WHITE + "  /arissweeping config cleanup-animals <true|false> - 被动生物清理\n" +
            ChatColor.WHITE + "  /arissweeping config cleanup-arrows <true|false> - 箭矢清理\n" +
            ChatColor.WHITE + "  /arissweeping config cleanup-falling <true|false> - 凋落物清理\n" +
            ChatColor.YELLOW + "畜牧业管理:\n" +
            ChatColor.WHITE + "  /arissweeping config livestock-check <true|false> - 密度检测\n" +
            ChatColor.WHITE + "  /arissweeping config livestock-limit <数量> - 每区块最大动物数\n" +
            ChatColor.WHITE + "  /arissweeping config warning-time <分钟> - 预警时间\n" +
            ChatColor.YELLOW + "其他:\n" +
            ChatColor.WHITE + "  /arissweeping config broadcast <true|false> - 清理消息广播\n" +
            ChatColor.WHITE + "  /arissweeping config show-stats <true|false> - 详细统计显示\n" +
            ChatColor.WHITE + "  /arissweeping config list - 查看当前配置\n" +
            ChatColor.WHITE + "  /arissweeping config reload - 重新加载配置"
        );
    }
    
    private void sendConfigList(CommandSender sender) {
        sender.sendMessage(
            ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝的当前配置~\n" +
            ChatColor.YELLOW + "基础设置:\n" +
            ChatColor.WHITE + "  插件状态: " + (config.isPluginEnabled() ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭") + "\n" +
            ChatColor.WHITE + "  清理间隔: " + ChatColor.YELLOW + config.getCleanupInterval() + ChatColor.WHITE + " 秒\n" +
            ChatColor.YELLOW + "清理设置:\n" +
            ChatColor.WHITE + "  掉落物: " + (config.isCleanupItems() ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭") + "\n" +
            ChatColor.WHITE + "  敌对生物: " + (config.isCleanupHostileMobs() ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭") + "\n" +
            ChatColor.WHITE + "  被动生物: " + (config.isCleanupPassiveMobs() ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭") + "\n" +
            ChatColor.WHITE + "  箭矢: " + (config.isCleanupArrows() ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭") + "\n" +
            ChatColor.WHITE + "  凋落物: " + (config.isCleanupFallingBlocks() ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭") + "\n" +
            ChatColor.YELLOW + "畜牧业管理:\n" +
            ChatColor.WHITE + "  密度检测: " + (config.isLivestockDensityCheckEnabled() ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭") + "\n" +
            ChatColor.WHITE + "  每区块最大动物数: " + ChatColor.YELLOW + config.getMaxAnimalsPerChunk() + "\n" +
            ChatColor.WHITE + "  预警时间: " + ChatColor.YELLOW + config.getWarningTime() + ChatColor.WHITE + " 分钟"
        );
    }
    
    private void handleToggleCommand(CommandSender sender) {
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
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(
            ChatColor.GOLD + "═══════════════════════════════════════\n" +
            ChatColor.GOLD + "        " + ChatColor.YELLOW + "✨ 爱丽丝清理系统 ✨" + ChatColor.GOLD + "\n" +
            ChatColor.GOLD + "═══════════════════════════════════════\n" +
            ChatColor.WHITE + "\n" +
            ChatColor.AQUA + "🧹 基础指令:\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping cleanup" + ChatColor.GRAY + " - 执行标准清理\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping cleanup items" + ChatColor.GRAY + " - 清理所有掉落物\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping cleanup mobs" + ChatColor.GRAY + " - 清理敌对生物\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping cleanup all" + ChatColor.GRAY + " - 强制清理所有实体\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping stats" + ChatColor.GRAY + " - 显示实体统计\n" +
            ChatColor.WHITE + "\n" +
            ChatColor.LIGHT_PURPLE + "⚙️ 配置管理:\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config help" + ChatColor.GRAY + " - 配置指令帮助\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config list" + ChatColor.GRAY + " - 查看当前配置\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config enable <true|false>" + ChatColor.GRAY + " - 插件总开关\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping toggle" + ChatColor.GRAY + " - 快速切换插件开关\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config interval <秒>" + ChatColor.GRAY + " - 设置清理间隔\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config reload" + ChatColor.GRAY + " - 重新加载配置\n" +
            ChatColor.WHITE + "\n" +
            ChatColor.YELLOW + "🐄 畜牧业管理:\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config livestock-check <true|false>" + ChatColor.GRAY + " - 密度检测\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config livestock-limit <数量>" + ChatColor.GRAY + " - 每区块动物限制\n" +
            ChatColor.WHITE + "\n" +
            ChatColor.RED + "🔥 快速清理:\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config cleanup-items <true|false>" + ChatColor.GRAY + " - 掉落物清理\n" +
            ChatColor.WHITE + "  " + ChatColor.GREEN + "/arissweeping config cleanup-mobs <true|false>" + ChatColor.GRAY + " - 敌对生物清理\n" +
            ChatColor.WHITE + "\n" +
            ChatColor.GOLD + "═══════════════════════════════════════\n" +
            ChatColor.GRAY + "        由爱丽丝为您提供清理服务 💖\n" +
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
        if (!sender.hasPermission("arissweeping.admin")) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            return Arrays.asList("cleanup", "stats", "config", "help")
                    .stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("cleanup")) {
                return Arrays.asList("items", "mobs", "all")
                        .stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("config")) {
                return Arrays.asList("interval", "reload")
                        .stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        return new ArrayList<>();
    }
}