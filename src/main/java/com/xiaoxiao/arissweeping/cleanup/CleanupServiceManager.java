package com.xiaoxiao.arissweeping.cleanup;

import com.xiaoxiao.arissweeping.ArisSweeping;
import com.xiaoxiao.arissweeping.util.CleanupStats;
import com.xiaoxiao.arissweeping.util.CleanupStateManager;
import com.xiaoxiao.arissweeping.util.CleanupStateManager.CleanupType;
import com.xiaoxiao.arissweeping.util.LoggerUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 清理服务管理器
 * 负责管理和协调所有清理服务
 */
public class CleanupServiceManager {
    
    private final ArisSweeping plugin;
    private final Map<CleanupType, CleanupService> services;
    private final CleanupStateManager stateManager;
    
    public CleanupServiceManager(ArisSweeping plugin) {
        this.plugin = plugin;
        this.services = new ConcurrentHashMap<>();
        this.stateManager = CleanupStateManager.getInstance();
        
        // 注册默认清理服务
        registerDefaultServices();
    }
    
    /**
     * 注册默认清理服务
     */
    private void registerDefaultServices() {
        registerService(new StandardCleanupService(plugin));
        registerService(new LivestockCleanupService(plugin));
    }
    
    /**
     * 注册清理服务
     * @param service 要注册的清理服务
     */
    public void registerService(CleanupService service) {
        if (service == null) {
            throw new IllegalArgumentException("清理服务不能为null");
        }
        
        CleanupType type = service.getCleanupType();
        services.put(type, service);
        
        plugin.getLogger().info(String.format(
            "[CleanupServiceManager] 注册清理服务: %s (%s)",
            service.getServiceName(), type
        ));
    }
    
    /**
     * 注销清理服务
     * @param type 要注销的清理服务类型
     */
    public void unregisterService(CleanupType type) {
        CleanupService service = services.remove(type);
        if (service != null) {
            // 停止服务
            service.stopCleanup();
            
            plugin.getLogger().info(String.format(
                "[CleanupServiceManager] 注销清理服务: %s (%s)",
                service.getServiceName(), type
            ));
        }
    }
    
    /**
     * 获取清理服务
     * @param type 清理服务类型
     * @return 清理服务，如果不存在则返回null
     */
    public CleanupService getService(CleanupType type) {
        return services.get(type);
    }
    
    /**
     * 获取所有注册的清理服务
     * @return 所有清理服务的集合
     */
    public Collection<CleanupService> getAllServices() {
        return new ArrayList<>(services.values());
    }
    
    /**
     * 检查指定类型的清理服务是否正在运行
     * @param type 清理类型
     * @return 如果正在运行返回true，否则返回false
     */
    public boolean isCleanupRunning(CleanupType type) {
        CleanupService service = services.get(type);
        return service != null && service.isCleanupRunning();
    }
    
    /**
     * 检查是否有任何清理服务正在运行
     * @return 如果有清理服务正在运行返回true，否则返回false
     */
    public boolean isAnyCleanupRunning() {
        return services.values().stream().anyMatch(CleanupService::isCleanupRunning);
    }
    
    /**
     * 执行指定类型的清理
     * @param type 清理类型
     * @param sender 命令发送者
     * @param async 是否异步执行
     * @return 清理统计信息的Future对象
     */
    public CompletableFuture<CleanupStats> executeCleanup(CleanupType type, CommandSender sender, boolean async) {
        CleanupService service = services.get(type);
        if (service == null) {
            CompletableFuture<CleanupStats> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("未找到清理服务: " + type));
            return future;
        }
        
