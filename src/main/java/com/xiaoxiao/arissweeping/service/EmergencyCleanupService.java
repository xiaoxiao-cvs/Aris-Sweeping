package com.xiaoxiao.arissweeping.service;

import com.xiaoxiao.arissweeping.ArisSweeping;
import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * 紧急清理服务 - 负责处理TPS过低时的紧急清理操作
 * 从EntityCleanupHandler中分离出来，遵循单一职责原则
 */
public class EmergencyCleanupService {
    private final ArisSweeping plugin;
    private final ModConfig config;
    private final TpsMonitor tpsMonitor;
    private final CleanupStateManager stateManager;
    private final EntityValidationService validationService;
    private final CleanupNotificationService notificationService;
    
    public EmergencyCleanupService(ArisSweeping plugin, 
                                 TpsMonitor tpsMonitor,
                                 EntityValidationService validationService,
                                 CleanupNotificationService notificationService) {
        this.plugin = plugin;
        this.config = plugin.getModConfig();
        this.tpsMonitor = tpsMonitor;
        this.stateManager = CleanupStateManager.getInstance();
        this.validationService = validationService;
        this.notificationService = notificationService;
    }
    
    /**
     * 执行紧急清理（由TPS监控触发）
     */
    public void performEmergencyCleanup() {
        try {
            // 检查是否启用紧急清理
            if (!config.isEmergencyCleanupEnabled()) {
                LoggerUtil.info("紧急清理跳过 - 紧急清理已禁用");
                return;
            }
            
            // 检查全局开关
            if (!config.isPluginEnabled()) {
                LoggerUtil.info("紧急清理跳过 - 插件已禁用");
                return;
            }
            
            // 检查是否已在运行
            if (!stateManager.tryStartCleanup(CleanupStateManager.CleanupType.EMERGENCY, "TPS_MONITOR")) {
                LoggerUtil.info("紧急清理跳过 - 已在运行中");
                return;
            }
            
            double currentTps = tpsMonitor.getCurrentTps();
            double threshold = config.getLowTpsThreshold();
            
            LoggerUtil.warning("执行紧急清理，TPS过低 (当前: " + currentTps + ", 阈值: " + threshold + ")");
            
            // 发送紧急清理开始通知
            notificationService.sendEmergencyCleanupStartNotification(currentTps, threshold);
            
            // 执行清理
            CleanupStats stats = executeEmergencyCleanup();
            
            // 发送完成通知
            long duration = System.currentTimeMillis() - System.currentTimeMillis(); // 这里应该记录实际耗时
            notificationService.broadcastEmergencyCleanupNotification(stats, duration);
            
            LoggerUtil.info("紧急清理完成，清理了 " + stats.getTotalCleaned() + " 个实体");
            
        } catch (Exception e) {
            LoggerUtil.severe("紧急清理执行失败: " + e.getMessage());
            e.printStackTrace();
            
            // 发送错误通知
            notificationService.sendEmergencyCleanupErrorNotification();
            
        } finally {
            stateManager.completeCleanup(CleanupStateManager.CleanupType.EMERGENCY);
        }
    }
    
