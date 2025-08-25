package com.xiaoxiao.arissweeping.command;

import com.xiaoxiao.arissweeping.ArisSweeping;
import com.xiaoxiao.arissweeping.permission.PermissionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 主命令处理器 - 负责分发命令到各个专门的处理器
 */
public class CleanupCommand implements CommandExecutor {
    private final ArisSweeping plugin;
    private final PermissionManager permissionManager;
    private final CleanupExecutor cleanupExecutor;
    private final StatsHandler statsHandler;
    private final ConfigHandler configHandler;
    private final PermissionCommandHandler permissionCommandHandler;
    
    // 测试相关
    private BukkitTask testTimerTask = null;
    
    public CleanupCommand(ArisSweeping plugin) {
        this.plugin = plugin;
        this.permissionManager = plugin.getPermissionManager();
        this.cleanupExecutor = new CleanupExecutor(plugin);
        this.statsHandler = new StatsHandler(plugin);
        this.configHandler = new ConfigHandler(plugin);
        this.permissionCommandHandler = new PermissionCommandHandler(plugin);
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
        
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "cleanup":
                cleanupExecutor.handleCleanupCommand(sender, args);
                break;
            case "stats":
                statsHandler.handleStatsCommand(sender);
                break;
            case "tps":
                statsHandler.handleTpsCommand(sender);
                break;
            case "config":
                configHandler.handleConfigCommand(sender, args);
                break;
            case "toggle":
                configHandler.handleToggleCommand(sender);
                break;
            case "permission":
                permissionCommandHandler.handlePermissionCommand(sender, args);
                break;
            case "test":
                handleTestCommand(sender, args);
                break;
            // livestock-stats命令已移除，现在使用YAML配置和Spark API自动监控
            case "help":
            default:
                sendHelp(sender);
                break;
        }
        
