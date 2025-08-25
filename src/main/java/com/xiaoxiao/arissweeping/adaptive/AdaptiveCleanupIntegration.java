package com.xiaoxiao.arissweeping.adaptive;

import com.xiaoxiao.arissweeping.ArisSweeping;
import com.xiaoxiao.arissweeping.handler.EntityCleanupHandler;
import com.xiaoxiao.arissweeping.util.LoggerUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 自适应清理集成类
 * 负责将自适应清理服务集成到主插件中
 */
public class AdaptiveCleanupIntegration implements CommandExecutor {
    
    private final ArisSweeping plugin;
    private final AdaptiveCleanupService adaptiveService;
    private final EntityCleanupHandler originalHandler;
    
    // 集成状态
    private boolean adaptiveMode = false;
    
    public AdaptiveCleanupIntegration(ArisSweeping plugin) {
        this.plugin = plugin;
        this.originalHandler = plugin.getCleanupHandler();
        this.adaptiveService = new AdaptiveCleanupService(plugin);
    }
    
    /**
     * 启用自适应模式
     */
    public void enableAdaptiveMode() {
        if (adaptiveMode) {
            LoggerUtil.warning("AdaptiveCleanupIntegration", "自适应模式已启用");
            return;
        }
        
        try {
            // 停止原有的清理服务
            if (originalHandler != null) {
                // 这里可以添加停止原有服务的逻辑
                LoggerUtil.info("AdaptiveCleanupIntegration", "正在停止原有清理服务");
            }
            
            // 启动自适应清理服务
            adaptiveService.start();
            adaptiveMode = true;
            
            LoggerUtil.info("AdaptiveCleanupIntegration", "自适应清理模式已启用");
            
        } catch (Exception e) {
            LoggerUtil.severe("AdaptiveCleanupIntegration", "启用自适应模式失败", e);
            adaptiveMode = false;
        }
    }
    
    /**
     * 禁用自适应模式
     */
    public void disableAdaptiveMode() {
        if (!adaptiveMode) {
            LoggerUtil.warning("AdaptiveCleanupIntegration", "自适应模式未启用");
            return;
        }
        
        try {
            // 停止自适应清理服务
            adaptiveService.stop();
            
            // 恢复原有的清理服务
            if (originalHandler != null) {
                // 这里可以添加恢复原有服务的逻辑
                LoggerUtil.info("AdaptiveCleanupIntegration", "正在恢复原有清理服务");
            }
            
            adaptiveMode = false;
            
            LoggerUtil.info("AdaptiveCleanupIntegration", "自适应清理模式已禁用");
            
        } catch (Exception e) {
            LoggerUtil.severe("AdaptiveCleanupIntegration", "禁用自适应模式失败", e);
        }
    }
    
    /**
     * 切换自适应模式
     */
    public void toggleAdaptiveMode() {
        if (adaptiveMode) {
            disableAdaptiveMode();
        } else {
            enableAdaptiveMode();
        }
    }
    
    /**
     * 重新加载自适应配置
     */
    public void reload() {
        if (adaptiveMode) {
            adaptiveService.reload();
            LoggerUtil.info("AdaptiveCleanupIntegration", "自适应配置已重新加载");
        }
    }
    
    /**
     * 获取自适应服务状态
     */
    public AdaptiveCleanupService.ServiceStatus getServiceStatus() {
        return adaptiveService.getStatus();
    }
    
    /**
     * 判断是否处于自适应模式
     */
    public boolean isAdaptiveMode() {
        return adaptiveMode;
    }
    
    /**
     * 获取自适应清理服务
     */
    public AdaptiveCleanupService getAdaptiveService() {
        return adaptiveService;
    }
    
    /**
     * 命令处理器
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("adaptive")) {
            return false;
        }
        
        if (!sender.hasPermission("arissweeping.adaptive")) {
            sender.sendMessage("§c你没有权限使用此命令");
            return true;
        }
        
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "enable":
                enableAdaptiveMode();
                sender.sendMessage("§a自适应清理模式已启用");
                break;
                
            case "disable":
                disableAdaptiveMode();
                sender.sendMessage("§a自适应清理模式已禁用");
                break;
                
            case "toggle":
                toggleAdaptiveMode();
                sender.sendMessage(adaptiveMode ? "§a自适应清理模式已启用" : "§a自适应清理模式已禁用");
                break;
                
            case "status":
                showStatus(sender);
                break;
                
            case "reload":
                reload();
                sender.sendMessage("§a自适应配置已重新加载");
                break;
                
            default:
                showHelp(sender);
                break;
        }
        
        return true;
    }
    
    /**
     * 显示帮助信息
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage("§6=== 自适应清理命令帮助 ===");
        sender.sendMessage("§e/adaptive enable §7- 启用自适应清理模式");
        sender.sendMessage("§e/adaptive disable §7- 禁用自适应清理模式");
        sender.sendMessage("§e/adaptive toggle §7- 切换自适应清理模式");
        sender.sendMessage("§e/adaptive status §7- 查看自适应清理状态");
        sender.sendMessage("§e/adaptive reload §7- 重新加载自适应配置");
    }
    
    /**
     * 显示状态信息
     */
    private void showStatus(CommandSender sender) {
        sender.sendMessage("§6=== 自适应清理状态 ===");
        sender.sendMessage("§e模式状态: " + (adaptiveMode ? "§a启用" : "§c禁用"));
        
        if (adaptiveMode) {
            AdaptiveCleanupService.ServiceStatus status = getServiceStatus();
            sender.sendMessage("§e服务运行: " + (status.isRunning() ? "§a是" : "§c否"));
            sender.sendMessage("§e已处理实体: §f" + status.getEntitiesProcessed());
            sender.sendMessage("§e已清理实体: §f" + status.getEntitiesCleaned());
            sender.sendMessage("§e清理率: §f" + String.format("%.2f%%", status.getCleanupRate() * 100));
            sender.sendMessage("§e总处理时间: §f" + status.getTotalProcessingTime() + "ms");
            sender.sendMessage("§e当前批配置: §f" + status.getBatchConfig().toString());
        }
    }
    
    /**
     * 关闭集成服务
     */
    public void shutdown() {
        if (adaptiveMode) {
            disableAdaptiveMode();
        }
    }
}