        if (!service.canExecuteCleanup()) {
            CompletableFuture<CleanupStats> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("清理服务无法执行: " + service.getServiceName()));
            return future;
        }
        
        if (async) {
            return service.executeCleanupAsync(sender);
        } else {
            try {
                CleanupStats stats = service.executeCleanup(sender);
                return CompletableFuture.completedFuture(stats);
            } catch (Exception e) {
                CompletableFuture<CleanupStats> future = new CompletableFuture<>();
                future.completeExceptionally(e);
                return future;
            }
        }
    }
    
    /**
     * 执行所有可用的清理服务
     * @param sender 命令发送者
     * @param async 是否异步执行
     * @return 所有清理统计信息的Future对象
     */
    public CompletableFuture<Map<CleanupType, CleanupStats>> executeAllCleanups(CommandSender sender, boolean async) {
        Map<CleanupType, CompletableFuture<CleanupStats>> futures = new HashMap<>();
        
        for (Map.Entry<CleanupType, CleanupService> entry : services.entrySet()) {
            CleanupType type = entry.getKey();
            CleanupService service = entry.getValue();
            
            if (service.canExecuteCleanup()) {
                futures.put(type, executeCleanup(type, sender, async));
            }
        }
        
        // 等待所有清理完成
        return CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                Map<CleanupType, CleanupStats> results = new HashMap<>();
                for (Map.Entry<CleanupType, CompletableFuture<CleanupStats>> entry : futures.entrySet()) {
                    try {
                        results.put(entry.getKey(), entry.getValue().get());
                    } catch (Exception e) {
                        plugin.getLogger().warning("[CleanupServiceManager] 清理服务 " + entry.getKey() + " 执行失败: " + e.getMessage());
                    }
                }
                return results;
            });
    }
    
    /**
     * 停止指定类型的清理服务
     * @param type 清理类型
     */
    public void stopCleanup(CleanupType type) {
        CleanupService service = services.get(type);
        if (service != null) {
            service.stopCleanup();
        }
    }
    
    /**
     * 停止所有清理服务
     */
    public void stopAllCleanups() {
        for (CleanupService service : services.values()) {
            service.stopCleanup();
        }
    }
    
    /**
     * 检查实体是否应该被清理
     * @param entity 要检查的实体
     * @return 如果应该被清理返回true，否则返回false
     */
    public boolean shouldCleanupEntity(Entity entity) {
        for (CleanupService service : services.values()) {
            if (service.shouldCleanupEntity(entity)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 批量清理实体
     * @param entities 要清理的实体列表
     * @param sender 命令发送者
     * @return 清理统计信息
     */
    public CleanupStats batchCleanupEntities(List<Entity> entities, CommandSender sender) {
        CleanupStats totalStats = new CleanupStats();
        
        // 按清理类型分组实体
        Map<CleanupType, List<Entity>> entityGroups = new HashMap<>();
        
        for (Entity entity : entities) {
            for (Map.Entry<CleanupType, CleanupService> entry : services.entrySet()) {
                CleanupType type = entry.getKey();
                CleanupService service = entry.getValue();
                
                if (service.shouldCleanupEntity(entity)) {
                    entityGroups.computeIfAbsent(type, k -> new ArrayList<>()).add(entity);
                    break; // 每个实体只分配给一个服务
                }
            }
        }
        
        // 使用相应的服务清理实体
        for (Map.Entry<CleanupType, List<Entity>> entry : entityGroups.entrySet()) {
            CleanupType type = entry.getKey();
            List<Entity> typeEntities = entry.getValue();
            CleanupService service = services.get(type);
            
            if (service != null && service.canExecuteCleanup()) {
                CleanupStats stats = service.batchCleanupEntities(typeEntities, sender);
                totalStats.merge(stats);
            }
        }
        
        return totalStats;
    }
    
    /**
     * 获取所有清理服务的状态信息
     * @return 状态信息字符串
     */
    public String getStatusInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== 清理服务管理器状态 ===\n");
        info.append("注册的服务数量: ").append(services.size()).append("\n");
        info.append("正在运行的服务: ").append(services.values().stream().mapToInt(s -> s.isCleanupRunning() ? 1 : 0).sum()).append("\n\n");
        
        for (Map.Entry<CleanupType, CleanupService> entry : services.entrySet()) {
            CleanupType type = entry.getKey();
            CleanupService service = entry.getValue();
            
            info.append("--- ").append(service.getServiceName()).append(" (").append(type).append(") ---\n");
            info.append(service.getStatusInfo()).append("\n");
        }
        
        return info.toString();
    }
    
    /**
     * 关闭管理器，停止所有服务
     */
    public void shutdown() {
        LoggerUtil.info("[CleanupServiceManager] 正在关闭清理服务管理器");
        
        stopAllCleanups();
        services.clear();
        
        LoggerUtil.info("[CleanupServiceManager] 清理服务管理器已关闭");
    }
}