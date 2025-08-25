package com.xiaoxiao.arissweeping.command;

import com.xiaoxiao.arissweeping.ArisSweeping;
import com.xiaoxiao.arissweeping.cleanup.CleanupServiceManager;
import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.CleanupStats;
import com.xiaoxiao.arissweeping.util.CleanupStateManager.CleanupType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 清理执行器 - 负责执行各种清理任务
 */
public class CleanupExecutor {
    private final ArisSweeping plugin;
    private final ModConfig config;
    private final CleanupServiceManager serviceManager;
    private volatile long lastCleanupTime = 0;
    private static final long CLEANUP_COOLDOWN = 5000; // 5秒冷却时间
    // isCleanupRunning 字段已被 serviceManager.isAnyCleanupRunning() 替代

    public CleanupExecutor(ArisSweeping plugin) {
        this.plugin = plugin;
        this.config = plugin.getModConfig();
        this.serviceManager = new CleanupServiceManager(plugin);
    }
    
    /**
     * 处理清理命令
     */
    public void handleCleanupCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 用法: /aris cleanup <items|mobs|all|force|status>");
            return;
        }

        String cleanupType = args[1].toLowerCase();
        switch (cleanupType) {
            case "items":
                executeCleanup(CleanupType.STANDARD, sender, false);
                break;
            case "mobs":
                executeCleanup(CleanupType.LIVESTOCK, sender, false);
                break;
            case "all":
                executeAllCleanups(sender);
                break;
            case "force":
                executeForceCleanup(sender);
                break;
            case "status":
                showCleanupStatus(sender);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 无效的清理类型！使用: items, mobs, all, force, status");
                break;
        }
    }

    /**
     * 执行指定类型的清理
     * @param type 清理类型
     * @param sender 命令发送者
     * @param async 是否异步执行
     */
    private void executeCleanup(CleanupType type, CommandSender sender, boolean async) {
        // 检查冷却时间
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime < CLEANUP_COOLDOWN) {
            long remainingTime = (CLEANUP_COOLDOWN - (currentTime - lastCleanupTime)) / 1000;
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 清理冷却中，请等待 " + remainingTime + " 秒后再试！");
            return;
        }

        if (serviceManager.isCleanupRunning(type)) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 清理任务正在进行中，请稍候...");
            return;
        }

        lastCleanupTime = currentTime;
        sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 开始执行 " + getCleanupTypeName(type) + " 清理...");

        CompletableFuture<CleanupStats> future = serviceManager.executeCleanup(type, sender, async || config.isAsyncCleanup());
        
        future.thenAccept(stats -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                String message = formatCleanupResult(type, stats);
                if (config.isBroadcastCleanup()) {
                    Bukkit.broadcastMessage(message);
                } else {
                    sender.sendMessage(message);
                }
            });
        }).exceptionally(throwable -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 清理时发生错误: " + throwable.getMessage());
                plugin.getLogger().severe("清理执行失败: " + throwable.getMessage());
            });
            return null;
        });
    }
    
    // 旧的清理方法已被新的服务架构替代

    // executeCleanupItems 方法已被新的服务架构替代

    // executeCleanupMobs 方法已被新的服务架构替代

    // executeCleanupAll 方法已被新的服务架构替代

    // performStandardCleanup 方法已移至 StandardCleanupService

    // performForceCleanup 方法已移至服务类

    // shouldForceCleanupEntity 和 shouldCleanupEntity 方法已移至服务类

    /**
     * 执行所有类型的清理
     */
    private void executeAllCleanups(CommandSender sender) {
        executeCleanup(CleanupType.STANDARD, sender, false);
    }

    /**
     * 执行强制清理
     */
    private void executeForceCleanup(CommandSender sender) {
        executeCleanup(CleanupType.MANUAL, sender, false);
    }

    /**
      * 显示清理状态
      */
     private void showCleanupStatus(CommandSender sender) {
         sender.sendMessage(ChatColor.YELLOW + "[邦邦卡邦！] 清理状态:");
         
         // 显示各个清理服务的状态
         sender.sendMessage(ChatColor.GRAY + "  标准清理: " + 
             (serviceManager.isCleanupRunning(CleanupType.STANDARD) ? ChatColor.RED + "运行中" : ChatColor.GREEN + "空闲"));
         sender.sendMessage(ChatColor.GRAY + "  生物清理: " + 
             (serviceManager.isCleanupRunning(CleanupType.LIVESTOCK) ? ChatColor.RED + "运行中" : ChatColor.GREEN + "空闲"));
         
         long timeSinceLastCleanup = System.currentTimeMillis() - lastCleanupTime;
         if (timeSinceLastCleanup < CLEANUP_COOLDOWN) {
             long remainingTime = (CLEANUP_COOLDOWN - timeSinceLastCleanup) / 1000;
             sender.sendMessage(ChatColor.YELLOW + "  冷却时间: " + remainingTime + " 秒");
         } else {
             sender.sendMessage(ChatColor.GREEN + "  冷却时间: 已就绪");
         }
         
         // 显示详细状态信息
         sender.sendMessage(ChatColor.GRAY + "详细状态信息:");
         String statusInfo = serviceManager.getStatusInfo();
         for (String line : statusInfo.split("\n")) {
             if (!line.trim().isEmpty()) {
                 sender.sendMessage(ChatColor.GRAY + "  " + line);
             }
         }
     }

    /**
     * 获取清理类型的显示名称
     */
    private String getCleanupTypeName(CleanupType type) {
        switch (type) {
            case STANDARD:
                return "标准清理";
            case LIVESTOCK:
                return "生物清理";
            case MANUAL:
                return "强制清理";
            default:
                return "未知清理";
        }
    }

    /**
     * 格式化清理结果消息
     */
    private String formatCleanupResult(CleanupType type, CleanupStats stats) {
        StringBuilder message = new StringBuilder();
        message.append(ChatColor.GREEN).append("[邦邦卡邦！] ").append(getCleanupTypeName(type)).append("完成！清理了 ");
        
        if (stats.getItemsCleaned() > 0) {
            message.append(ChatColor.YELLOW).append(stats.getItemsCleaned()).append(ChatColor.GREEN).append(" 个物品，");
        }
        if (stats.getExperienceOrbsCleaned() > 0) {
            message.append(ChatColor.YELLOW).append(stats.getExperienceOrbsCleaned()).append(ChatColor.GREEN).append(" 个经验球，");
        }
        if (stats.getArrowsCleaned() > 0) {
            message.append(ChatColor.YELLOW).append(stats.getArrowsCleaned()).append(ChatColor.GREEN).append(" 支箭矢，");
        }
        if (stats.getMobsCleaned() > 0) {
            message.append(ChatColor.YELLOW).append(stats.getMobsCleaned()).append(ChatColor.GREEN).append(" 个生物，");
        }
        
        // 移除最后的逗号
        String result = message.toString();
        if (result.endsWith("，")) {
            result = result.substring(0, result.length() - 1);
        }
        result += "！";
        
        return result;
    }

    // Getter methods
    public boolean isCleanupRunning() {
        return serviceManager.isAnyCleanupRunning();
    }

    public long getLastCleanupTime() {
        return lastCleanupTime;
    }

    public CleanupServiceManager getServiceManager() {
        return serviceManager;
    }
}