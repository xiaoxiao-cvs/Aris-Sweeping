package com.xiaoxiao.arissweeping.service;

import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.CleanupStats;
import com.xiaoxiao.arissweeping.util.LoggerUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

/**
 * 清理通知服务 - 负责处理所有清理相关的通知和消息广播
 * 从EntityCleanupHandler中分离出来，遵循单一职责原则
 */
public class CleanupNotificationService {
    private final ModConfig config;
    
    public CleanupNotificationService(ModConfig config) {
        this.config = config;
    }
    
    /**
     * 广播常规清理消息
     */
    public void broadcastCleanupMessage(CleanupStats stats) {
        try {
            // 检查配置，确保广播是启用的
            if (!config.isBroadcastCleanup()) {
                LoggerUtil.info("清理消息广播跳过 - 广播已禁用");
                return;
            }
            
            LoggerUtil.info("广播清理消息 - 总清理数: " + stats.getTotalCleaned());
            
            String message = buildCleanupMessage(stats);
            
            // 记录日志（移除颜色代码）
            String logMessage = message.replaceAll("§[0-9a-fk-or]", "");
            LoggerUtil.info("清理消息: " + logMessage);
            
            // 向所有在线玩家广播消息
            Bukkit.broadcastMessage(message);
            LoggerUtil.info("清理消息已成功广播给 " + Bukkit.getOnlinePlayers().size() + " 名玩家");
            
        } catch (Exception e) {
            LoggerUtil.severe("广播清理消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 构建清理消息
     */
    private String buildCleanupMessage(CleanupStats stats) {
        if (config.isShowCleanupStats() && stats.getTotalCleaned() > 0) {
            return buildDetailedCleanupMessage(stats);
        } else {
            return buildSimpleCleanupMessage(stats);
        }
    }
    
    /**
     * 构建详细清理消息
     */
    private String buildDetailedCleanupMessage(CleanupStats stats) {
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
        
        return detailBuilder.toString();
    }
    
    /**
     * 构建简单清理消息
     */
    private String buildSimpleCleanupMessage(CleanupStats stats) {
        if (stats.getTotalCleaned() > 0) {
            return ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝扫掉了 " + 
                   ChatColor.RED + stats.getTotalCleaned() + ChatColor.WHITE + " 个实体呢~ 老师~";
        } else {
            return ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝完成了清理检查，一切都很干净呢~ 老师~";
        }
    }
    
    /**
     * 广播紧急清理通知
     */
    public void broadcastEmergencyCleanupNotification(CleanupStats stats, long duration) {
        try {
            if (!config.isBroadcastCleanup()) {
                LoggerUtil.info("紧急清理通知跳过 - 广播已禁用");
                return;
            }
            
            String message = buildEmergencyCleanupMessage(stats, duration);
            
            Bukkit.broadcastMessage(message);
            LoggerUtil.info("紧急清理通知已发送给 " + Bukkit.getOnlinePlayers().size() + " 名玩家");
            
        } catch (Exception e) {
            LoggerUtil.severe("发送紧急清理通知失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 构建紧急清理消息
     */
    private String buildEmergencyCleanupMessage(CleanupStats stats, long duration) {
        if (stats.getTotalCleaned() > 0) {
            if (config.isShowCleanupStats()) {
                return buildDetailedEmergencyMessage(stats, duration);
            } else {
                return buildSimpleEmergencyMessage(stats);
            }
        } else {
            return ChatColor.RED + "[紧急清理] " + ChatColor.WHITE + "未发现需要清理的实体，TPS问题可能由其他原因引起";
        }
    }
    
    /**
     * 构建详细紧急清理消息
     */
    private String buildDetailedEmergencyMessage(CleanupStats stats, long duration) {
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
        
        return detailMessage.toString();
    }
    
    /**
     * 构建简单紧急清理消息
     */
    private String buildSimpleEmergencyMessage(CleanupStats stats) {
        return ChatColor.RED + "[紧急清理] " + ChatColor.WHITE + "爱丽丝紧急清理了 " + 
               ChatColor.YELLOW + stats.getTotalCleaned() + ChatColor.WHITE + " 个实体，服务器性能已恢复";
    }
    
    /**
     * 发送紧急清理开始通知
     */
    public void sendEmergencyCleanupStartNotification(double currentTps, double threshold) {
        try {
            if (!config.isBroadcastCleanup()) {
                return;
            }
            
            String startMessage = ChatColor.RED + "[紧急清理] " + ChatColor.YELLOW + 
                                "检测到服务器TPS过低 (" + String.format("%.2f", currentTps) + 
                                "/" + threshold + ")，爱丽丝开始紧急清理实体...";
            
            Bukkit.broadcastMessage(startMessage);
            LoggerUtil.info("紧急清理开始通知已发送");
            
        } catch (Exception e) {
            LoggerUtil.warning("发送紧急清理开始通知失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送紧急清理错误通知
     */
    public void sendEmergencyCleanupErrorNotification() {
        try {
            if (!config.isBroadcastCleanup()) {
                return;
            }
            
            String errorMessage = ChatColor.RED + "[紧急清理] " + ChatColor.WHITE + "清理过程中发生错误，请查看控制台日志";
            Bukkit.broadcastMessage(errorMessage);
            LoggerUtil.info("紧急清理错误通知已发送");
            
        } catch (Exception e) {
            LoggerUtil.warning("发送紧急清理错误通知失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送倒计时消息
     */
    public void sendCountdownMessage(int timeLeft) {
        try {
            if (!config.isBroadcastCleanup()) {
                return;
            }
            
            String message = ChatColor.YELLOW + "[清理提醒] " + ChatColor.WHITE + 
                           "爱丽丝将在 " + ChatColor.RED + timeLeft + ChatColor.WHITE + " 秒后开始清理实体";
            
            Bukkit.broadcastMessage(message);
            LoggerUtil.info("倒计时消息已发送: " + timeLeft + " 秒");
            
        } catch (Exception e) {
            LoggerUtil.warning("发送倒计时消息时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 发送密度警告通知
     */
    public void sendDensityWarningNotification(int worldsWithHighDensity, List<String> highDensityWorlds, int totalEntities) {
        try {
            if (!config.isBroadcastCleanup()) {
                return;
            }
            
            String message = ChatColor.YELLOW + "[密度警告] " + ChatColor.WHITE + 
                           "检测到 " + ChatColor.RED + worldsWithHighDensity + ChatColor.WHITE + 
                           " 个世界实体密度过高 (总计: " + totalEntities + " 个实体)";
            
            Bukkit.broadcastMessage(message);
            
            // 如果启用详细信息，显示具体世界
            if (config.isShowCleanupStats() && highDensityWorlds.size() <= 5) {
                String detailMessage = ChatColor.GRAY + "高密度世界: " + 
                                     String.join(", ", highDensityWorlds);
                Bukkit.broadcastMessage(detailMessage);
            }
            
            LoggerUtil.info("密度警告通知已发送");
            
        } catch (Exception e) {
            LoggerUtil.severe("发送密度警告通知时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 发送配置错误通知
     */
    public void sendConfigErrorNotification(String errorMessage) {
        try {
            if (!config.isBroadcastCleanup()) {
                return;
            }
            
            String message = ChatColor.RED + "[配置错误] " + ChatColor.WHITE + errorMessage;
            
            // 向管理员发送通知
            Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission("arissweeping.admin"))
                .forEach(player -> player.sendMessage(message));
            
            LoggerUtil.warning("配置错误通知已发送: " + errorMessage);
            
        } catch (Exception e) {
            LoggerUtil.severe("发送配置错误通知时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 移除消息中的颜色代码（用于日志记录）
     */
    public String removeColorCodes(String message) {
        if (message == null) {
            return null;
        }
        return message.replaceAll("§[0-9a-fk-or]", "");
    }
    
    /**
     * 检查是否启用广播
     */
    public boolean isBroadcastEnabled() {
        return config.isBroadcastCleanup();
    }
    
    /**
     * 检查是否显示详细统计
     */
    public boolean isShowDetailedStats() {
        return config.isShowCleanupStats();
    }
}