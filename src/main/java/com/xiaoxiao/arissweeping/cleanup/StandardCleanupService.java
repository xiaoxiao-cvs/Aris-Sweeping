package com.xiaoxiao.arissweeping.cleanup;

import com.xiaoxiao.arissweeping.ArisSweeping;
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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 标准清理服务实现
 * 负责执行标准的实体清理任务
 */
public class StandardCleanupService implements CleanupService {
    
    private final ArisSweeping plugin;
    private final ModConfig config;
    private final CleanupStateManager stateManager;
    
    public StandardCleanupService(ArisSweeping plugin) {
        this.plugin = plugin;
        this.config = plugin.getModConfig();
        this.stateManager = CleanupStateManager.getInstance();
    }
    
    @Override
    public CleanupType getCleanupType() {
        return CleanupType.STANDARD;
    }
    
    @Override
    public String getServiceName() {
        return "标准清理服务";
    }
    
    @Override
    public String getServiceDescription() {
        return "执行标准的实体清理，包括掉落物品、经验球、箭矢等";
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
        
        if (!stateManager.tryStartCleanup(getCleanupType(), "STANDARD_SERVICE")) {
            sendMessage(sender, ChatColor.RED + "[" + getServiceName() + "] 无法启动清理任务，请稍后重试");
            return new CleanupStats();
        }
        
        try {
            sendMessage(sender, ChatColor.GREEN + "[" + getServiceName() + "] 开始执行清理...");
            CleanupStats stats = performCleanup();
            sendMessage(sender, ChatColor.GREEN + "[" + getServiceName() + "] 清理完成！" + formatCleanupResult(stats));
            return stats;
        } catch (Exception e) {
            plugin.getLogger().severe("[" + getServiceName() + "] 清理过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            sendMessage(sender, ChatColor.RED + "[" + getServiceName() + "] 清理过程中发生错误，请查看控制台日志");
            return new CleanupStats();
        } finally {
            stateManager.completeCleanup(getCleanupType());
        }
    }
    
    @Override
    public CompletableFuture<CleanupStats> executeCleanupAsync(CommandSender sender) {
        return CompletableFuture.supplyAsync(() -> {
            if (!canExecuteCleanup()) {
                sendMessageAsync(sender, ChatColor.RED + "[" + getServiceName() + "] 清理任务正在进行中或条件不满足，请稍候...");
                return new CleanupStats();
            }
            
            if (!stateManager.tryStartCleanup(getCleanupType(), "STANDARD_SERVICE_ASYNC")) {
                sendMessageAsync(sender, ChatColor.RED + "[" + getServiceName() + "] 无法启动清理任务，请稍后重试");
                return new CleanupStats();
            }
            
            try {
                sendMessageAsync(sender, ChatColor.GREEN + "[" + getServiceName() + "] 开始执行异步清理...");
                CleanupStats stats = performCleanup();
                sendMessageAsync(sender, ChatColor.GREEN + "[" + getServiceName() + "] 异步清理完成！" + formatCleanupResult(stats));
                return stats;
            } catch (Exception e) {
                plugin.getLogger().severe("[" + getServiceName() + "] 异步清理过程中发生错误: " + e.getMessage());
                e.printStackTrace();
                sendMessageAsync(sender, ChatColor.RED + "[" + getServiceName() + "] 异步清理过程中发生错误，请查看控制台日志");
                return new CleanupStats();
            } finally {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        stateManager.completeCleanup(getCleanupType());
                    }
                }.runTask(plugin);
            }
        });
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
        
        plugin.getLogger().info("[StandardCleanupService] 开始执行标准清理");
        
        for (World world : Bukkit.getWorlds()) {
            if (world == null) {
                continue;
            }
            
            // 检查插件是否在清理过程中被禁用
            if (!config.isPluginEnabled()) {
                plugin.getLogger().info("[StandardCleanupService] 清理过程中插件被禁用，停止清理");
                break;
            }
            
            cleanupWorld(world, stats);
        }
        
