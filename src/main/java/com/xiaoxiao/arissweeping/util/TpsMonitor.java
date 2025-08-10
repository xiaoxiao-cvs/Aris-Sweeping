package com.xiaoxiao.arissweeping.util;

import com.xiaoxiao.arissweeping.ArisSweeping;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * TPS监控器，用于监控服务器TPS并在低TPS时触发清理
 */
public class TpsMonitor {
    private final ArisSweeping plugin;
    private BukkitTask monitorTask;
    private final List<Integer> tickList = new ArrayList<>();
    private boolean lowTpsCleanupTriggered = false;
    
    public TpsMonitor(ArisSweeping plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 启动TPS监控
     */
    public void startMonitoring() {
        if (monitorTask != null) {
            monitorTask.cancel();
        }
        
        plugin.getLogger().info("TPS monitoring activated.");
        
        monitorTask = new BukkitRunnable() {
            @Override
            public void run() {
                measureTps();
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
    }
    
    /**
     * 测量TPS
     */
    private void measureTps() {
        long now = System.currentTimeMillis();
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                ticks++;
                if (now + 1000 <= System.currentTimeMillis()) {
                    cancel();
                    processTps(ticks);
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    /**
     * 处理TPS数据
     */
    private void processTps(int tps) {
        tickList.add(tps);
        
        // 保持最近10次的TPS记录
        if (tickList.size() > 10) {
            tickList.remove(0);
        } else {
            return; // 数据不足，等待更多数据
        }
        
        // 计算平均TPS
        int sum = 0;
        for (int tick : tickList) {
            sum += tick;
        }
        double averageTps = (double) sum / tickList.size();
        
        // 检查是否需要触发低TPS清理
        checkLowTps(averageTps);
    }
    
    /**
     * 检查低TPS并触发相应操作
     */
    private void checkLowTps(double tps) {
        double threshold = plugin.getModConfig().getLowTpsThreshold();
        
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
            if (player.hasPermission("arissweeping.lowtps.notify")) {
                player.sendMessage(warningMessage);
            }
        }
        
        plugin.getLogger().warning(String.format("Low TPS detected (%.1f), triggering emergency cleanup", tps));
        
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
     * 获取当前TPS
     */
    public double getCurrentTps() {
        if (tickList.isEmpty()) {
            return 20.0; // 默认值
        }
        
        int sum = 0;
        for (int tick : tickList) {
            sum += tick;
        }
        return (double) sum / tickList.size();
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