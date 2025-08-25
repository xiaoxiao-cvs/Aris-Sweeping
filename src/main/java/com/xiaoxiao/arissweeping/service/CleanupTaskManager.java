package com.xiaoxiao.arissweeping.service;

import com.xiaoxiao.arissweeping.ArisSweeping;
import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.LoggerUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * 清理任务管理器 - 负责管理定时清理任务和倒计时功能
 * 从EntityCleanupHandler中分离出来，遵循单一职责原则
 */
public class CleanupTaskManager {
    private final ArisSweeping plugin;
    private final ModConfig config;
    private final Runnable cleanupCallback;
    
    private BukkitTask cleanupTask;
    private BukkitTask countdownTask;
    
    public CleanupTaskManager(ArisSweeping plugin, Runnable cleanupCallback) {
        this.plugin = plugin;
        this.config = plugin.getModConfig();
        this.cleanupCallback = cleanupCallback;
    }
    
    /**
     * 启动清理任务
     */
    public void startCleanupTask() {
        try {
            // 停止现有任务
            stopCleanupTask();
            
            if (!config.isPluginEnabled()) {
                LoggerUtil.info("清理任务未启动 - 插件已禁用");
                return;
            }
            
            int intervalSeconds = config.getCleanupInterval();
            if (intervalSeconds <= 0) {
                LoggerUtil.warning("清理任务未启动 - 清理间隔无效: " + intervalSeconds);
                return;
            }
            
            // 启动定时清理任务
            cleanupTask = new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        if (!config.isPluginEnabled()) {
                            LoggerUtil.info("清理任务跳过 - 插件已禁用");
                            return;
                        }
                        
                        // 检查是否启用倒计时
                        if (config.isCountdownEnabled()) {
                            startCleanupCountdown();
                        } else {
                            // 直接执行清理
                            cleanupCallback.run();
                        }
                    } catch (Exception e) {
                        LoggerUtil.severe("清理任务执行时发生错误: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }.runTaskTimer(plugin, 20L * intervalSeconds, 20L * intervalSeconds);
            
            LoggerUtil.info("清理任务已启动，间隔: " + intervalSeconds + " 秒");
            
        } catch (Exception e) {
            LoggerUtil.severe("启动清理任务时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 启动清理倒计时
     */
    private void startCleanupCountdown() {
        try {
            if (!config.isCountdownEnabled()) {
                cleanupCallback.run();
                return;
            }
            
            int countdownTime = config.getCountdownTime();
            if (countdownTime <= 0) {
                LoggerUtil.warning("倒计时时间无效，直接执行清理: " + countdownTime);
                cleanupCallback.run();
                return;
            }
            
            LoggerUtil.info("开始清理倒计时: " + countdownTime + " 秒");
            
            // 停止现有倒计时任务
            if (countdownTask != null && !countdownTask.isCancelled()) {
                countdownTask.cancel();
            }
            
            List<Integer> warningTimes = getWarningTimes(countdownTime);
            
            countdownTask = new BukkitRunnable() {
                int timeLeft = countdownTime;
                
                @Override
                public void run() {
                    try {
                        if (!config.isPluginEnabled()) {
                            LoggerUtil.info("倒计时取消 - 插件已禁用");
                            cancel();
                            return;
                        }
                        
                        if (timeLeft <= 0) {
                            // 倒计时结束，执行清理
                            cleanupCallback.run();
                            cancel();
                            return;
                        }
                        
                        // 发送警告消息
                        if (warningTimes.contains(timeLeft)) {
                            sendCountdownMessage(timeLeft);
                        }
                        
                        timeLeft--;
                    } catch (Exception e) {
                        LoggerUtil.severe("倒计时任务执行时发生错误: " + e.getMessage());
                        e.printStackTrace();
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L); // 每秒执行一次
            
        } catch (Exception e) {
            LoggerUtil.severe("启动清理倒计时时发生错误: " + e.getMessage());
            e.printStackTrace();
            // 出错时直接执行清理
            cleanupCallback.run();
        }
    }
    
    /**
     * 获取警告时间点
     */
    private List<Integer> getWarningTimes(int countdownTime) {
        List<Integer> warningTimes = new ArrayList<>();
        
        // 标准警告时间点
        int[] standardWarnings = {60, 30, 15, 10, 5, 3, 2, 1};
        
        for (int warning : standardWarnings) {
            if (warning < countdownTime) {
                warningTimes.add(warning);
            }
        }
        
        // 如果倒计时时间很长，添加更多警告点
        if (countdownTime > 120) {
            warningTimes.add(120);
        }
        if (countdownTime > 300) {
            warningTimes.add(300);
        }
        
        return warningTimes;
    }
    
    /**
     * 发送倒计时消息
     */
    private void sendCountdownMessage(int timeLeft) {
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
     * 停止清理任务
     */
    public void stopCleanupTask() {
        try {
            if (cleanupTask != null && !cleanupTask.isCancelled()) {
                cleanupTask.cancel();
                LoggerUtil.info("清理任务已停止");
            }
            
            if (countdownTask != null && !countdownTask.isCancelled()) {
                countdownTask.cancel();
                LoggerUtil.info("倒计时任务已停止");
            }
        } catch (Exception e) {
            LoggerUtil.severe("停止清理任务时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 重启清理任务
     */
    public void restartCleanupTask() {
        stopCleanupTask();
        startCleanupTask();
        LoggerUtil.info("清理任务已重启，新间隔: " + config.getCleanupInterval() + " 秒");
    }
    
    /**
     * 获取任务状态信息
     */
    public String getTaskStatusInfo() {
        StringBuilder status = new StringBuilder();
        status.append("清理任务管理器状态:\n");
        
        // 清理任务状态
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            status.append("- 清理任务: 运行中\n");
        } else {
            status.append("- 清理任务: 已停止\n");
        }
        
        // 倒计时任务状态
        if (countdownTask != null && !countdownTask.isCancelled()) {
            status.append("- 倒计时任务: 运行中\n");
        } else {
            status.append("- 倒计时任务: 已停止\n");
        }
        
        // 配置信息
        status.append("- 清理间隔: ").append(config.getCleanupInterval()).append(" 秒\n");
        status.append("- 倒计时启用: ").append(config.isCountdownEnabled() ? "是" : "否");
        if (config.isCountdownEnabled()) {
            status.append(" (").append(config.getCountdownTime()).append(" 秒)");
        }
        
        return status.toString();
    }
    
    /**
     * 检查任务是否正在运行
     */
    public boolean isTaskRunning() {
        return cleanupTask != null && !cleanupTask.isCancelled();
    }
    
    /**
     * 检查倒计时是否正在运行
     */
    public boolean isCountdownRunning() {
        return countdownTask != null && !countdownTask.isCancelled();
    }
    
    /**
     * 执行手动清理
     */
    public void performManualCleanup() {
        if (cleanupCallback != null) {
            LoggerUtil.info("CleanupTaskManager", "执行手动清理");
            cleanupCallback.run();
        } else {
            LoggerUtil.warning("CleanupTaskManager", "无法执行手动清理：清理回调为空");
        }
    }
    
    /**
     * 关闭任务管理器
     */
    public void shutdown() {
        stopCleanupTask();
        LoggerUtil.info("清理任务管理器已关闭");
    }
}