    /**
     * 执行紧急清理操作
     */
    private CleanupStats executeEmergencyCleanup() {
        CleanupStats stats = new CleanupStats();
        long startTime = System.currentTimeMillis();
        
        try {
            // 紧急清理使用更激进的策略
            for (World world : Bukkit.getWorlds()) {
                // 再次检查插件状态
                if (!config.isPluginEnabled()) {
                    LoggerUtil.info("紧急清理中断 - 插件已禁用");
                    break;
                }
                
                if (world == null) {
                    LoggerUtil.warning("跳过无效世界的紧急清理");
                    continue;
                }
                
                cleanupWorldEmergency(world, stats);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            LoggerUtil.info("紧急清理完成，耗时 " + duration + "ms，清理了 " + stats.getTotalCleaned() + " 个实体");
            
        } catch (Exception e) {
            LoggerUtil.severe("执行紧急清理时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        
        return stats;
    }
    
    /**
     * 对单个世界执行紧急清理
     */
    private void cleanupWorldEmergency(World world, CleanupStats stats) {
        try {
            List<Entity> entities = new ArrayList<>(world.getEntities());
            
            if (config.isClusterCleanupEnabled()) {
                cleanupWorldWithClusterAlgorithm(world, entities, stats);
            } else {
                cleanupWorldWithStandardAlgorithm(world, entities, stats);
            }
            
            LoggerUtil.info("世界 " + world.getName() + " 紧急清理完成");
            
        } catch (Exception e) {
            LoggerUtil.severe("世界 " + world.getName() + " 紧急清理失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 使用聚集算法进行紧急清理
     */
    private void cleanupWorldWithClusterAlgorithm(World world, List<Entity> entities, CleanupStats stats) {
        try {
            LoggerUtil.info("在世界 " + world.getName() + " 中使用聚集清理算法进行紧急清理");
            
            List<Entity> candidates = ClusterDetector.getClusterCleanupCandidates(
                entities,
                config.getClusterDetectionDistance(),
                config.getMinClusterSize(),
                config.getClusterPreserveRatio()
            );
            
            for (Entity entity : candidates) {
                if (cleanupEntitySafely(entity, stats)) {
                    // 紧急清理时可以更激进，但仍要保证安全
                }
            }
            
        } catch (Exception e) {
            LoggerUtil.severe("世界 " + world.getName() + " 的紧急聚集清理失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 使用标准算法进行紧急清理
     */
    private void cleanupWorldWithStandardAlgorithm(World world, List<Entity> entities, CleanupStats stats) {
        try {
            LoggerUtil.info("在世界 " + world.getName() + " 中使用标准清理算法进行紧急清理");
            
            for (Entity entity : entities) {
                if (cleanupEntitySafely(entity, stats)) {
                    // 标准清理逻辑
                }
            }
            
        } catch (Exception e) {
            LoggerUtil.severe("世界 " + world.getName() + " 的紧急标准清理失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 安全地清理实体
     */
    private boolean cleanupEntitySafely(Entity entity, CleanupStats stats) {
        try {
            if (entity != null && entity.isValid() && validationService.shouldCleanupEntity(entity)) {
                entity.remove();
                stats.incrementType(entity);
                return true;
            }
        } catch (Exception e) {
            LoggerUtil.warning("紧急清理实体时失败: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * 检查是否需要紧急清理
     */
    public boolean shouldPerformEmergencyCleanup() {
        try {
            if (!config.isEmergencyCleanupEnabled() || !config.isPluginEnabled()) {
                return false;
            }
            
            if (stateManager.isCleanupRunning(CleanupStateManager.CleanupType.EMERGENCY)) {
                return false; // 已在运行紧急清理
            }
            
            double currentTps = tpsMonitor.getCurrentTps();
            double threshold = config.getLowTpsThreshold();
            
            return currentTps < threshold;
            
        } catch (Exception e) {
            LoggerUtil.warning("检查紧急清理条件时发生错误: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 强制执行紧急清理（管理员命令触发）
     */
    public void forceEmergencyCleanup() {
        try {
            LoggerUtil.info("管理员强制执行紧急清理");
            
            if (!stateManager.tryStartCleanup(CleanupStateManager.CleanupType.EMERGENCY, "ADMIN_COMMAND")) {
                LoggerUtil.warning("强制紧急清理失败 - 已有清理在运行中");
                return;
            }
            
            // 发送开始通知
            notificationService.sendEmergencyCleanupStartNotification(tpsMonitor.getCurrentTps(), 0.0);
            
            // 执行清理
            CleanupStats stats = executeEmergencyCleanup();
            
            // 发送完成通知
            long duration = 0; // 这里应该记录实际耗时
            notificationService.broadcastEmergencyCleanupNotification(stats, duration);
            
            LoggerUtil.info("强制紧急清理完成");
            
        } catch (Exception e) {
            LoggerUtil.severe("强制紧急清理失败: " + e.getMessage());
            e.printStackTrace();
            notificationService.sendEmergencyCleanupErrorNotification();
        } finally {
            stateManager.completeCleanup(CleanupStateManager.CleanupType.EMERGENCY);
        }
    }
    
    /**
     * 获取紧急清理状态信息
     */
    public String getStatusInfo() {
        StringBuilder status = new StringBuilder();
        status.append("紧急清理服务状态:\n");
        
        // 基本状态
        status.append("- 紧急清理启用: ").append(config.isEmergencyCleanupEnabled() ? "是" : "否").append("\n");
        status.append("- 当前TPS: ").append(String.format("%.2f", tpsMonitor.getCurrentTps())).append("\n");
        status.append("- TPS阈值: ").append(config.getLowTpsThreshold()).append("\n");
        
        // 运行状态
        boolean isRunning = stateManager.isCleanupRunning(CleanupStateManager.CleanupType.EMERGENCY);
        status.append("- 紧急清理运行中: ").append(isRunning ? "是" : "否").append("\n");
        
        // 清理条件
        boolean shouldCleanup = shouldPerformEmergencyCleanup();
        status.append("- 满足清理条件: ").append(shouldCleanup ? "是" : "否");
        
        return status.toString();
    }
    
    /**
     * 检查紧急清理是否正在运行
     */
    public boolean isEmergencyCleanupRunning() {
        return stateManager.isCleanupRunning(CleanupStateManager.CleanupType.EMERGENCY);
    }
    
    /**
     * 获取当前TPS
     */
    public double getCurrentTps() {
        return tpsMonitor.getCurrentTps();
    }
    
    /**
     * 获取TPS阈值
     */
    public double getTpsThreshold() {
        return config.getLowTpsThreshold();
    }
}