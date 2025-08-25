package com.xiaoxiao.arissweeping.cleanup;

import com.xiaoxiao.arissweeping.ArisSweeping;
import com.xiaoxiao.arissweeping.util.LoggerUtil;
import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.CleanupStats;
import com.xiaoxiao.arissweeping.util.CleanupStateManager;
import com.xiaoxiao.arissweeping.util.CleanupStateManager.CleanupType;
import com.xiaoxiao.arissweeping.util.EntityTypeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.entity.Animals;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 生物清理服务实现
 * 负责执行生物清理任务，包括智能清理和密度控制
 */
public class LivestockCleanupService implements CleanupService {
    
    private final ArisSweeping plugin;
    private final ModConfig config;
    private final CleanupStateManager stateManager;
    
    // 待清理的生物队列
    private final Queue<Entity> pendingCleanups = new LinkedList<>();
    private final Map<String, Integer> worldAnimalCounts = new HashMap<>();
    
    public LivestockCleanupService(ArisSweeping plugin) {
        this.plugin = plugin;
        this.config = plugin.getModConfig();
        this.stateManager = CleanupStateManager.getInstance();
    }
    
    @Override
    public CleanupType getCleanupType() {
        return CleanupType.LIVESTOCK;
    }
    
    @Override
    public String getServiceName() {
        return "生物清理服务";
    }
    
    @Override
    public String getServiceDescription() {
        return "执行生物清理，包括智能清理和密度控制";
    }
    
    @Override
    public boolean canExecuteCleanup() {
        return config.isPluginEnabled() && !isCleanupRunning();
    }
    
