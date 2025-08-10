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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 畜牧业密度管理处理器
 * 负责监控和管理各区块中的动物密度，防止过度繁殖影响服务器性能
 */
public class LivestockDensityHandler {
    private final Plugin plugin;
    private final ModConfig config;
    private BukkitTask densityCheckTask;
    private BukkitTask warningTask;
    private final Map<String, ViolationInfo> pendingCleanups = new HashMap<>();
    private final AtomicBoolean isCleanupRunning = new AtomicBoolean(false);
    
    /**
     * 违规信息类，用于存储区块违规详情
     */
    private static class ViolationInfo {
        final int animalCount;
        final int excessCount;
        final List<String> animalTypes;
        final long detectionTime;
        
        ViolationInfo(int animalCount, int excessCount, List<String> animalTypes) {
            this.animalCount = animalCount;
            this.excessCount = excessCount;
            this.animalTypes = new ArrayList<>(animalTypes);
            this.detectionTime = System.currentTimeMillis();
        }
    }
    
    public LivestockDensityHandler(Plugin plugin, ModConfig config) {
        this.plugin = plugin;
        this.config = config;
    }
    
    /**
     * 启动密度检查任务
     */
    public void startDensityCheck() {
        // 检查全局开关和功能开关
        if (!config.isPluginEnabled() || !config.isLivestockDensityCheckEnabled()) {
            plugin.getLogger().info("[LivestockDensityHandler] 密度检查功能已禁用，跳过启动");
            return;
        }
        
        try {
            stopDensityCheck();
            
            // 验证配置参数
            if (config.getMaxAnimalsPerChunk() <= 0) {
                plugin.getLogger().warning("[LivestockDensityHandler] 无效的动物密度阈值配置: " + config.getMaxAnimalsPerChunk());
                return;
            }
            
            // 每5分钟检查一次密度
            densityCheckTask = new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        checkLivestockDensity();
                    } catch (Exception e) {
                        plugin.getLogger().severe("[LivestockDensityHandler] 密度检查过程中发生异常: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }.runTaskTimerAsynchronously(plugin, 0L, 6000L); // 5分钟 = 6000 ticks
            
            plugin.getLogger().info("[LivestockDensityHandler] 密度检查任务已启动，检查间隔: 5分钟");
            
        } catch (Exception e) {
            plugin.getLogger().severe("[LivestockDensityHandler] 启动密度检查任务失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 停止密度检查任务
     */
    public void stopDensityCheck() {
        try {
            if (densityCheckTask != null) {
                densityCheckTask.cancel();
                densityCheckTask = null;
                plugin.getLogger().info("[LivestockDensityHandler] 密度检查任务已停止");
            }
            
            if (warningTask != null) {
                warningTask.cancel();
                warningTask = null;
                plugin.getLogger().info("[LivestockDensityHandler] 警告清理任务已停止");
            }
            
            // 清理待处理的清理任务
            pendingCleanups.clear();
            isCleanupRunning.set(false);
            
        } catch (Exception e) {
            plugin.getLogger().severe("[LivestockDensityHandler] 停止密度检查任务时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 检查畜牧业密度
     */
    private void checkLivestockDensity() {
        // 双重检查全局开关
        if (!config.isPluginEnabled() || !config.isLivestockDensityCheckEnabled()) {
            return;
        }
        
        // 防止重复执行清理
        if (isCleanupRunning.get()) {
            plugin.getLogger().info("[LivestockDensityHandler] 清理任务正在进行中，跳过本次检查");
            return;
        }
        
        Map<String, ViolationInfo> violations = new HashMap<>();
        int totalChunksChecked = 0;
        int totalAnimalsFound = 0;
        
        try {
            for (World world : Bukkit.getWorlds()) {
                if (world == null) continue;
                
                for (Chunk chunk : world.getLoadedChunks()) {
                    if (chunk == null) continue;
                    
                    totalChunksChecked++;
                    int animalCount = 0;
                    Map<String, Integer> animalTypeCount = new HashMap<>();
                    
                    try {
                        for (Entity entity : chunk.getEntities()) {
                            if (entity instanceof Animals) {
                                animalCount++;
                                totalAnimalsFound++;
                                String type = entity.getType().name();
                                animalTypeCount.put(type, animalTypeCount.getOrDefault(type, 0) + 1);
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("[LivestockDensityHandler] 检查区块 " + 
                            chunk.getX() + "," + chunk.getZ() + " 时发生异常: " + e.getMessage());
                        continue;
                    }
                    
                    if (animalCount > config.getMaxAnimalsPerChunk()) {
                        String location = String.format("%s (%d, %d)", 
                            world.getName(), chunk.getX() * 16, chunk.getZ() * 16);
                        
                        List<String> animalTypes = new ArrayList<>();
                        for (Map.Entry<String, Integer> entry : animalTypeCount.entrySet()) {
                            animalTypes.add(entry.getKey() + "(" + entry.getValue() + ")");
                        }
                        
                        int excessCount = animalCount - config.getMaxAnimalsPerChunk();
                        violations.put(location, new ViolationInfo(animalCount, excessCount, animalTypes));
                    }
                }
            }
            
            // 记录检查统计信息
            if (config.isDebugMode()) {
                plugin.getLogger().info(String.format(
                    "[LivestockDensityHandler] 密度检查完成 - 检查区块: %d, 发现动物: %d, 违规区块: %d",
                    totalChunksChecked, totalAnimalsFound, violations.size()
                ));
            }
            
            if (!violations.isEmpty() && config.isWarningEnabled()) {
                sendWarningMessage(violations);
                scheduleCleanup(violations);
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("[LivestockDensityHandler] 密度检查过程中发生严重异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 发送警告消息
     */
    private void sendWarningMessage(Map<String, ViolationInfo> violations) {
        try {
            StringBuilder message = new StringBuilder();
            message.append(ChatColor.RED).append("[邦邦卡邦！] ").append(ChatColor.YELLOW)
                   .append("⚠️ 爱丽丝发现了畜牧业密度超标！\n");
            message.append(ChatColor.WHITE).append("以下区域将在 ").append(ChatColor.RED)
                   .append(config.getWarningTime()).append(ChatColor.WHITE).append(" 分钟后进行清理：\n");
            
            int totalExcess = 0;
            for (Map.Entry<String, ViolationInfo> entry : violations.entrySet()) {
                ViolationInfo info = entry.getValue();
                totalExcess += info.excessCount;
                
                message.append(ChatColor.GOLD).append("📍 ").append(entry.getKey()).append(":\n");
                message.append(ChatColor.WHITE).append("  • 动物数量: ").append(ChatColor.RED)
                       .append(info.animalCount).append(ChatColor.WHITE).append(" (超出 ")
                       .append(ChatColor.RED).append(info.excessCount).append(ChatColor.WHITE).append(")\n");
                message.append(ChatColor.WHITE).append("  • 类型分布: ")
                       .append(String.join(", ", info.animalTypes)).append("\n");
            }
            
            message.append(ChatColor.YELLOW).append("\n总计将清理约 ").append(ChatColor.RED)
                   .append(totalExcess).append(ChatColor.YELLOW).append(" 只超标动物\n");
            message.append(ChatColor.YELLOW).append("请老师们尽快处理，避免动物损失！");
            
            Bukkit.broadcastMessage(message.toString());
            
            // 记录警告日志
            plugin.getLogger().info(String.format(
                "[LivestockDensityHandler] 发送密度警告 - 违规区块: %d, 预计清理: %d 只动物",
                violations.size(), totalExcess
            ));
            
        } catch (Exception e) {
            plugin.getLogger().severe("[LivestockDensityHandler] 发送警告消息时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 调度清理任务
     */
    private void scheduleCleanup(Map<String, ViolationInfo> violations) {
        try {
            // 清理之前的待处理任务
            pendingCleanups.clear();
            pendingCleanups.putAll(violations);
            
            // 取消之前的清理任务
            if (warningTask != null) {
                warningTask.cancel();
                warningTask = null;
            }
            
            // 验证警告时间配置
            int warningTime = config.getWarningTime();
            if (warningTime <= 0) {
                plugin.getLogger().warning("[LivestockDensityHandler] 无效的警告时间配置: " + warningTime + "，使用默认值5分钟");
                warningTime = 5;
            }
            
            // 启动倒计时清理
            startLivestockCountdown(warningTime, violations.size());
            
        } catch (Exception e) {
            plugin.getLogger().severe("[LivestockDensityHandler] 调度清理任务时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // 仿照EntityCleanupHandler的倒计时系统
    private void startLivestockCountdown(int warningTimeMinutes, int violationCount) {
        // 检查全局开关
        if (!config.isPluginEnabled() || !config.isLivestockDensityCheckEnabled()) {
            plugin.getLogger().info("[LivestockDensityHandler] 畜牧业清理倒计时跳过 - 功能已禁用");
            return;
        }
        
        // 获取警告时间列表（以秒为单位）
        List<Integer> warningTimes = getLivestockWarningTimes(warningTimeMinutes);
        if (warningTimes.isEmpty()) {
            // 如果没有警告时间，直接执行清理
            plugin.getLogger().info("[LivestockDensityHandler] 无警告时间配置，立即执行畜牧业清理");
            performLivestockCleanup();
            return;
        }
        
        int initialTime = warningTimes.get(0); // 最大的警告时间
        final int[] timeLeft = {initialTime};
        
        plugin.getLogger().info(String.format(
            "[LivestockDensityHandler] 开始畜牧业清理倒计时: %d 秒，涉及 %d 个区块",
            initialTime, violationCount
        ));
        
        warningTask = new BukkitRunnable() {
            @Override
            public void run() {
                // 检查全局开关
                if (!config.isPluginEnabled() || !config.isLivestockDensityCheckEnabled()) {
                    plugin.getLogger().info("[LivestockDensityHandler] 畜牧业清理倒计时取消 - 功能已禁用");
                    cancel();
                    return;
                }
                
                if (timeLeft[0] <= 0) {
                    // 倒计时结束，执行清理
                    plugin.getLogger().info("[LivestockDensityHandler] 倒计时结束，开始畜牧业清理");
                    // 输出光呀！
                    if (config.isBroadcastCleanup()) {
                        Bukkit.broadcastMessage(ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.YELLOW + "光呀！");
                    }
                    try {
                        performLivestockCleanup();
                    } catch (Exception e) {
                        plugin.getLogger().severe("[LivestockDensityHandler] 执行清理任务时发生异常: " + e.getMessage());
                        e.printStackTrace();
                    } finally {
                        isCleanupRunning.set(false);
                    }
                    cancel();
                    return;
                }
                
                // 检查是否需要发送警告消息
                if (warningTimes.contains(timeLeft[0])) {
                    sendLivestockCountdownMessage(timeLeft[0], violationCount);
                }
                
                timeLeft[0]--;
            }
        }.runTaskTimer(plugin, 0, 20L); // 每秒执行一次
    }
    
    // 获取畜牧业警告时间列表（降序排列，以秒为单位）
    private List<Integer> getLivestockWarningTimes(int warningTimeMinutes) {
        List<Integer> times = new ArrayList<>();
        
        int totalSeconds = warningTimeMinutes * 60;
        
        // 根据总时间设置合适的警告时间点
        if (totalSeconds >= 600) {
            // 10分钟以上：5分钟、2分钟、1分钟、30秒警告
            times.add(300); // 5分钟
            times.add(120); // 2分钟
            times.add(60);  // 1分钟
            times.add(30);  // 30秒
        } else if (totalSeconds >= 300) {
            // 5分钟以上：2分钟、1分钟、30秒警告
            times.add(120); // 2分钟
            times.add(60);  // 1分钟
            times.add(30);  // 30秒
        } else if (totalSeconds >= 120) {
            // 2分钟以上：1分钟、30秒警告
            times.add(60);  // 1分钟
            times.add(30);  // 30秒
        } else if (totalSeconds >= 60) {
            // 1分钟以上：30秒、10秒警告
            times.add(30);  // 30秒
            times.add(10);  // 10秒
        } else {
            // 短时间：只在10秒前警告
            times.add(10);  // 10秒
        }
        
        // 降序排列（最大的时间在前）
        times.sort((a, b) -> b.compareTo(a));
        return times;
    }
    
    // 发送畜牧业倒计时消息
    private void sendLivestockCountdownMessage(int timeLeft, int violationCount) {
        if (!config.isBroadcastCleanup()) {
            return;
        }
        
        String timeUnit = timeLeft >= 60 ? "分钟" : "秒";
        int displayTime = timeLeft >= 60 ? timeLeft / 60 : timeLeft;
        
        String message = ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "爱丽丝将在 " + 
                        ChatColor.RED + displayTime + timeUnit + ChatColor.WHITE + " 后清理过密的畜牧业区块（" + 
                        ChatColor.YELLOW + violationCount + "个区块" + ChatColor.WHITE + "），请老师们及时处理！";
        
        Bukkit.broadcastMessage(message);
        plugin.getLogger().info(String.format(
            "[LivestockDensityHandler] 畜牧业清理警告已发送: %d 秒剩余，%d 个违规区块",
            timeLeft, violationCount
        ));
    }
    
    /**
     * 执行畜牧业清理
     */
    private void performLivestockCleanup() {
        if (pendingCleanups.isEmpty()) {
            plugin.getLogger().info("[LivestockDensityHandler] 没有待处理的清理任务");
            return;
        }
        
        // 设置清理状态
        isCleanupRunning.set(true);
        
        int totalCleaned = 0;
        int totalProtected = 0;
        int processedChunks = 0;
        Map<String, Integer> cleanedByType = new HashMap<>();
        
        try {
            for (World world : Bukkit.getWorlds()) {
                if (world == null) continue;
                
                for (Chunk chunk : world.getLoadedChunks()) {
                    if (chunk == null) continue;
                    
                    String location = String.format("%s (%d, %d)", 
                        world.getName(), chunk.getX() * 16, chunk.getZ() * 16);
                    
                    ViolationInfo violationInfo = pendingCleanups.get(location);
                    if (violationInfo != null) {
                        processedChunks++;
                        List<Animals> animals = new ArrayList<>();
                        
                        try {
                            for (Entity entity : chunk.getEntities()) {
                                if (entity instanceof Animals) {
                                    animals.add((Animals) entity);
                                }
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("[LivestockDensityHandler] 获取区块实体时发生异常: " + e.getMessage());
                            continue;
                        }
                        
                        // 只清理超出部分的动物
                        int excess = animals.size() - config.getMaxAnimalsPerChunk();
                        if (excess > 0) {
                            int cleanedInChunk = 0;
                            
                            // 随机打乱动物列表，实现随机清理各种动物
                            Collections.shuffle(animals);
                            
                            for (int i = 0; i < excess && i < animals.size(); i++) {
                                Animals animal = animals.get(i);
                                
                                try {
                                    // 保护有名字的动物
                                    if (animal.getCustomName() != null) {
                                        totalProtected++;
                                        continue;
                                    }
                                    
                                    // 保护被拴住的动物（如果配置启用）
                                    if (config.isProtectLeashedEntities() && animal.isLeashed()) {
                                        totalProtected++;
                                        continue;
                                    }
                                    
                                    String animalType = animal.getType().name();
                                    animal.remove();
                                    totalCleaned++;
                                    cleanedInChunk++;
                                    cleanedByType.put(animalType, cleanedByType.getOrDefault(animalType, 0) + 1);
                                    
                                } catch (Exception e) {
                                    plugin.getLogger().warning("[LivestockDensityHandler] 清理动物时发生异常: " + e.getMessage());
                                }
                            }
                            
                            if (config.isDebugMode() && cleanedInChunk > 0) {
                                plugin.getLogger().info(String.format(
                                    "[LivestockDensityHandler] 区块 %s 清理了 %d 只动物",
                                    location, cleanedInChunk
                                ));
                            }
                        }
                    }
                }
            }
            
            // 发送清理完成消息
            if (totalCleaned > 0 || totalProtected > 0) {
                StringBuilder message = new StringBuilder();
                message.append(ChatColor.GOLD).append("[邦邦卡邦！] ").append(ChatColor.WHITE)
                       .append("爱丽丝完成了畜牧业密度管理！\n");
                message.append(ChatColor.WHITE).append("清理了 ").append(ChatColor.RED)
                       .append(totalCleaned).append(ChatColor.WHITE).append(" 只超标动物");
                
                if (totalProtected > 0) {
                    message.append("，保护了 ").append(ChatColor.GREEN)
                           .append(totalProtected).append(ChatColor.WHITE).append(" 只特殊动物");
                }
                
                if (config.isDebugMode() && !cleanedByType.isEmpty()) {
                    message.append("\n清理详情: ");
                    for (Map.Entry<String, Integer> entry : cleanedByType.entrySet()) {
                        message.append(entry.getKey()).append("(").append(entry.getValue()).append(") ");
                    }
                }
                
                Bukkit.broadcastMessage(message.toString());
            }
            
            // 记录清理统计
            plugin.getLogger().info(String.format(
                "[LivestockDensityHandler] 清理完成 - 处理区块: %d, 清理动物: %d, 保护动物: %d",
                processedChunks, totalCleaned, totalProtected
            ));
            
        } catch (Exception e) {
            plugin.getLogger().severe("[LivestockDensityHandler] 执行清理时发生严重异常: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 清理完成后重置状态
            pendingCleanups.clear();
            isCleanupRunning.set(false);
        }
    }
    
    /**
     * 重启密度检查
     */
    public void restartDensityCheck() {
        plugin.getLogger().info("[LivestockDensityHandler] 重启密度检查任务");
        try {
            stopDensityCheck();
            startDensityCheck();
        } catch (Exception e) {
            plugin.getLogger().severe("[LivestockDensityHandler] 重启密度检查时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取当前状态信息
     */
    public String getStatusInfo() {
        StringBuilder status = new StringBuilder();
        status.append("LivestockDensityHandler 状态:\n");
        status.append("- 密度检查任务: ").append(densityCheckTask != null ? "运行中" : "已停止").append("\n");
        status.append("- 警告清理任务: ").append(warningTask != null ? "已调度" : "无").append("\n");
        status.append("- 待处理清理: ").append(pendingCleanups.size()).append(" 个区块\n");
        status.append("- 清理状态: ").append(isCleanupRunning.get() ? "进行中" : "空闲");
        return status.toString();
    }
}