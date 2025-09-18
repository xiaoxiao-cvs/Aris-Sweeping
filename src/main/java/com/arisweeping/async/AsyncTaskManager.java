package com.arisweeping.async;

import com.arisweeping.core.Constants;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 异步任务管理器
 * 
 * 负责管理多个线程池，提供不同类型的异步任务执行能力
 */
public class AsyncTaskManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // 线程池
    private final ThreadPoolExecutor coreThreadPool;
    private final ThreadPoolExecutor ioThreadPool;
    private final ScheduledExecutorService schedulerThreadPool;
    
    // 管理状态
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);
    private final AtomicLong taskCounter = new AtomicLong(0);
    
    public AsyncTaskManager() {
        LOGGER.info("Initializing AsyncTaskManager...");
        
        // 创建核心线程池 - 用于CPU密集型任务
        this.coreThreadPool = new ThreadPoolExecutor(
            Constants.AsyncProcessing.CORE_THREAD_POOL_SIZE,
            Constants.AsyncProcessing.MAX_THREAD_POOL_SIZE,
            Constants.AsyncProcessing.THREAD_KEEP_ALIVE_SECONDS,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(Constants.AsyncProcessing.TASK_QUEUE_CAPACITY),
            this::createCoreThread,
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // 创建IO线程池 - 用于IO密集型任务
        this.ioThreadPool = new ThreadPoolExecutor(
            Constants.AsyncProcessing.IO_THREAD_POOL_SIZE,
            Constants.AsyncProcessing.IO_THREAD_POOL_SIZE * 2,
            Constants.AsyncProcessing.THREAD_KEEP_ALIVE_SECONDS,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(Constants.AsyncProcessing.TASK_QUEUE_CAPACITY),
            this::createIOThread,
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // 创建调度线程池 - 用于定时任务
        this.schedulerThreadPool = Executors.newScheduledThreadPool(
            Constants.AsyncProcessing.SCHEDULER_THREAD_POOL_SIZE,
            this::createSchedulerThread
        );
        
        // 允许核心线程超时
        this.coreThreadPool.allowCoreThreadTimeOut(true);
        this.ioThreadPool.allowCoreThreadTimeOut(true);
        
        LOGGER.info("AsyncTaskManager initialized with core pool size: {}, io pool size: {}, scheduler pool size: {}",
                   coreThreadPool.getCorePoolSize(), ioThreadPool.getCorePoolSize(), 
                   Constants.AsyncProcessing.SCHEDULER_THREAD_POOL_SIZE);
    }
    
    /**
     * 提交核心任务（CPU密集型）
     */
    public <T> CompletableFuture<T> submitCoreTask(Callable<T> task) {
        if (isShutdown.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("AsyncTaskManager is shutdown"));
        }
        
        taskCounter.incrementAndGet();
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, coreThreadPool);
    }
    
    /**
     * 提交核心任务（无返回值）
     */
    public CompletableFuture<Void> submitCoreTask(Runnable task) {
        if (isShutdown.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("AsyncTaskManager is shutdown"));
        }
        
        taskCounter.incrementAndGet();
        return CompletableFuture.runAsync(task, coreThreadPool);
    }
    
    /**
     * 提交IO任务
     */
    public <T> CompletableFuture<T> submitIOTask(Callable<T> task) {
        if (isShutdown.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("AsyncTaskManager is shutdown"));
        }
        
        taskCounter.incrementAndGet();
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, ioThreadPool);
    }
    
    /**
     * 提交IO任务（无返回值）
     */
    public CompletableFuture<Void> submitIOTask(Runnable task) {
        if (isShutdown.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("AsyncTaskManager is shutdown"));
        }
        
        taskCounter.incrementAndGet();
        return CompletableFuture.runAsync(task, ioThreadPool);
    }
    
    /**
     * 调度延迟任务
     */
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        if (isShutdown.get()) {
            throw new IllegalStateException("AsyncTaskManager is shutdown");
        }
        
        return schedulerThreadPool.schedule(task, delay, unit);
    }
    
    /**
     * 调度固定频率任务
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        if (isShutdown.get()) {
            throw new IllegalStateException("AsyncTaskManager is shutdown");
        }
        
        return schedulerThreadPool.scheduleAtFixedRate(task, initialDelay, period, unit);
    }
    
    /**
     * 调度固定延迟任务
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
        if (isShutdown.get()) {
            throw new IllegalStateException("AsyncTaskManager is shutdown");
        }
        
        return schedulerThreadPool.scheduleWithFixedDelay(task, initialDelay, delay, unit);
    }
    
    /**
     * 获取线程池状态信息
     */
    public String getPoolStatus() {
        if (isShutdown.get()) {
            return "AsyncTaskManager is shutdown";
        }
        
        return String.format(
            "Core Pool: %d/%d (active/total), IO Pool: %d/%d (active/total), " +
            "Queue sizes: Core=%d, IO=%d, Total tasks: %d",
            coreThreadPool.getActiveCount(), coreThreadPool.getPoolSize(),
            ioThreadPool.getActiveCount(), ioThreadPool.getPoolSize(),
            coreThreadPool.getQueue().size(), ioThreadPool.getQueue().size(),
            taskCounter.get()
        );
    }
    
    /**
     * 检查是否已关闭
     */
    public boolean isShutdown() {
        return isShutdown.get();
    }
    
    /**
     * 优雅关闭
     */
    public void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            LOGGER.info("Shutting down AsyncTaskManager...");
            
            // 关闭线程池
            coreThreadPool.shutdown();
            ioThreadPool.shutdown();
            schedulerThreadPool.shutdown();
            
            try {
                // 等待任务完成
                if (!coreThreadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                    LOGGER.warn("Core thread pool did not terminate gracefully, forcing shutdown");
                    coreThreadPool.shutdownNow();
                }
                
                if (!ioThreadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                    LOGGER.warn("IO thread pool did not terminate gracefully, forcing shutdown");
                    ioThreadPool.shutdownNow();
                }
                
                if (!schedulerThreadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                    LOGGER.warn("Scheduler thread pool did not terminate gracefully, forcing shutdown");
                    schedulerThreadPool.shutdownNow();
                }
                
                LOGGER.info("AsyncTaskManager shutdown completed. Total tasks processed: {}", taskCounter.get());
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted during shutdown", e);
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 强制关闭
     */
    public void shutdownNow() {
        if (isShutdown.compareAndSet(false, true)) {
            LOGGER.warn("Force shutting down AsyncTaskManager...");
            
            coreThreadPool.shutdownNow();
            ioThreadPool.shutdownNow();
            schedulerThreadPool.shutdownNow();
            
            LOGGER.warn("AsyncTaskManager force shutdown completed. Total tasks processed: {}", taskCounter.get());
        }
    }
    
    // 线程工厂方法
    
    private Thread createCoreThread(Runnable r) {
        Thread thread = new Thread(r, "ArisSweeping-Core-" + taskCounter.incrementAndGet());
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY);
        return thread;
    }
    
    private Thread createIOThread(Runnable r) {
        Thread thread = new Thread(r, "ArisSweeping-IO-" + taskCounter.incrementAndGet());
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        return thread;
    }
    
    private Thread createSchedulerThread(Runnable r) {
        Thread thread = new Thread(r, "ArisSweeping-Scheduler-" + taskCounter.incrementAndGet());
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY);
        return thread;
    }
}