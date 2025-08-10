package com.xiaoxiao.arissweeping.handler;

import com.xiaoxiao.arissweeping.config.ModConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LivestockDensityHandler {
    private final Plugin plugin;
    private final ModConfig config;
    private BukkitTask densityCheckTask;
    private BukkitTask warningTask;
    private final Map<String, List<String>> pendingCleanups = new HashMap<>();
    
    public LivestockDensityHandler(Plugin plugin, ModConfig config) {
        this.plugin = plugin;
        this.config = config;
    }
    
    public void startDensityCheck() {
        if (!config.isLivestockDensityCheckEnabled()) {
            return;
        }
        
        stopDensityCheck();
        
        // 每5分钟检查一次密度
        densityCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkLivestockDensity();
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 6000L); // 5分钟 = 6000 ticks
    }
    
    public void stopDensityCheck() {
        if (densityCheckTask != null) {
            densityCheckTask.cancel();
            densityCheckTask = null;
        }
        if (warningTask != null) {
            warningTask.cancel();
            warningTask = null;
        }
    }
    
    private void checkLivestockDensity() {
        if (!config.isPluginEnabled() || !config.isLivestockDensityCheckEnabled()) {
            return;
        }
        
        Map<String, List<String>> violations = new HashMap<>();
        
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                int animalCount = 0;
                List<String> animalTypes = new ArrayList<>();
                
                for (Entity entity : chunk.getEntities()) {
                    if (entity instanceof Animals) {
                        animalCount++;
                        String type = entity.getType().name();
                        if (!animalTypes.contains(type)) {
                            animalTypes.add(type);
                        }
                    }
                }
                
                if (animalCount > config.getMaxAnimalsPerChunk()) {
                    String location = String.format("%s (%d, %d)", 
                        world.getName(), chunk.getX() * 16, chunk.getZ() * 16);
                    String info = String.format("动物数量: %d (超出%d), 类型: %s", 
                        animalCount, animalCount - config.getMaxAnimalsPerChunk(), 
                        String.join(", ", animalTypes));
                    
                    violations.computeIfAbsent(location, k -> new ArrayList<>()).add(info);
                }
            }
        }
        
        if (!violations.isEmpty() && config.isWarningEnabled()) {
            sendWarningMessage(violations);
            scheduleCleanup(violations);
        }
    }
    
    private void sendWarningMessage(Map<String, List<String>> violations) {
        StringBuilder message = new StringBuilder();
        message.append(ChatColor.RED).append("[邦邦卡邦！] ").append(ChatColor.YELLOW)
               .append("⚠️ 爱丽丝发现了畜牧业密度超标！\n");
        message.append(ChatColor.WHITE).append("以下区域将在 ").append(ChatColor.RED)
               .append(config.getWarningTime()).append(ChatColor.WHITE).append(" 分钟后进行清理：\n");
        
        for (Map.Entry<String, List<String>> entry : violations.entrySet()) {
            message.append(ChatColor.GOLD).append("📍 ").append(entry.getKey()).append(":\n");
            for (String info : entry.getValue()) {
                message.append(ChatColor.WHITE).append("  • ").append(info).append("\n");
            }
        }
        
        message.append(ChatColor.YELLOW).append("请老师们尽快处理，避免动物损失！");
        
        Bukkit.broadcastMessage(message.toString());
    }
    
    private void scheduleCleanup(Map<String, List<String>> violations) {
        pendingCleanups.clear();
        pendingCleanups.putAll(violations);
        
        // 取消之前的清理任务
        if (warningTask != null) {
            warningTask.cancel();
        }
        
        // 安排延迟清理
        warningTask = new BukkitRunnable() {
            @Override
            public void run() {
                performLivestockCleanup();
            }
        }.runTaskLater(plugin, config.getWarningTime() * 60 * 20L); // 转换为ticks
    }
    
    private void performLivestockCleanup() {
        if (pendingCleanups.isEmpty()) {
            return;
        }
        
        int totalCleaned = 0;
        
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                String location = String.format("%s (%d, %d)", 
                    world.getName(), chunk.getX() * 16, chunk.getZ() * 16);
                
                if (pendingCleanups.containsKey(location)) {
                    List<Animals> animals = new ArrayList<>();
                    
                    for (Entity entity : chunk.getEntities()) {
                        if (entity instanceof Animals) {
                            animals.add((Animals) entity);
                        }
                    }
                    
                    // 只清理超出部分的动物
                    int excess = animals.size() - config.getMaxAnimalsPerChunk();
                    if (excess > 0) {
                        for (int i = 0; i < excess && i < animals.size(); i++) {
                            Animals animal = animals.get(i);
                            if (animal.getCustomName() == null) { // 不清理有名字的动物
                                animal.remove();
                                totalCleaned++;
                            }
                        }
                    }
                }
            }
        }
        
        if (totalCleaned > 0) {
            String message = String.format(
                ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + 
                "爱丽丝完成了畜牧业密度管理，清理了 " + ChatColor.RED + "%d" + 
                ChatColor.WHITE + " 只超标动物~",
                totalCleaned
            );
            Bukkit.broadcastMessage(message);
        }
        
        pendingCleanups.clear();
    }
    
    public void restartDensityCheck() {
        startDensityCheck();
    }
}