        plugin.getLogger().info(String.format(
            "[StandardCleanupService] 标准清理完成 - 总计清理: %d, 物品: %d, 经验球: %d, 箭矢: %d",
            stats.getTotalCleaned(), stats.getItemsCleaned(), 
            stats.getExperienceOrbsCleaned(), stats.getArrowsCleaned()
        ));
        
        return stats;
    }
    
    /**
     * 清理指定世界的实体
     * @param world 要清理的世界
     * @param stats 清理统计信息
     */
    private void cleanupWorld(World world, CleanupStats stats) {
        try {
            for (Chunk chunk : world.getLoadedChunks()) {
                if (chunk == null) {
                    continue;
                }
                
                cleanupChunk(chunk, stats);
                
                // 添加小延迟避免过度占用服务器资源
                if (config.getBatchDelay() > 0) {
                    try {
                        Thread.sleep(config.getBatchDelay());
                    } catch (InterruptedException e) {
                        // 简化异常处理，这个异常在Minecraft环境中不会发生
                        plugin.getLogger().warning("[StandardCleanupService] 清理过程被中断");
                        return;
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning(String.format(
                "[StandardCleanupService] 清理世界 %s 时发生异常: %s", 
                world.getName(), e.getMessage()
            ));
        }
    }
    
    /**
     * 清理指定区块的实体
     * @param chunk 要清理的区块
     * @param stats 清理统计信息
     */
    private void cleanupChunk(Chunk chunk, CleanupStats stats) {
        try {
            Entity[] entities = chunk.getEntities();
            
            for (Entity entity : entities) {
                if (entity == null || !entity.isValid()) {
                    continue;
                }
                
                if (shouldCleanupEntity(entity)) {
                    // 统计清理的实体类型
                    if (entity instanceof Item) {
                        stats.incrementItems();
                    } else if (entity instanceof ExperienceOrb) {
                        stats.incrementExperienceOrbs();
                    } else if (entity instanceof Arrow) {
                        stats.incrementArrows();
                    } else if (entity instanceof FallingBlock) {
                        stats.incrementFallingBlocks();
                    }
                    
                    entity.remove();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning(String.format(
                "[StandardCleanupService] 清理区块 [%d, %d] 时发生异常: %s",
                chunk.getX(), chunk.getZ(), e.getMessage()
            ));
        }
    }
    
    @Override
    public boolean shouldCleanupEntity(Entity entity) {
        // 永远不清理玩家
        if (entity instanceof Player) {
            return false;
        }
        
        // 使用统一的保护实体判断
        if (EntityTypeUtils.isProtectedEntity(entity)) {
            return false;
        }
        
        // 清理物品（检查年龄）
        if (entity instanceof Item) {
            if (!config.isCleanupItems()) {
                return false;
            }
            
            Item item = (Item) entity;
            int itemAge = item.getTicksLived();
            int ageThreshold = config.getItemAgeThreshold() * 20; // 转换为tick
            
            return itemAge >= ageThreshold;
        }
        
        // 清理经验球
        if (entity instanceof ExperienceOrb) {
            return config.isCleanupExperienceOrbs();
        }
        
        // 清理箭矢
        if (entity instanceof Arrow) {
            return config.isCleanupArrows();
        }
        
        // 清理掉落方块
        if (entity instanceof FallingBlock) {
            return config.isCleanupFallingBlocks();
        }
        
        // 标准清理不处理生物
        return false;
    }
    
    private String getAdditionalStatusInfo() {
        StringBuilder info = new StringBuilder();
        info.append("清理配置:\n");
        info.append("  - 清理物品: ").append(config.isCleanupItems() ? "启用" : "禁用").append("\n");
        info.append("  - 清理经验球: ").append(config.isCleanupExperienceOrbs() ? "启用" : "禁用").append("\n");
        info.append("  - 清理箭矢: ").append(config.isCleanupArrows() ? "启用" : "禁用").append("\n");
        info.append("  - 清理掉落方块: ").append(config.isCleanupFallingBlocks() ? "启用" : "禁用").append("\n");
        info.append("  - 物品年龄阈值: ").append(config.getItemAgeThreshold()).append("秒\n");
        info.append("  - 异步清理: ").append(config.isAsyncCleanup() ? "启用" : "禁用").append("\n");
        
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