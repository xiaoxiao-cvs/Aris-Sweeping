package com.xiaoxiao.arissweeping.util;

import com.xiaoxiao.arissweeping.ArisSweeping;
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
            plugin.getLogger().info("[TpsMonitor] 成功连接到Spark API");
        } catch (Exception e) {
            plugin.getLogger().severe("[TpsMonitor] 无法连接到Spark API: " + e.getMessage());
            throw new RuntimeException("Spark API不可用", e);
        }
    }
    
    /**
     * 启动TPS监控
     */
    public void startMonitoring() {
        try {
            if (monitorTask != null) {
                try {
                    monitorTask.cancel();
                } catch (Exception e) {
                    plugin.getLogger().warning("[TpsMonitor] 取消现有监控任务时发生异常: " + e.getMessage());
                }
            }
            
            plugin.getLogger().info("TPS monitoring activated.");
            
            monitorTask = new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        checkTpsStatus();
                    } catch (Exception e) {
                        plugin.getLogger().warning("[TpsMonitor] TPS检查时发生异常: " + e.getMessage());
                    }
                }
            }.runTaskTimer(plugin, 20L, 20L); // 每秒检查一次
        } catch (Exception e) {
            plugin.getLogger().severe("[TpsMonitor] 启动TPS监控时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 停止TPS监控
     */
    public void stopMonitoring() {
        try {
            if (monitorTask != null) {
                try {
                    monitorTask.cancel();
                } catch (Exception e) {
                    plugin.getLogger().warning("[TpsMonitor] 取消监控任务时发生异常: " + e.getMessage());
                }
                monitorTask = null;
            }
            plugin.getLogger().info("[TpsMonitor] TPS监控已停止");
        } catch (Exception e) {
            plugin.getLogger().warning("[TpsMonitor] 停止TPS监控时发生异常: " + e.getMessage());
        }
    }
    
    /**
     * 检查TPS状态并触发相应操作
     */
    private void checkTpsStatus() {
        try {
            double currentTps = getCurrentTps();
            double currentMspt = getCurrentMspt();
            
            // 检查是否需要触发低TPS清理
            checkLowTps(currentTps);
            
            // 记录详细性能信息（调试模式）
            if (plugin.getModConfig().isDebugMode()) {
                plugin.getLogger().info(String.format(
                    "[TpsMonitor] TPS: %.2f, MSPT: %.2f", 
                    currentTps, currentMspt
                ));
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("[TpsMonitor] 检查TPS状态时发生异常: " + e.getMessage());
        }
    }
    
    /**
     * 检查低TPS并触发相应操作
     */
    private void checkLowTps(double tps) {
        try {
            double threshold = plugin.getModConfig().getLowTpsThreshold();
            
            if (threshold <= 0) {
                plugin.getLogger().warning("[TpsMonitor] 无效的低TPS阈值: " + threshold);
                return;
            }
            
            if (tps < threshold && !lowTpsCleanupTriggered) {
                triggerLowTpsCleanup(tps);
            } else if (tps >= threshold) {
                lowTpsCleanupTriggered = false; // 重置标志
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[TpsMonitor] 检查低TPS时发生异常: " + e.getMessage());
        }
    }
    
    /**
     * 触发低TPS清理
     */
    private void triggerLowTpsCleanup(double tps) {
        try {
            lowTpsCleanupTriggered = true;
            
            // 向有权限的玩家发送警告消息
            String warningMessage = ChatColor.RED + "[警告] " + ChatColor.YELLOW + 
                String.format("服务器TPS过低 (%.1f)，正在执行紧急清理...", tps);
            
            try {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    try {
                        if (player != null && player.isOnline() && player.hasPermission("arissweeping.lowtps.notify")) {
                            player.sendMessage(warningMessage);
                        }
                    } catch (Exception playerException) {
                        plugin.getLogger().warning("[TpsMonitor] 向玩家发送低TPS警告时发生异常: " + playerException.getMessage());
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[TpsMonitor] 获取在线玩家列表时发生异常: " + e.getMessage());
            }
            
            plugin.getLogger().warning(String.format("Low TPS detected (%.1f), triggering emergency cleanup", tps));
            
            // 触发紧急清理
            try {
                plugin.getCleanupHandler().performEmergencyCleanup();
            } catch (Exception e) {
                plugin.getLogger().severe("[TpsMonitor] 执行紧急清理时发生异常: " + e.getMessage());
                e.printStackTrace();
            }
            
            // 设置冷却时间，避免频繁触发
            try {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        lowTpsCleanupTriggered = false;
                    }
                }.runTaskLater(plugin, 1200L); // 60秒冷却
            } catch (Exception e) {
                plugin.getLogger().warning("[TpsMonitor] 设置冷却任务时发生异常: " + e.getMessage());
                // 手动重置标志以防任务失败
                lowTpsCleanupTriggered = false;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[TpsMonitor] 触发低TPS清理时发生异常: " + e.getMessage());
            e.printStackTrace();
            lowTpsCleanupTriggered = false; // 重置标志
        }
    }
    
    /**
     * 获取当前TPS（使用Spark API）
     * @return 当前TPS值
     */
    public double getCurrentTps() {
        try {
            DoubleStatistic<StatisticWindow.TicksPerSecond> tpsStatistic = spark.tps();
            if (tpsStatistic != null) {
                // 获取最近10秒的TPS
                return tpsStatistic.poll(StatisticWindow.TicksPerSecond.SECONDS_10);
            }
            return 20.0; // 默认返回20
        } catch (Exception e) {
            plugin.getLogger().warning("[TpsMonitor] 获取当前TPS时发生异常: " + e.getMessage());
            return 20.0;
        }
    }
    
    /**
     * 获取当前MSPT（毫秒每tick）
     * @return 当前MSPT值
     */
    public double getCurrentMspt() {
        try {
            GenericStatistic<DoubleAverageInfo, StatisticWindow.MillisPerTick> msptStatistic = spark.mspt();
            if (msptStatistic != null) {
                DoubleAverageInfo msptInfo = msptStatistic.poll(StatisticWindow.MillisPerTick.SECONDS_10);
                if (msptInfo != null) {
                    return msptInfo.mean();
                }
            }
            return 50.0; // 默认返回50ms
        } catch (Exception e) {
            plugin.getLogger().warning("[TpsMonitor] 获取当前MSPT时发生异常: " + e.getMessage());
            return 50.0;
        }
    }
    
    /**
     * 获取TPS状态描述
     */
    public String getTpsStatus() {
        try {
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
        } catch (Exception e) {
            plugin.getLogger().warning("[TpsMonitor] 获取TPS状态时发生异常: " + e.getMessage());
            return "未知";
        }
    }
}