    @Override
    public CleanupStats executeCleanup(CommandSender sender) {
        if (!canExecuteCleanup()) {
            sendMessage(sender, ChatColor.RED + "[" + getServiceName() + "] 清理任务正在进行中或条件不满足，请稍候...");
            return new CleanupStats();
        }
        
        if (!stateManager.tryStartCleanup(getCleanupType(), "LIVESTOCK_SERVICE")) {
            sendMessage(sender, ChatColor.RED + "[" + getServiceName() + "] 无法启动清理任务，请稍后重试");
            return new CleanupStats();
        }
        
        try {
            sendMessage(sender, ChatColor.GREEN + "[" + getServiceName() + "] 开始执行清理...");
            CleanupStats stats = performCleanup();
            sendMessage(sender, ChatColor.GREEN + "[" + getServiceName() + "] 清理完成！" + formatCleanupResult(stats));
            return stats;
        } catch (Exception e) {
            LoggerUtil.severe("[" + getServiceName() + "] 清理过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            sendMessage(sender, ChatColor.RED + "[" + getServiceName() + "] 清理过程中发生错误，请查看控制台日志");
            return new CleanupStats();
        } finally {
            stateManager.completeCleanup(getCleanupType());
        }
    }
    
    @Override
    public CompletableFuture<CleanupStats> executeCleanupAsync(CommandSender sender) {
        CompletableFuture<CleanupStats> future = new CompletableFuture<>();
        
        // 在主线程中执行清理，避免异步线程访问Bukkit API导致的问题
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (!canExecuteCleanup()) {
                        sendMessage(sender, ChatColor.RED + "[" + getServiceName() + "] 清理任务正在进行中或条件不满足，请稍候...");
                        future.complete(new CleanupStats());
                        return;
                    }
                    
                    if (!stateManager.tryStartCleanup(getCleanupType(), "LIVESTOCK_SERVICE_ASYNC")) {
                        sendMessage(sender, ChatColor.RED + "[" + getServiceName() + "] 无法启动清理任务，请稍后重试");
                        future.complete(new CleanupStats());
                        return;
                    }
                    
                    try {
                        sendMessage(sender, ChatColor.GREEN + "[" + getServiceName() + "] 开始执行异步清理...");
                        CleanupStats stats = performCleanup();
                        sendMessage(sender, ChatColor.GREEN + "[" + getServiceName() + "] 异步清理完成！" + formatCleanupResult(stats));
                        future.complete(stats);
                    } catch (Exception e) {
                        LoggerUtil.severe("[" + getServiceName() + "] 异步清理过程中发生错误: " + e.getMessage());
                        e.printStackTrace();
                        sendMessage(sender, ChatColor.RED + "[" + getServiceName() + "] 异步清理过程中发生错误，请查看控制台日志");
                        future.complete(new CleanupStats());
                    } finally {
                        stateManager.completeCleanup(getCleanupType());
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        }.runTask(plugin);
        
        return future;
    }
    
    @Override
    public int cleanupEntities(List<Entity> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        
        int cleaned = 0;
        for (Entity entity : entities) {
            if (entity != null && entity.isValid() && shouldCleanupEntity(entity)) {
                entity.remove();
                cleaned++;
            }
        }
        return cleaned;
    }
    
    @Override
    public boolean isCleanupRunning() {
        return stateManager.isCleanupRunning(getCleanupType());
    }
    
    @Override
    public void stopCleanup() {
        stateManager.completeCleanup(getCleanupType());
    }
    
    @Override
    public String getStatusInfo() {
        StringBuilder status = new StringBuilder();
        status.append("=== ").append(getServiceName()).append(" 状态 ===").append("\n");
        status.append("服务描述: ").append(getServiceDescription()).append("\n");
        status.append("清理类型: ").append(getCleanupType().name()).append("\n");
        status.append("运行状态: ").append(isCleanupRunning() ? "运行中" : "空闲").append("\n");
        status.append("可执行清理: ").append(canExecuteCleanup() ? "是" : "否").append("\n");
        status.append(getAdditionalStatusInfo());
        return status.toString();
    }
    
    private CleanupStats performCleanup() {
        CleanupStats stats = new CleanupStats();
        
        LoggerUtil.info("[LivestockCleanupService] 开始执行生物清理");
        
        // 执行智能清理
        performSmartCleanup(stats);
        
        // 处理待清理队列
        processPendingCleanups(stats);
        
        LoggerUtil.info("[LivestockCleanupService] 生物清理完成 - 总计清理: " + stats.getTotalCleaned() + ", 生物: " + stats.getMobsCleaned());
        
        return stats;
    }
    
    /**
     * 执行智能清理
     * @param stats 清理统计信息
     */
    private void performSmartCleanup(CleanupStats stats) {
        if (!config.isSmartCleanupEnabled()) {
            LoggerUtil.info("[LivestockCleanupService] 智能清理已禁用");
            return;
        }
        
        // 更新世界动物计数
        updateWorldAnimalCounts();
        
        for (World world : Bukkit.getWorlds()) {
            if (world == null) {
                continue;
            }
            
            // 检查插件是否在清理过程中被禁用
            if (!config.isPluginEnabled()) {
                LoggerUtil.info("[LivestockCleanupService] 清理过程中插件被禁用，停止清理");
                break;
            }
            
            cleanupWorldAnimals(world, stats);
        }
    }
    
    /**
     * 更新世界动物计数
     */
    private void updateWorldAnimalCounts() {
        worldAnimalCounts.clear();
        
        for (World world : Bukkit.getWorlds()) {
            if (world == null) {
                continue;
            }
            
            int animalCount = 0;
            for (Entity entity : world.getEntities()) {
                if (EntityTypeUtils.isAnimal(entity)) {
                    animalCount++;
                }
            }
            
            worldAnimalCounts.put(world.getName(), animalCount);
        }
    }
    
    /**
     * 清理指定世界的动物
     * @param world 要清理的世界
     * @param stats 清理统计信息
     */
    private void cleanupWorldAnimals(World world, CleanupStats stats) {
        try {
            String worldName = world.getName();
            int currentAnimalCount = worldAnimalCounts.getOrDefault(worldName, 0);
            int maxAnimals = config.getMaxAnimalsPerChunk() * 16; // 假设每世界最大动物数为每区块限制的16倍
            
            if (currentAnimalCount <= maxAnimals) {
                return; // 不需要清理
            }
            
            LoggerUtil.info(String.format(
                "[LivestockCleanupService] 世界 %s 动物数量 (%d) 超过限制 (%d)，开始清理",
                worldName, currentAnimalCount, maxAnimals
            ));
            
            // 收集需要清理的动物
            List<Animals> animalsToCleanup = new ArrayList<>();
            
            for (Chunk chunk : world.getLoadedChunks()) {
                if (chunk == null) {
                    continue;
                }
                
                for (Entity entity : chunk.getEntities()) {
                    if (EntityTypeUtils.isAnimal(entity) && shouldCleanupEntity(entity)) {
                        animalsToCleanup.add((Animals) entity);
                    }
                }
            }
            
            // 按优先级排序（年龄较大的优先清理）
            animalsToCleanup.sort((a1, a2) -> Integer.compare(a2.getTicksLived(), a1.getTicksLived()));
            
            // 清理多余的动物
            int animalsToRemove = currentAnimalCount - maxAnimals;
            int removed = 0;
            
            for (Animals animal : animalsToCleanup) {
                if (removed >= animalsToRemove) {
                    break;
                }
                
                if (animal.isValid()) {
                    animal.remove();
                    stats.incrementMobs();
                    removed++;
                }
            }
            
            LoggerUtil.info(String.format(
                "[LivestockCleanupService] 世界 %s 清理了 %d 只动物",
                worldName, removed
            ));
            
        } catch (Exception e) {
            LoggerUtil.warning(String.format(
                "[LivestockCleanupService] 清理世界 %s 的动物时发生异常: %s",
                world.getName(), e.getMessage()
            ));
        }
    }
    
    /**
     * 处理待清理队列
     * @param stats 清理统计信息
     */
    private void processPendingCleanups(CleanupStats stats) {
        if (pendingCleanups.isEmpty()) {
            return;
        }
        
        LoggerUtil.info(String.format(
            "[LivestockCleanupService] 处理 %d 个待清理的生物",
            pendingCleanups.size()
        ));
        
        int processed = 0;
        while (!pendingCleanups.isEmpty() && processed < config.getBatchSize()) {
            Entity entity = pendingCleanups.poll();
            
            if (entity != null && entity.isValid()) {
                entity.remove();
                stats.incrementMobs();
                processed++;
            }
        }
        
        LoggerUtil.info(String.format(
            "[LivestockCleanupService] 从待清理队列中处理了 %d 个生物",
            processed
        ));
    }
    
    @Override
    public boolean shouldCleanupEntity(Entity entity) {
        // 永远不清理玩家
        if (entity instanceof Player) {
            return false;
        }
        
        // 只处理动物
        if (!EntityTypeUtils.isAnimal(entity)) {
            return false;
        }
        
        Animals animal = (Animals) entity;
        
        // 不清理有自定义名称的动物
        if (animal.getCustomName() != null && !animal.getCustomName().isEmpty()) {
            return false;
        }
        
        // 不清理被驯服的动物
        if (animal instanceof Tameable) {
            Tameable tameable = (Tameable) animal;
            if (tameable.isTamed()) {
                return false;
            }
        }
        
        // 不清理被拴住的动物
        if (animal.isLeashed()) {
            return false;
        }
        
        // 不清理幼体动物（如果配置禁止）
        if (false && !animal.isAdult()) { // 默认不清理幼体动物
            return false;
        }
        
        // 检查动物年龄
        int animalAge = animal.getTicksLived();
        int ageThreshold = config.getItemAgeThreshold() * 20; // 转换为tick，使用物品年龄阈值作为动物年龄阈值
        
        return animalAge >= ageThreshold;
    }
    
    /**
     * 添加生物到待清理队列
     * @param entity 要添加的生物
     */
    public void addToPendingCleanup(Entity entity) {
        if (EntityTypeUtils.isAnimal(entity) && shouldCleanupEntity(entity)) {
            pendingCleanups.offer(entity);
        }
    }
    
    /**
     * 获取待清理队列大小
     * @return 待清理队列大小
     */
    public int getPendingCleanupCount() {
        return pendingCleanups.size();
    }
    
    /**
     * 清空待清理队列
     */
    public void clearPendingCleanups() {
        pendingCleanups.clear();
    }
    
    /**
     * 获取指定世界的动物数量
     * @param worldName 世界名称
     * @return 动物数量
     */
    public int getWorldAnimalCount(String worldName) {
        return worldAnimalCounts.getOrDefault(worldName, 0);
    }
    
    private String getAdditionalStatusInfo() {
        StringBuilder info = new StringBuilder();
        info.append("生物清理配置:\n");
        info.append("  - 智能清理: ").append(config.isSmartCleanupEnabled() ? "启用" : "禁用").append("\n");
        info.append("  - 清理幼体动物: ").append("禁用").append("\n");
        info.append("  - 动物年龄阈值: ").append(config.getItemAgeThreshold()).append("秒\n");
        info.append("  - 每世界最大动物数: ").append(config.getMaxAnimalsPerChunk() * 16).append("\n");
        info.append("  - 待清理队列大小: ").append(getPendingCleanupCount()).append("\n");
        
        info.append("\n世界动物统计:\n");
        for (Map.Entry<String, Integer> entry : worldAnimalCounts.entrySet()) {
            info.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        
        return info.toString();
    }
    
    private String formatCleanupResult(CleanupStats stats) {
        if (stats.getTotalCleaned() == 0) {
            return " 没有找到需要清理的实体";
        }
        
        StringBuilder result = new StringBuilder();
        result.append(" 清理了 ").append(stats.getTotalCleaned()).append(" 个实体");
        
        if (stats.getItemsCleaned() > 0) {
            result.append("（物品: ").append(stats.getItemsCleaned()).append("）");
        }
        if (stats.getMobsCleaned() > 0) {
            result.append("（生物: ").append(stats.getMobsCleaned()).append("）");
        }
        if (stats.getExperienceOrbsCleaned() > 0) {
            result.append("（经验球: ").append(stats.getExperienceOrbsCleaned()).append("）");
        }
        if (stats.getArrowsCleaned() > 0) {
            result.append("（箭矢: ").append(stats.getArrowsCleaned()).append("）");
        }
        
        return result.toString();
    }
    
    private void sendMessage(CommandSender sender, String message) {
        if (sender != null) {
            sender.sendMessage(message);
        }
    }
    
    private void sendMessageAsync(CommandSender sender, String message) {
        if (sender != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    sender.sendMessage(message);
                }
            }.runTask(plugin);
        }
    }
    
    @Override
    public CleanupStats batchCleanupEntities(List<Entity> entities, CommandSender sender) {
        CleanupStats stats = new CleanupStats();
        
        for (Entity entity : entities) {
            if (shouldCleanupEntity(entity)) {
                entity.remove();
                stats.incrementType(entity);
            }
        }
        
        return stats;
    }
}