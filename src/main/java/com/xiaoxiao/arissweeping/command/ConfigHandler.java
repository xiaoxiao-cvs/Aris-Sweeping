package com.xiaoxiao.arissweeping.command;

import com.xiaoxiao.arissweeping.ArisSweeping;
import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.permission.PermissionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 配置管理器 - 负责处理配置相关命令
 */
public class ConfigHandler {
    private final ArisSweeping plugin;
    private final ModConfig config;
    private final PermissionManager permissionManager;

    public ConfigHandler(ArisSweeping plugin) {
        this.plugin = plugin;
        this.config = plugin.getModConfig();
        this.permissionManager = plugin.getPermissionManager();
    }

    /**
     * 处理配置命令
     */
    public void handleConfigCommand(CommandSender sender, String[] args) {
        // 检查权限
        if (!permissionManager.hasPermission(sender, PermissionManager.CONFIG)) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
            return;
        }

        if (args.length == 1) {
            sendConfigHelp(sender);
            return;
        }

        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "interval":
                handleIntervalConfig(sender, args);
                break;
            case "items":
                handleBooleanConfig(sender, args, "cleanup-items", "物品清理");
                break;
            case "mobs":
                handleBooleanConfig(sender, args, "cleanup-hostile-mobs", "敌对生物清理");
                break;
            case "animals":
                handleBooleanConfig(sender, args, "cleanup-passive-mobs", "被动生物清理");
                break;
            case "experience":
            case "exp":
                handleBooleanConfig(sender, args, "cleanup-experience-orbs", "经验球清理");
                break;
            case "arrows":
                handleBooleanConfig(sender, args, "cleanup-arrows", "箭矢清理");
                break;
            case "broadcast":
                handleBooleanConfig(sender, args, "broadcast-cleanup", "清理广播");
                break;
            case "async":
                handleBooleanConfig(sender, args, "async-cleanup", "异步清理");
                break;
            case "tps-monitor":
                handleBooleanConfig(sender, args, "tps-monitor-enabled", "TPS监控");
                break;
            // livestock配置已移除，现在使用YAML配置文件
            case "item-age":
                handleIntConfig(sender, args, "item-min-age", "物品最小年龄", 0, 12000);
                break;
            case "exp-age":
                handleIntConfig(sender, args, "experience-orb-min-age", "经验球最小年龄", 0, 12000);
                break;
            case "arrow-age":
                handleIntConfig(sender, args, "arrow-min-age", "箭矢最小年龄", 0, 12000);
                break;
            case "list":
                sendConfigList(sender);
                break;
            case "get":
                handleGetConfig(sender, args);
                break;
            case "reload":
                config.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 配置已重新加载！");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 未知的配置项: " + subCommand);
                sendConfigHelp(sender);
                break;
        }
    }

    /**
     * 处理间隔配置
     */
    private void handleIntervalConfig(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 用法: /arissweeping config interval <秒数>");
            sender.sendMessage(ChatColor.YELLOW + "当前清理间隔: " + config.getCleanupInterval() + " 秒");
            return;
        }
        
        try {
            int interval = Integer.parseInt(args[2]);
            if (interval < 30) {
                sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 清理间隔不能少于30秒！");
                return;
            }
            if (interval > 3600) {
                sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 清理间隔不能超过3600秒（1小时）！");
                return;
            }
            
            config.setCleanupInterval(interval);
            config.saveConfig();
            
            // 重启清理任务以应用新间隔
            plugin.getCleanupHandler().restartCleanupTask();
            
            sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 清理间隔已设置为 " + ChatColor.YELLOW + interval + ChatColor.GREEN + " 秒！");
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 请输入有效的数字！");
        }
    }

    /**
     * 处理布尔值配置
     */
    private void handleBooleanConfig(CommandSender sender, String[] args, String configPath, String displayName) {
        if (args.length < 3) {
            boolean currentValue = getBooleanConfigValue(configPath);
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 用法: /arissweeping config " + args[1] + " <true/false>");
            sender.sendMessage(ChatColor.YELLOW + "当前" + displayName + "状态: " + (currentValue ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
            return;
        }
        
        String value = args[2].toLowerCase();
        boolean enable;
        
        if (value.equals("true") || value.equals("on") || value.equals("enable") || value.equals("启用")) {
            enable = true;
        } else if (value.equals("false") || value.equals("off") || value.equals("disable") || value.equals("禁用")) {
            enable = false;
        } else {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 请输入 true 或 false！");
            return;
        }
        
        setBooleanConfigValue(configPath, enable);
        config.saveConfig();
        
        sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] " + displayName + "已" + (enable ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用") + ChatColor.GREEN + "！");
        
        // 特殊处理：如果是TPS监控或畜牧业检查，需要重启相关任务
        if (configPath.equals("tps-monitor-enabled") || configPath.equals("livestock-density-check")) {
            plugin.getCleanupHandler().restartCleanupTask();
        }
    }

    /**
     * 处理整数配置
     */
    private void handleIntConfig(CommandSender sender, String[] args, String configPath, String displayName, int min, int max) {
        if (args.length < 3) {
            int currentValue = getIntConfigValue(configPath);
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 用法: /arissweeping config " + args[1] + " <数值>");
            sender.sendMessage(ChatColor.YELLOW + "当前" + displayName + ": " + currentValue);
            sender.sendMessage(ChatColor.GRAY + "允许范围: " + min + " - " + max);
            return;
        }
        
        try {
            int value = Integer.parseInt(args[2]);
            if (value < min || value > max) {
                sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 数值必须在 " + min + " 到 " + max + " 之间！");
                return;
            }
            
            setIntConfigValue(configPath, value);
            config.saveConfig();
            
            sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] " + displayName + "已设置为 " + ChatColor.YELLOW + value + ChatColor.GREEN + "！");
            
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 请输入有效的数字！");
        }
    }

    /**
     * 发送配置帮助
     */
    private void sendConfigHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "配置命令帮助:");
        sender.sendMessage(ChatColor.YELLOW + "/arissweeping config interval <秒数>" + ChatColor.WHITE + " - 设置清理间隔");
        sender.sendMessage(ChatColor.YELLOW + "/arissweeping config items <true/false>" + ChatColor.WHITE + " - 物品清理开关");
        sender.sendMessage(ChatColor.YELLOW + "/arissweeping config mobs <true/false>" + ChatColor.WHITE + " - 敌对生物清理开关");
        sender.sendMessage(ChatColor.YELLOW + "/arissweeping config animals <true/false>" + ChatColor.WHITE + " - 被动生物清理开关");
        sender.sendMessage(ChatColor.YELLOW + "/arissweeping config exp <true/false>" + ChatColor.WHITE + " - 经验球清理开关");
        sender.sendMessage(ChatColor.YELLOW + "/arissweeping config arrows <true/false>" + ChatColor.WHITE + " - 箭矢清理开关");
        sender.sendMessage(ChatColor.YELLOW + "/arissweeping config broadcast <true/false>" + ChatColor.WHITE + " - 清理广播开关");
        sender.sendMessage(ChatColor.YELLOW + "/arissweeping config async <true/false>" + ChatColor.WHITE + " - 异步清理开关");
        sender.sendMessage(ChatColor.YELLOW + "/arissweeping config tps-monitor <true/false>" + ChatColor.WHITE + " - TPS监控开关");
        // livestock配置命令已移除，请使用YAML配置文件
        sender.sendMessage(ChatColor.YELLOW + "/arissweeping config item-age <tick>" + ChatColor.WHITE + " - 物品最小年龄");
        sender.sendMessage(ChatColor.YELLOW + "/arissweeping config exp-age <tick>" + ChatColor.WHITE + " - 经验球最小年龄");
        sender.sendMessage(ChatColor.YELLOW + "/arissweeping config arrow-age <tick>" + ChatColor.WHITE + " - 箭矢最小年龄");
        sender.sendMessage(ChatColor.YELLOW + "/arissweeping config list" + ChatColor.WHITE + " - 查看当前配置");
        sender.sendMessage(ChatColor.YELLOW + "/arissweeping config reload" + ChatColor.WHITE + " - 重新加载配置");
    }

    /**
     * 发送配置列表
     */
    private void sendConfigList(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "当前配置:");
        sender.sendMessage(ChatColor.AQUA + "=== 基础设置 ===");
        sender.sendMessage(ChatColor.WHITE + "插件启用: " + (config.isPluginEnabled() ? ChatColor.GREEN + "是" : ChatColor.RED + "否"));
        sender.sendMessage(ChatColor.WHITE + "清理间隔: " + ChatColor.YELLOW + config.getCleanupInterval() + ChatColor.WHITE + " 秒");
        sender.sendMessage(ChatColor.WHITE + "广播清理: " + (config.isBroadcastCleanup() ? ChatColor.GREEN + "是" : ChatColor.RED + "否"));
        sender.sendMessage(ChatColor.WHITE + "异步清理: " + (config.isAsyncCleanup() ? ChatColor.GREEN + "是" : ChatColor.RED + "否"));
        
        sender.sendMessage(ChatColor.AQUA + "=== 清理设置 ===");
        sender.sendMessage(ChatColor.WHITE + "物品清理: " + (config.isCleanupItems() ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        sender.sendMessage(ChatColor.WHITE + "敌对生物清理: " + (config.isCleanupHostileMobs() ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        sender.sendMessage(ChatColor.WHITE + "被动生物清理: " + (config.isCleanupPassiveMobs() ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        sender.sendMessage(ChatColor.WHITE + "经验球清理: " + (config.isCleanupExperienceOrbs() ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        sender.sendMessage(ChatColor.WHITE + "箭矢清理: " + (config.isCleanupArrows() ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        
        sender.sendMessage(ChatColor.AQUA + "=== 年龄设置 ===");
        sender.sendMessage(ChatColor.WHITE + "物品最小年龄: " + ChatColor.YELLOW + config.getItemMinAge() + ChatColor.WHITE + " tick");
        sender.sendMessage(ChatColor.WHITE + "经验球最小年龄: " + ChatColor.YELLOW + config.getExperienceOrbMinAge() + ChatColor.WHITE + " tick");
        sender.sendMessage(ChatColor.WHITE + "箭矢最小年龄: " + ChatColor.YELLOW + config.getArrowMinAge() + ChatColor.WHITE + " tick");
        
        sender.sendMessage(ChatColor.AQUA + "=== 监控设置 ===");
        sender.sendMessage(ChatColor.WHITE + "TPS监控: " + (config.isTpsMonitorEnabled() ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        sender.sendMessage(ChatColor.GRAY + "畜牧业配置: 请查看YAML配置文件 (config.yml)");
    }

    /**
     * 处理获取配置命令
     */
    private void handleGetConfig(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 用法: /arissweeping config get <配置项>");
            sender.sendMessage(ChatColor.YELLOW + "可用配置项: interval, items, mobs, animals, experience, arrows, broadcast, async, tps-monitor, item-age, exp-age, arrow-age");
            return;
        }

        String configItem = args[2].toLowerCase();
        
        switch (configItem) {
            case "interval":
                sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 清理间隔: " + config.getCleanupInterval() + " 秒");
                break;
            case "items":
                sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 物品清理: " + (getBooleanConfigValue("cleanup-items") ? "启用" : "禁用"));
                break;
            case "mobs":
                sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 敌对生物清理: " + (getBooleanConfigValue("cleanup-hostile-mobs") ? "启用" : "禁用"));
                break;
            case "animals":
                sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 被动生物清理: " + (getBooleanConfigValue("cleanup-passive-mobs") ? "启用" : "禁用"));
                break;
            case "experience":
            case "exp":
                sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 经验球清理: " + (getBooleanConfigValue("cleanup-experience-orbs") ? "启用" : "禁用"));
                break;
            case "arrows":
                sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 箭矢清理: " + (getBooleanConfigValue("cleanup-arrows") ? "启用" : "禁用"));
                break;
            case "broadcast":
                sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 清理广播: " + (getBooleanConfigValue("broadcast-cleanup") ? "启用" : "禁用"));
                break;
            case "async":
                sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 异步清理: " + (getBooleanConfigValue("async-cleanup") ? "启用" : "禁用"));
                break;
            case "tps-monitor":
                sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] TPS监控: " + (getBooleanConfigValue("tps-monitor-enabled") ? "启用" : "禁用"));
                break;
            // livestock配置查询已移除，请查看YAML配置文件
            case "item-age":
                sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 物品最小年龄: " + getIntConfigValue("item-min-age") + " tick");
                break;
            case "exp-age":
                sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 经验球最小年龄: " + getIntConfigValue("experience-orb-min-age") + " tick");
                break;
            case "arrow-age":
                sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 箭矢最小年龄: " + getIntConfigValue("arrow-min-age") + " tick");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 未知的配置项: " + configItem);
                sender.sendMessage(ChatColor.YELLOW + "可用配置项: interval, items, mobs, animals, experience, arrows, broadcast, async, tps-monitor, item-age, exp-age, arrow-age");
                break;
        }
    }

    /**
     * 处理插件总开关命令
     */
    public void handleToggleCommand(CommandSender sender) {
        // 检查权限
        if (!permissionManager.hasPermission(sender, PermissionManager.CONFIG)) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
            return;
        }

        boolean currentState = config.isPluginEnabled();
        boolean newState = !currentState;
        
        config.setPluginEnabled(newState);
        config.saveConfig();
        
        if (newState) {
            sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 插件已启用！清理任务将开始运行。");
            // 启动清理任务
            plugin.getCleanupHandler().startCleanupTask();
        } else {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 插件已禁用！所有清理任务已停止。");
            // 停止清理任务
            plugin.getCleanupHandler().stopCleanupTask();
        }
    }

    /**
     * 获取布尔值配置
     */
    private boolean getBooleanConfigValue(String path) {
        switch (path) {
            case "cleanup-items": return config.isCleanupItems();
            case "cleanup-hostile-mobs": return config.isCleanupHostileMobs();
            case "cleanup-passive-mobs": return config.isCleanupPassiveMobs();
            case "cleanup-experience-orbs": return config.isCleanupExperienceOrbs();
            case "cleanup-arrows": return config.isCleanupArrows();
            case "broadcast-cleanup": return config.isBroadcastCleanup();
            case "async-cleanup": return config.isAsyncCleanup();
            case "tps-monitor-enabled": return config.isTpsMonitorEnabled();
            case "livestock-density-check": return config.isLivestockDensityCheckEnabled();
            default: return false;
        }
    }

    /**
     * 设置布尔值配置
     */
    private void setBooleanConfigValue(String path, boolean value) {
        switch (path) {
            case "cleanup-items": config.setCleanupItems(value); break;
            case "cleanup-hostile-mobs": config.setCleanupHostileMobs(value); break;
            case "cleanup-passive-mobs": config.setCleanupPassiveMobs(value); break;
            case "cleanup-experience-orbs": config.setCleanupExperienceOrbs(value); break;
            case "cleanup-arrows": config.setCleanupArrows(value); break;
            case "broadcast-cleanup": config.setBroadcastCleanup(value); break;
            case "async-cleanup": config.setAsyncCleanup(value); break;
            case "tps-monitor-enabled": config.setTpsMonitorEnabled(value); break;
            case "livestock-density-check": config.setLivestockDensityCheckEnabled(value); break;
        }
    }

    /**
     * 获取整数配置
     */
    private int getIntConfigValue(String path) {
        switch (path) {
            case "max-animals-per-chunk": return config.getMaxAnimalsPerChunk();
            case "item-min-age": return config.getItemMinAge();
            case "experience-orb-min-age": return config.getExperienceOrbMinAge();
            case "arrow-min-age": return config.getArrowMinAge();
            default: return 0;
        }
    }

    /**
     * 设置整数配置
     */
    private void setIntConfigValue(String path, int value) {
        switch (path) {
            // max-animals-per-chunk已移除，现在使用YAML配置
            case "item-min-age": config.setItemMinAge(value); break;
            case "experience-orb-min-age": config.setExperienceOrbMinAge(value); break;
            case "arrow-min-age": config.setArrowMinAge(value); break;
        }
    }

    /**
     * 检查权限
     */
    // 权限检查方法已移至PermissionManager统一处理
}