        return true;
    }
    
    /**
     * 处理测试命令
     */
    private void handleTestCommand(CommandSender sender, String[] args) {
        if (!permissionManager.hasPermission(sender, PermissionManager.ADMIN)) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 老师~没有权限使用测试功能哦~");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "[邦邦卡邦！] 用法: /cleanup test <timer|status>");
            return;
        }
        
        String testType = args[1].toLowerCase();
        switch (testType) {
            case "timer":
                testTimer(sender);
                break;
            case "status":
                showTimerStatus(sender);
                break;
            default:
                sender.sendMessage(ChatColor.YELLOW + "[邦邦卡邦！] 用法: /cleanup test <timer|status>");
                break;
        }
    }
    
    /**
     * 测试定时器
     */
    private void testTimer(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 爱丽丝开始测试定时清理功能~");
        if (plugin.getCleanupHandler() != null) {
            plugin.getCleanupHandler().startCleanupTask();
        } else {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 清理处理器未初始化！");
        }
    }
    
    /**
     * 显示定时器状态
     */
    private void showTimerStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "========== [邦邦卡邦！] 爱丽丝状态报告 ==========");
        
        // 插件状态
        boolean enabled = plugin.getModConfig().getBoolean("enabled", true);
        sender.sendMessage(ChatColor.YELLOW + "插件状态: " + (enabled ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        
        // 清理配置
        sender.sendMessage(ChatColor.YELLOW + "清理间隔: " + ChatColor.WHITE + plugin.getModConfig().getInt("cleanup.interval", 300) + "秒");
        sender.sendMessage(ChatColor.YELLOW + "物品清理: " + (plugin.getModConfig().getBoolean("cleanup.items.enabled", true) ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        sender.sendMessage(ChatColor.YELLOW + "生物清理: " + (plugin.getModConfig().getBoolean("cleanup.mobs.enabled", true) ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        sender.sendMessage(ChatColor.YELLOW + "经验清理: " + (plugin.getModConfig().getBoolean("cleanup.experience.enabled", true) ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        sender.sendMessage(ChatColor.YELLOW + "箭矢清理: " + (plugin.getModConfig().getBoolean("cleanup.arrows.enabled", true) ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        
        // 其他配置
        sender.sendMessage(ChatColor.YELLOW + "广播消息: " + (plugin.getModConfig().getBoolean("cleanup.broadcast", true) ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        sender.sendMessage(ChatColor.YELLOW + "异步清理: " + (plugin.getModConfig().getBoolean("cleanup.async", true) ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        sender.sendMessage(ChatColor.YELLOW + "TPS监控: " + (plugin.getModConfig().getBoolean("cleanup.tps-monitor", true) ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        
        // 定时任务状态
        boolean hasScheduledTask = plugin.getCleanupHandler() != null;
        sender.sendMessage(ChatColor.YELLOW + "定时任务: " + (hasScheduledTask ? ChatColor.GREEN + "运行中" : ChatColor.RED + "未运行"));
        
        if (hasScheduledTask) {
            sender.sendMessage(ChatColor.YELLOW + "清理处理器: " + ChatColor.GREEN + "已初始化");
            sender.sendMessage(ChatColor.YELLOW + "下次清理: " + ChatColor.WHITE + plugin.getModConfig().getCleanupInterval() + "秒间隔");
        }
        
        sender.sendMessage(ChatColor.AQUA + "===========================================");
    }
    

    
    /**
     * 发送帮助信息
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "========== [邦邦卡邦！] 爱丽丝帮助 ==========");
        
        if (permissionManager.hasPermission(sender, PermissionManager.CLEANUP)) {
            sender.sendMessage(ChatColor.YELLOW + "/cleanup cleanup items" + ChatColor.WHITE + " - 清理掉落物品");
            sender.sendMessage(ChatColor.YELLOW + "/cleanup cleanup mobs" + ChatColor.WHITE + " - 清理敌对生物");
            sender.sendMessage(ChatColor.YELLOW + "/cleanup cleanup all" + ChatColor.WHITE + " - 强制清理所有实体");
        }
        
        if (permissionManager.hasPermission(sender, PermissionManager.STATS)) {
            sender.sendMessage(ChatColor.YELLOW + "/cleanup stats" + ChatColor.WHITE + " - 查看服务器统计");
            sender.sendMessage(ChatColor.YELLOW + "/cleanup tps" + ChatColor.WHITE + " - 查看TPS状态");
            sender.sendMessage(ChatColor.YELLOW + "/cleanup livestock-stats" + ChatColor.WHITE + " - 查看Spark增强版畜牧业统计");
        }
        
        if (permissionManager.hasPermission(sender, PermissionManager.CONFIG)) {
            sender.sendMessage(ChatColor.YELLOW + "/cleanup config" + ChatColor.WHITE + " - 配置插件设置");
            sender.sendMessage(ChatColor.YELLOW + "/cleanup toggle" + ChatColor.WHITE + " - 切换插件开关");
        }
        
        if (permissionManager.hasPermission(sender, PermissionManager.ADMIN)) {
            sender.sendMessage(ChatColor.YELLOW + "/cleanup permission" + ChatColor.WHITE + " - 权限管理");
            sender.sendMessage(ChatColor.YELLOW + "/cleanup test" + ChatColor.WHITE + " - 测试功能");
        }
        
        sender.sendMessage(ChatColor.YELLOW + "/cleanup help" + ChatColor.WHITE + " - 显示此帮助");
        sender.sendMessage(ChatColor.AQUA + "===========================================");
    }
    
    /**
     * 检查是否有任何权限
     */
    private boolean hasAnyPermission(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return true; // 控制台总是有权限
        }
        
        Player player = (Player) sender;
        String playerName = player.getName();
        
        return permissionManager.hasPermission(playerName, PermissionManager.ADMIN) ||
               permissionManager.hasPermission(playerName, PermissionManager.CLEANUP) ||
               permissionManager.hasPermission(playerName, PermissionManager.STATS) ||
               permissionManager.hasPermission(playerName, PermissionManager.CONFIG) ||
               player.hasPermission(PermissionManager.ADMIN) ||
               player.hasPermission(PermissionManager.CLEANUP) ||
               player.hasPermission(PermissionManager.STATS) ||
               player.hasPermission(PermissionManager.CONFIG);
    }
    
    /**
     * 检查权限
     */
    // 权限检查方法已移至PermissionManager统一处理
}