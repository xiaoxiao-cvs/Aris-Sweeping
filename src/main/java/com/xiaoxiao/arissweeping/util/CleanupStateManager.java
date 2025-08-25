package com.xiaoxiao.arissweeping.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * 统一的清理状态管理器
 * 负责管理所有清理任务的状态，避免重复的状态管理代码
 */
public class CleanupStateManager {
    
    // 清理状态枚举
    public enum CleanupType {
        STANDARD,      // 标准清理
        EMERGENCY,     // 紧急清理
        LIVESTOCK,     // 畜牧业清理
        ITEMS_ONLY,    // 仅物品清理
        MOBS_ONLY,     // 仅生物清理
        MANUAL         // 手动清理
    }
    
    // 清理任务状态
    public static class CleanupStatus {
        private final CleanupType type;
        private final long startTime;
        private final String initiator;
        private volatile boolean isRunning;
        
        public CleanupStatus(CleanupType type, String initiator) {
            this.type = type;
            this.initiator = initiator;
            this.startTime = System.currentTimeMillis();
            this.isRunning = true;
        }
        
        public CleanupType getType() { return type; }
        public long getStartTime() { return startTime; }
        public String getInitiator() { return initiator; }
        public boolean isRunning() { return isRunning; }
        public long getDuration() { return System.currentTimeMillis() - startTime; }
        
        public void markCompleted() {
            this.isRunning = false;
        }
    }
    
    // 单例实例
    private static volatile CleanupStateManager instance;
    
    // 状态管理
    private final Map<CleanupType, CleanupStatus> activeCleanups = new ConcurrentHashMap<>();
    private final AtomicBoolean globalCleanupLock = new AtomicBoolean(false);
    private final AtomicLong lastCleanupTime = new AtomicLong(0);
    private final Set<String> pendingCleanupTasks = new ConcurrentSkipListSet<>();
    
    // 冷却时间配置
    private static final long DEFAULT_COOLDOWN_MS = 5000; // 5秒
    private volatile long cooldownMs = DEFAULT_COOLDOWN_MS;
    
    private CleanupStateManager() {}
    
    /**
     * 获取单例实例
     */
    public static CleanupStateManager getInstance() {
        if (instance == null) {
            synchronized (CleanupStateManager.class) {
                if (instance == null) {
                    instance = new CleanupStateManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 尝试开始清理任务
     * @param type 清理类型
     * @param initiator 发起者标识
     * @return 是否成功开始清理
     */
    public boolean tryStartCleanup(CleanupType type, String initiator) {
        // 检查冷却时间
        if (!checkCooldown()) {
            return false;
        }
        
        // 检查是否有冲突的清理任务
        if (hasConflictingCleanup(type)) {
            return false;
        }
        
        // 尝试获取全局锁（对于需要独占的清理类型）
        if (requiresGlobalLock(type)) {
            if (!globalCleanupLock.compareAndSet(false, true)) {
                return false;
            }
        }
        
        // 记录清理状态
        CleanupStatus status = new CleanupStatus(type, initiator);
        activeCleanups.put(type, status);
        lastCleanupTime.set(System.currentTimeMillis());
        
        return true;
    }
    
    /**
     * 完成清理任务
     * @param type 清理类型
     */
    public void completeCleanup(CleanupType type) {
        CleanupStatus status = activeCleanups.remove(type);
        if (status != null) {
            status.markCompleted();
            
            // 释放全局锁
            if (requiresGlobalLock(type)) {
                globalCleanupLock.set(false);
            }
        }
    }
    
    /**
     * 检查特定类型的清理是否正在运行
     */
    public boolean isCleanupRunning(CleanupType type) {
        CleanupStatus status = activeCleanups.get(type);
        return status != null && status.isRunning();
    }
    
    /**
     * 检查是否有任何清理正在运行
     */
    public boolean isAnyCleanupRunning() {
        return !activeCleanups.isEmpty() || globalCleanupLock.get();
    }
    
    /**
     * 获取当前运行的清理状态
     */
    public Map<CleanupType, CleanupStatus> getActiveCleanups() {
        return new ConcurrentHashMap<>(activeCleanups);
    }
    
    /**
     * 获取清理状态信息
     */
    public String getStatusInfo() {
        StringBuilder status = new StringBuilder();
        status.append("=== 清理状态管理器 ===\n");
        status.append("全局锁状态: ").append(globalCleanupLock.get() ? "已锁定" : "空闲").append("\n");
        status.append("活跃清理任务: ").append(activeCleanups.size()).append("\n");
        
        if (!activeCleanups.isEmpty()) {
            status.append("当前清理任务:\n");
            activeCleanups.forEach((type, cleanupStatus) -> {
                status.append("  - ").append(type.name())
                      .append(" (发起者: ").append(cleanupStatus.getInitiator())
                      .append(", 运行时间: ").append(cleanupStatus.getDuration()).append("ms)\n");
            });
        }
        
        long timeSinceLastCleanup = System.currentTimeMillis() - lastCleanupTime.get();
        status.append("距离上次清理: ").append(timeSinceLastCleanup).append("ms\n");
        status.append("冷却时间: ").append(cooldownMs).append("ms");
        
        return status.toString();
    }
    
    /**
     * 强制停止所有清理任务
     */
    public void forceStopAllCleanups() {
        activeCleanups.clear();
        globalCleanupLock.set(false);
        pendingCleanupTasks.clear();
    }
    
    /**
     * 设置冷却时间
     */
    public void setCooldownMs(long cooldownMs) {
        this.cooldownMs = Math.max(0, cooldownMs);
    }
    
    /**
     * 获取剩余冷却时间
     */
    public long getRemainingCooldown() {
        long elapsed = System.currentTimeMillis() - lastCleanupTime.get();
        return Math.max(0, cooldownMs - elapsed);
    }
    
    /**
     * 添加待处理的清理任务
     */
    public void addPendingTask(String taskId) {
        pendingCleanupTasks.add(taskId);
    }
    
    /**
     * 移除待处理的清理任务
     */
    public void removePendingTask(String taskId) {
        pendingCleanupTasks.remove(taskId);
    }
    
    /**
     * 获取待处理任务数量
     */
    public int getPendingTaskCount() {
        return pendingCleanupTasks.size();
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 检查冷却时间
     */
    private boolean checkCooldown() {
        long elapsed = System.currentTimeMillis() - lastCleanupTime.get();
        return elapsed >= cooldownMs;
    }
    
    /**
     * 检查是否有冲突的清理任务
     */
    private boolean hasConflictingCleanup(CleanupType type) {
        // 紧急清理可以与其他清理并行
        if (type == CleanupType.EMERGENCY) {
            return false;
        }
        
        // 检查是否有需要独占的清理正在运行
        for (CleanupType activeType : activeCleanups.keySet()) {
            if (requiresGlobalLock(activeType) || requiresGlobalLock(type)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 判断清理类型是否需要全局锁
     */
    private boolean requiresGlobalLock(CleanupType type) {
        return type == CleanupType.STANDARD || 
               type == CleanupType.MANUAL || 
               type == CleanupType.LIVESTOCK;
    }
    
    /**
     * 关闭状态管理器
     */
    public void shutdown() {
        forceStopAllCleanups();
        instance = null;
    }
}