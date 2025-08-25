package com.xiaoxiao.arissweeping.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 线程安全管理器
 * 提供统一的线程安全机制和并发控制策略
 */
public class ThreadSafetyManager {
    
    // 单例实例
    private static volatile ThreadSafetyManager instance;
    private static final Object INSTANCE_LOCK = new Object();
    
    // 全局锁管理
    private final Map<String, ReentrantReadWriteLock> namedLocks = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> operationFlags = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> lastOperationTimes = new ConcurrentHashMap<>();
    
    // 配置
    private static final long DEFAULT_OPERATION_TIMEOUT_MS = 30000; // 30秒超时
    private static final long DEFAULT_COOLDOWN_MS = 1000; // 1秒冷却
    
    private ThreadSafetyManager() {
        // 私有构造函数
    }
    
    /**
     * 获取单例实例（线程安全的双重检查锁定）
     */
    public static ThreadSafetyManager getInstance() {
        if (instance == null) {
            synchronized (INSTANCE_LOCK) {
                if (instance == null) {
                    instance = new ThreadSafetyManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 获取命名锁
     * @param lockName 锁名称
     * @return 读写锁
     */
    public ReentrantReadWriteLock getNamedLock(String lockName) {
        return namedLocks.computeIfAbsent(lockName, k -> new ReentrantReadWriteLock(true)); // 公平锁
    }
    
    /**
     * 尝试获取操作标志（防止重复操作）
     * @param operationName 操作名称
     * @return 是否成功获取标志
     */
    public boolean tryAcquireOperationFlag(String operationName) {
        AtomicBoolean flag = operationFlags.computeIfAbsent(operationName, k -> new AtomicBoolean(false));
        return flag.compareAndSet(false, true);
    }
    
    /**
     * 释放操作标志
     * @param operationName 操作名称
     */
    public void releaseOperationFlag(String operationName) {
        AtomicBoolean flag = operationFlags.get(operationName);
        if (flag != null) {
            flag.set(false);
        }
    }
    
    /**
     * 检查操作冷却时间
     * @param operationName 操作名称
     * @param cooldownMs 冷却时间（毫秒）
     * @return 是否已过冷却时间
     */
    public boolean checkOperationCooldown(String operationName, long cooldownMs) {
        AtomicLong lastTime = lastOperationTimes.computeIfAbsent(operationName, k -> new AtomicLong(0));
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastTime.get();
        
        if (elapsed >= cooldownMs) {
            lastTime.set(currentTime);
            return true;
        }
        return false;
    }
    
    /**
     * 检查操作冷却时间（使用默认冷却时间）
     * @param operationName 操作名称
     * @return 是否已过冷却时间
     */
    public boolean checkOperationCooldown(String operationName) {
        return checkOperationCooldown(operationName, DEFAULT_COOLDOWN_MS);
    }
    
    /**
     * 安全执行操作（带超时和异常处理）
     * @param operationName 操作名称
     * @param operation 要执行的操作
     * @param timeoutMs 超时时间
     * @return 操作结果
     */
    public <T> SafeOperationResult<T> safeExecute(String operationName, SafeOperation<T> operation, long timeoutMs) {
        if (!tryAcquireOperationFlag(operationName)) {
            return SafeOperationResult.failure("操作 " + operationName + " 正在进行中");
        }
        
        try {
            ReentrantReadWriteLock lock = getNamedLock(operationName);
            boolean acquired = false;
            
            try {
                // 尝试获取写锁（带超时）
                acquired = lock.writeLock().tryLock(timeoutMs, TimeUnit.MILLISECONDS);
                if (!acquired) {
                    return SafeOperationResult.failure("获取锁超时: " + operationName);
                }
                
                // 执行操作
                T result = operation.execute();
                return SafeOperationResult.success(result);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return SafeOperationResult.failure("操作被中断: " + operationName);
            } catch (Exception e) {
                LoggerUtil.severe("ThreadSafetyManager", "操作执行失败: " + operationName, e);
                return SafeOperationResult.failure("操作执行失败: " + e.getMessage());
            } finally {
                if (acquired) {
                    lock.writeLock().unlock();
                }
            }
            
        } finally {
            releaseOperationFlag(operationName);
        }
    }
    
    /**
     * 安全执行操作（使用默认超时）
     * @param operationName 操作名称
     * @param operation 要执行的操作
     * @return 操作结果
     */
    public <T> SafeOperationResult<T> safeExecute(String operationName, SafeOperation<T> operation) {
        return safeExecute(operationName, operation, DEFAULT_OPERATION_TIMEOUT_MS);
    }
    
    /**
     * 安全执行读操作
     * @param operationName 操作名称
     * @param operation 要执行的操作
     * @param timeoutMs 超时时间
     * @return 操作结果
     */
    public <T> SafeOperationResult<T> safeRead(String operationName, SafeOperation<T> operation, long timeoutMs) {
        ReentrantReadWriteLock lock = getNamedLock(operationName);
        boolean acquired = false;
        
        try {
            // 尝试获取读锁（带超时）
            acquired = lock.readLock().tryLock(timeoutMs, TimeUnit.MILLISECONDS);
            if (!acquired) {
                return SafeOperationResult.failure("获取读锁超时: " + operationName);
            }
            
            // 执行操作
            T result = operation.execute();
            return SafeOperationResult.success(result);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SafeOperationResult.failure("读操作被中断: " + operationName);
        } catch (Exception e) {
            LoggerUtil.severe("ThreadSafetyManager", "读操作执行失败: " + operationName, e);
            return SafeOperationResult.failure("读操作执行失败: " + e.getMessage());
        } finally {
            if (acquired) {
                lock.readLock().unlock();
            }
        }
    }
    
    /**
     * 安全执行读操作（使用默认超时）
     * @param operationName 操作名称
     * @param operation 要执行的操作
     * @return 操作结果
     */
    public <T> SafeOperationResult<T> safeRead(String operationName, SafeOperation<T> operation) {
        return safeRead(operationName, operation, DEFAULT_OPERATION_TIMEOUT_MS);
    }
    
    /**
     * 获取操作状态信息
     * @return 状态信息
     */
    public String getStatusInfo() {
        StringBuilder status = new StringBuilder();
        status.append("线程安全管理器状态:\n");
        status.append("- 活跃锁数量: ").append(namedLocks.size()).append("\n");
        status.append("- 活跃操作标志: ").append(operationFlags.size()).append("\n");
        status.append("- 记录的操作时间: ").append(lastOperationTimes.size()).append("\n");
        
        // 显示正在进行的操作
        long activeOperations = operationFlags.values().stream()
            .mapToLong(flag -> flag.get() ? 1 : 0)
            .sum();
        status.append("- 正在进行的操作: ").append(activeOperations);
        
        return status.toString();
    }
    
    /**
     * 清理过期的锁和标志
     */
    public void cleanup() {
        long currentTime = System.currentTimeMillis();
        long expireTime = 5 * 60 * 1000; // 5分钟过期
        
        // 清理过期的操作时间记录
        lastOperationTimes.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().get() > expireTime);
        
        // 清理未使用的操作标志
        operationFlags.entrySet().removeIf(entry -> 
            !entry.getValue().get() && !lastOperationTimes.containsKey(entry.getKey()));
        
        LoggerUtil.debug(true, "ThreadSafetyManager", "清理完成，剩余锁: %d, 操作标志: %d", 
            namedLocks.size(), operationFlags.size());
    }
    
    /**
     * 关闭管理器
     */
    public void shutdown() {
        LoggerUtil.info("ThreadSafetyManager", "正在关闭线程安全管理器...");
        
        // 清理所有资源
        namedLocks.clear();
        operationFlags.clear();
        lastOperationTimes.clear();
        
        LoggerUtil.info("ThreadSafetyManager", "线程安全管理器已关闭");
    }
    
    /**
     * 安全操作接口
     */
    @FunctionalInterface
    public interface SafeOperation<T> {
        T execute() throws Exception;
    }
    
    /**
     * 安全操作结果
     */
    public static class SafeOperationResult<T> {
        private final boolean success;
        private final T result;
        private final String errorMessage;
        
        private SafeOperationResult(boolean success, T result, String errorMessage) {
            this.success = success;
            this.result = result;
            this.errorMessage = errorMessage;
        }
        
        public static <T> SafeOperationResult<T> success(T result) {
            return new SafeOperationResult<>(true, result, null);
        }
        
        public static <T> SafeOperationResult<T> failure(String errorMessage) {
            return new SafeOperationResult<>(false, null, errorMessage);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public T getResult() {
            return result;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public T getResultOrDefault(T defaultValue) {
            return success ? result : defaultValue;
        }
        
        public T getResultOrThrow() throws RuntimeException {
            if (!success) {
                throw new RuntimeException(errorMessage);
            }
            return result;
        }
    }
}