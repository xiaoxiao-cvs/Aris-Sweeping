package com.xiaoxiao.arissweeping.util;

import com.xiaoxiao.arissweeping.ArisSweeping;
import com.xiaoxiao.arissweeping.util.LoggerUtil;
import me.lucko.spark.api.Spark;
import me.lucko.spark.api.SparkProvider;
import me.lucko.spark.api.statistic.StatisticWindow;
import me.lucko.spark.api.statistic.types.DoubleStatistic;
import me.lucko.spark.api.statistic.types.GenericStatistic;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * TPS监控器，用于监控服务器TPS并在低TPS时触发清理
 * 使用Spark API获取精确的性能数据
 */
public class TpsMonitor {
    private final ArisSweeping plugin;
    private final Spark spark;
    private BukkitTask monitorTask;
    private boolean lowTpsCleanupTriggered = false;
    
    public TpsMonitor(ArisSweeping plugin) {
        this.plugin = plugin;
        try {
            this.spark = SparkProvider.get();
            LoggerUtil.info("[TpsMonitor] 成功连接到Spark API");
        } catch (Exception e) {
            LoggerUtil.severe("[TpsMonitor] 无法连接到Spark API: " + e.getMessage());
            throw new RuntimeException("Spark API不可用", e);
        }
    }
    
    /**
     * 启动TPS监控
     */
    public void startMonitoring() {
        if (monitorTask != null) {
            monitorTask.cancel();
        }
        
        LoggerUtil.info("TPS monitoring activated.");
        
        monitorTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkTpsStatus();
            }
        }.runTaskTimer(plugin, 20L, 20L); // 每秒检查一次
    }
    
    /**
     * 停止TPS监控
     */
    public void stopMonitoring() {
        if (monitorTask != null) {
            monitorTask.cancel();
            monitorTask = null;
        }
        LoggerUtil.info("[TpsMonitor] TPS监控已停止");
    }
    
    /**
     * 检查TPS状态并触发相应操作
     */
    private void checkTpsStatus() {
        double currentTps = getCurrentTps();
        double currentMspt = getCurrentMspt();
        
        // 检查是否需要触发低TPS清理
        checkLowTps(currentTps);
        
        // 记录详细性能信息（调试模式）
        if (plugin.getModConfig().isDebugMode()) {
            LoggerUtil.info(String.format(
                "[TpsMonitor] TPS: %.2f, MSPT: %.2f", 
                currentTps, currentMspt
            ));
        }
    }
    
    /**
     * 检查低TPS并触发相应操作
     */
    private void checkLowTps(double tps) {
        double threshold = plugin.getModConfig().getLowTpsThreshold();
        
        if (threshold <= 0) {
            LoggerUtil.warning("[TpsMonitor] 无效的低TPS阈值: " + threshold);
            return;
        }
        
        if (tps < threshold && !lowTpsCleanupTriggered) {
            triggerLowTpsCleanup(tps);
        } else if (tps >= threshold) {
            lowTpsCleanupTriggered = false; // 重置标志
        }
    }
    
    /**
     * 触发低TPS清理
     */
    private void triggerLowTpsCleanup(double tps) {
        lowTpsCleanupTriggered = true;
        
        // 向有权限的玩家发送警告消息
        String warningMessage = ChatColor.RED + "[警告] " + ChatColor.YELLOW + 
            String.format("服务器TPS过低 (%.1f)，正在执行紧急清理...", tps);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != null && player.isOnline() && player.hasPermission("arissweeping.lowtps.notify")) {
                player.sendMessage(warningMessage);
            }
        }
        
        LoggerUtil.warning(String.format("Low TPS detected (%.1f), triggering emergency cleanup", tps));
        
        // 触发紧急清理
        plugin.getCleanupHandler().performEmergencyCleanup();
        
        // 设置冷却时间，避免频繁触发
        new BukkitRunnable() {
            @Override
            public void run() {
                lowTpsCleanupTriggered = false;
            }
        }.runTaskLater(plugin, 1200L); // 60秒冷却
    }
    
    /**
     * 获取当前TPS（使用Spark API）
     * @return 当前TPS值
     */
    public double getCurrentTps() {
        DoubleStatistic<StatisticWindow.TicksPerSecond> tpsStatistic = spark.tps();
        if (tpsStatistic != null) {
            // 获取最近10秒的TPS
            return tpsStatistic.poll(StatisticWindow.TicksPerSecond.SECONDS_10);
        }
        return 20.0; // 默认返回20
    }
    
    /**
     * 获取当前MSPT（毫秒每tick）
     * @return 当前MSPT值
     */
    public double getCurrentMspt() {
        GenericStatistic<DoubleAverageInfo, StatisticWindow.MillisPerTick> msptStatistic = spark.mspt();
        if (msptStatistic != null) {
            DoubleAverageInfo msptInfo = msptStatistic.poll(StatisticWindow.MillisPerTick.SECONDS_10);
            if (msptInfo != null) {
                return msptInfo.mean();
            }
        }
        return 50.0; // 默认返回50ms
    }
    
    /**
     * 获取TPS状态描述
     */
    public String getTpsStatus() {
        double tps = getCurrentTps();
        
        if (tps >= 19.5) {
            return ChatColor.GREEN + "优秀";
        } else if (tps >= 18.0) {
            return ChatColor.YELLOW + "良好";
        } else if (tps >= 15.0) {
            return ChatColor.GOLD + "一般";
        } else {
            return ChatColor.RED + "较差";
        }
    }
}