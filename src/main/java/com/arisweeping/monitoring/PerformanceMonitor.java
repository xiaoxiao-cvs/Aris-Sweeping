package com.arisweeping.monitoring;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.arisweeping.async.AsyncTaskManager;
import com.arisweeping.async.ThreadSafeCounter;
import com.arisweeping.core.ArisLogger;

/**
 * 性能监控器
 * 提供实时性能指标、内存使用追踪和清理效率统计
 */
public class PerformanceMonitor {
    
    // 监控间隔
    private static final int MONITORING_INTERVAL_SECONDS = 5;
    private static final int STATS_HISTORY_SIZE = 100; // 保留最近100个统计点
    
    // 性能指标
    public static class PerformanceMetrics {
        private final long timestamp;
        private final double cpuUsage;
        private final MemoryUsage heapMemory;
        private final MemoryUsage nonHeapMemory;
        private final int threadCount;
        private final double systemLoadAverage;
        
        // 模组特定指标
        private final int activeAsyncTasks;
        private final long totalTasksExecuted;
        private final long totalEntitiesProcessed;
        private final double cleaningEfficiency; // entities/second
        
        public PerformanceMetrics(long timestamp, double cpuUsage, MemoryUsage heapMemory,
                                MemoryUsage nonHeapMemory, int threadCount, double systemLoadAverage,
                                int activeAsyncTasks, long totalTasksExecuted, 
                                long totalEntitiesProcessed, double cleaningEfficiency) {
            this.timestamp = timestamp;
            this.cpuUsage = cpuUsage;
            this.heapMemory = heapMemory;
            this.nonHeapMemory = nonHeapMemory;
            this.threadCount = threadCount;
            this.systemLoadAverage = systemLoadAverage;
            this.activeAsyncTasks = activeAsyncTasks;
            this.totalTasksExecuted = totalTasksExecuted;
            this.totalEntitiesProcessed = totalEntitiesProcessed;
            this.cleaningEfficiency = cleaningEfficiency;
        }
        
        // Getters
        public long getTimestamp() { return timestamp; }
        public double getCpuUsage() { return cpuUsage; }
        public MemoryUsage getHeapMemory() { return heapMemory; }
        public MemoryUsage getNonHeapMemory() { return nonHeapMemory; }
        public int getThreadCount() { return threadCount; }
        public double getSystemLoadAverage() { return systemLoadAverage; }
        public int getActiveAsyncTasks() { return activeAsyncTasks; }
        public long getTotalTasksExecuted() { return totalTasksExecuted; }
        public long getTotalEntitiesProcessed() { return totalEntitiesProcessed; }
        public double getCleaningEfficiency() { return cleaningEfficiency; }
        
        @Override
        public String toString() {
            return String.format("PerformanceMetrics{cpu=%.1f%%, heap=%dMB/%dMB, threads=%d, tasks=%d, efficiency=%.2f/s}", 
                               cpuUsage, heapMemory.getUsed() / 1024 / 1024, heapMemory.getMax() / 1024 / 1024,
                               threadCount, activeAsyncTasks, cleaningEfficiency);
        }
    }
    
    // 性能警报
    public static class PerformanceAlert {
        public enum AlertLevel {
            INFO, WARNING, CRITICAL
        }
        
        private final AlertLevel level;
        private final String message;
        private final long timestamp;
        private final Map<String, Object> context;
        
        public PerformanceAlert(AlertLevel level, String message, Map<String, Object> context) {
            this.level = level;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
            this.context = new HashMap<>(context);
        }
        
        public PerformanceAlert(AlertLevel level, String message) {
            this(level, message, Collections.emptyMap());
        }
        
        // Getters
        public AlertLevel getLevel() { return level; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
        public Map<String, Object> getContext() { return context; }
        
        @Override
        public String toString() {
            return String.format("[%s] %s", level, message);
        }
    }
    
    // 单例实例
    private static volatile PerformanceMonitor instance;
    
    // 系统监控组件
    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;
    
    // 监控数据
    private final AsyncTaskManager asyncManager;
    private final AtomicReference<PerformanceMetrics> currentMetrics;
    private final Queue<PerformanceMetrics> metricsHistory;
    private final Queue<PerformanceAlert> recentAlerts;
    
    // 监控状态
    private volatile boolean isRunning = false;
    private ScheduledExecutorService monitoringExecutor;
    
    // 性能阈值
    private double cpuThreshold = 80.0;      // CPU使用率警告阈值
    private double memoryThreshold = 85.0;   // 内存使用率警告阈值
    private int maxThreadCount = 50;         // 最大线程数警告阈值
    private double minEfficiency = 10.0;     // 最小清理效率警告阈值
    
    private PerformanceMonitor(AsyncTaskManager asyncManager) {
        this.asyncManager = asyncManager;
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
        
        this.currentMetrics = new AtomicReference<>();
        this.metricsHistory = new ConcurrentLinkedQueue<>();
        this.recentAlerts = new ConcurrentLinkedQueue<>();
        
        ArisLogger.info("PerformanceMonitor initialized");
    }
    
    /**
     * 获取性能监控器实例
     */
    public static PerformanceMonitor getInstance(AsyncTaskManager asyncManager) {
        if (instance == null) {
            synchronized (PerformanceMonitor.class) {
                if (instance == null) {
                    instance = new PerformanceMonitor(asyncManager);
                }
            }
        }
        return instance;
    }
    
    /**
     * 启动性能监控
     */
    public synchronized void start() {
        if (isRunning) {
            ArisLogger.warn("PerformanceMonitor is already running");
            return;
        }
        
        ArisLogger.info("Starting PerformanceMonitor...");
        
        monitoringExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "ArisSweeping-PerformanceMonitor");
            thread.setDaemon(true);
            return thread;
        });
        
        // 定期收集性能指标
        monitoringExecutor.scheduleAtFixedRate(
            this::collectMetrics,
            0,
            MONITORING_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
        
        // 定期检查性能警报
        monitoringExecutor.scheduleAtFixedRate(
            this::checkAlerts,
            MONITORING_INTERVAL_SECONDS,
            MONITORING_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
        
        // 定期清理历史数据
        monitoringExecutor.scheduleAtFixedRate(
            this::cleanupHistory,
            60,
            60,
            TimeUnit.SECONDS
        );
        
        isRunning = true;
        ArisLogger.info("PerformanceMonitor started successfully");
    }
    
    /**
     * 停止性能监控
     */
    public synchronized void stop() {
        if (!isRunning) {
            return;
        }
        
        ArisLogger.info("Stopping PerformanceMonitor...");
        
        isRunning = false;
        
        if (monitoringExecutor != null) {
            monitoringExecutor.shutdown();
            try {
                if (!monitoringExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    monitoringExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                monitoringExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        ArisLogger.info("PerformanceMonitor stopped");
    }
    
    /**
     * 收集当前性能指标
     */
    private void collectMetrics() {
        try {
            long timestamp = System.currentTimeMillis();
            
            // 收集系统指标
            MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeapMemory = memoryBean.getNonHeapMemoryUsage();
            int threadCount = threadBean.getThreadCount();
            double systemLoadAverage = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
            
            // 估算CPU使用率（简化版）
            double cpuUsage = calculateCpuUsage();
            
            // 收集模组特定指标
            int activeAsyncTasks = getActiveAsyncTaskCount();
            long totalTasksExecuted = ThreadSafeCounter.get(ThreadSafeCounter.TASKS_EXECUTED);
            long totalEntitiesProcessed = ThreadSafeCounter.get(ThreadSafeCounter.ENTITIES_PROCESSED);
            
            // 计算清理效率
            double cleaningEfficiency = calculateCleaningEfficiency();
            
            // 创建性能指标对象
            PerformanceMetrics metrics = new PerformanceMetrics(
                timestamp, cpuUsage, heapMemory, nonHeapMemory, threadCount,
                systemLoadAverage, activeAsyncTasks, totalTasksExecuted,
                totalEntitiesProcessed, cleaningEfficiency
            );
            
            // 更新当前指标和历史记录
            currentMetrics.set(metrics);
            addToHistory(metrics);
            
            ArisLogger.debug("Collected performance metrics: {}", metrics);
            
        } catch (Exception e) {
            ArisLogger.error("Failed to collect performance metrics", e);
        }
    }
    
    /**
     * 计算CPU使用率（简化估算）
     */
    private double calculateCpuUsage() {
        // 这是一个简化的CPU使用率估算
        // 实际应用中可能需要更复杂的计算
        double systemLoad = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        
        if (systemLoad < 0) {
            return 0.0; // 系统不支持负载均值
        }
        
        return Math.min(100.0, (systemLoad / availableProcessors) * 100.0);
    }
    
    /**
     * 获取活动异步任务数量
     */
    private int getActiveAsyncTaskCount() {
        try {
            if (asyncManager != null) {
                return asyncManager.getTotalActiveCount();
            }
            return 0;
        } catch (Exception e) {
            ArisLogger.debug("Failed to get active async task count", e);
            return 0;
        }
    }
    
    /**
     * 计算清理效率
     */
    private double calculateCleaningEfficiency() {
        // 计算最近一段时间的清理效率（实体数/秒）
        List<PerformanceMetrics> recent = getRecentMetrics(10); // 最近10个数据点
        
        if (recent.size() < 2) {
            return 0.0;
        }
        
        PerformanceMetrics oldest = recent.get(0);
        PerformanceMetrics latest = recent.get(recent.size() - 1);
        
        long timeDiff = latest.getTimestamp() - oldest.getTimestamp();
        long entityDiff = latest.getTotalEntitiesProcessed() - oldest.getTotalEntitiesProcessed();
        
        if (timeDiff <= 0) {
            return 0.0;
        }
        
        return (double) entityDiff / (timeDiff / 1000.0); // entities per second
    }
    
    /**
     * 将指标添加到历史记录
     */
    private void addToHistory(PerformanceMetrics metrics) {
        metricsHistory.offer(metrics);
        
        // 限制历史记录大小
        while (metricsHistory.size() > STATS_HISTORY_SIZE) {
            metricsHistory.poll();
        }
    }
    
    /**
     * 检查性能警报
     */
    private void checkAlerts() {
        PerformanceMetrics current = currentMetrics.get();
        if (current == null) {
            return;
        }
        
        try {
            // 检查CPU使用率
            if (current.getCpuUsage() > cpuThreshold) {
                addAlert(PerformanceAlert.AlertLevel.WARNING,
                        String.format("高CPU使用率: %.1f%%", current.getCpuUsage()));
            }
            
            // 检查内存使用率
            MemoryUsage heapMemory = current.getHeapMemory();
            double memoryUsagePercent = (double) heapMemory.getUsed() / heapMemory.getMax() * 100;
            
            if (memoryUsagePercent > memoryThreshold) {
                addAlert(PerformanceAlert.AlertLevel.WARNING,
                        String.format("高内存使用率: %.1f%%", memoryUsagePercent));
            }
            
            // 检查线程数量
            if (current.getThreadCount() > maxThreadCount) {
                addAlert(PerformanceAlert.AlertLevel.WARNING,
                        String.format("线程数量过多: %d", current.getThreadCount()));
            }
            
            // 检查清理效率
            if (current.getCleaningEfficiency() < minEfficiency && current.getActiveAsyncTasks() > 0) {
                addAlert(PerformanceAlert.AlertLevel.INFO,
                        String.format("清理效率较低: %.2f entities/s", current.getCleaningEfficiency()));
            }
            
            // 检查内存泄漏迹象
            checkMemoryLeak();
            
        } catch (Exception e) {
            ArisLogger.error("Failed to check performance alerts", e);
        }
    }
    
    /**
     * 检查内存泄漏迹象
     */
    private void checkMemoryLeak() {
        List<PerformanceMetrics> recent = getRecentMetrics(20); // 最近20个数据点
        
        if (recent.size() < 10) {
            return;
        }
        
        // 检查内存使用是否持续增长
        boolean isMemoryIncreasing = true;
        long previousMemory = recent.get(0).getHeapMemory().getUsed();
        
        for (int i = 1; i < recent.size(); i++) {
            long currentMemory = recent.get(i).getHeapMemory().getUsed();
            if (currentMemory <= previousMemory) {
                isMemoryIncreasing = false;
                break;
            }
            previousMemory = currentMemory;
        }
        
        if (isMemoryIncreasing) {
            long memoryIncrease = recent.get(recent.size() - 1).getHeapMemory().getUsed() - 
                                recent.get(0).getHeapMemory().getUsed();
            
            if (memoryIncrease > 50 * 1024 * 1024) { // 超过50MB增长
                addAlert(PerformanceAlert.AlertLevel.CRITICAL,
                        "检测到可能的内存泄漏：内存使用持续增长");
            }
        }
    }
    
    /**
     * 添加性能警报
     */
    private void addAlert(PerformanceAlert.AlertLevel level, String message) {
        PerformanceAlert alert = new PerformanceAlert(level, message);
        recentAlerts.offer(alert);
        
        // 限制警报数量
        while (recentAlerts.size() > 50) {
            recentAlerts.poll();
        }
        
        // 记录到日志
        switch (level) {
            case INFO:
                ArisLogger.info("Performance Alert: {}", message);
                break;
            case WARNING:
                ArisLogger.warn("Performance Alert: {}", message);
                break;
            case CRITICAL:
                ArisLogger.error("Performance Alert: {}", message);
                break;
        }
    }
    
    /**
     * 清理历史数据
     */
    private void cleanupHistory() {
        // 清理过期的警报（保留最近1小时的）
        long cutoffTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
        recentAlerts.removeIf(alert -> alert.getTimestamp() < cutoffTime);
    }
    
    /**
     * 获取当前性能指标
     */
    public PerformanceMetrics getCurrentMetrics() {
        return currentMetrics.get();
    }
    
    /**
     * 获取最近的性能指标
     */
    public List<PerformanceMetrics> getRecentMetrics(int count) {
        List<PerformanceMetrics> result = new ArrayList<>(metricsHistory);
        
        if (result.size() <= count) {
            return result;
        }
        
        return result.subList(result.size() - count, result.size());
    }
    
    /**
     * 获取所有性能指标历史
     */
    public List<PerformanceMetrics> getAllMetrics() {
        return new ArrayList<>(metricsHistory);
    }
    
    /**
     * 获取最近的警报
     */
    public List<PerformanceAlert> getRecentAlerts() {
        return new ArrayList<>(recentAlerts);
    }
    
    /**
     * 获取性能摘要报告
     */
    public String getPerformanceSummary() {
        PerformanceMetrics current = getCurrentMetrics();
        if (current == null) {
            return "性能监控尚未启动或无数据";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("=== ArisSweeping 性能监控报告 ===\n");
        
        // 系统资源
        summary.append(String.format("CPU使用率: %.1f%%\n", current.getCpuUsage()));
        
        MemoryUsage heap = current.getHeapMemory();
        long heapUsedMB = heap.getUsed() / 1024 / 1024;
        long heapMaxMB = heap.getMax() / 1024 / 1024;
        double heapPercent = (double) heap.getUsed() / heap.getMax() * 100;
        summary.append(String.format("堆内存: %dMB / %dMB (%.1f%%)\n", heapUsedMB, heapMaxMB, heapPercent));
        
        summary.append(String.format("线程数量: %d\n", current.getThreadCount()));
        
        if (current.getSystemLoadAverage() >= 0) {
            summary.append(String.format("系统负载: %.2f\n", current.getSystemLoadAverage()));
        }
        
        // 模组性能
        summary.append("\n--- 模组性能 ---\n");
        summary.append(String.format("活动任务: %d\n", current.getActiveAsyncTasks()));
        summary.append(String.format("总执行任务: %d\n", current.getTotalTasksExecuted()));
        summary.append(String.format("总处理实体: %d\n", current.getTotalEntitiesProcessed()));
        summary.append(String.format("清理效率: %.2f entities/s\n", current.getCleaningEfficiency()));
        
        // 最近警报
        List<PerformanceAlert> alerts = getRecentAlerts();
        if (!alerts.isEmpty()) {
            summary.append("\n--- 最近警报 ---\n");
            alerts.stream()
                    .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                    .limit(5)
                    .forEach(alert -> summary.append(String.format("[%s] %s\n", 
                            alert.getLevel(), alert.getMessage())));
        }
        
        return summary.toString();
    }
    
    /**
     * 强制垃圾回收并记录前后内存使用情况
     */
    public void performGCAnalysis() {
        PerformanceMetrics beforeGC = getCurrentMetrics();
        if (beforeGC == null) {
            return;
        }
        
        long beforeHeap = beforeGC.getHeapMemory().getUsed();
        ArisLogger.info("Performing GC analysis - Memory before GC: {} MB", beforeHeap / 1024 / 1024);
        
        // 建议垃圾回收
        System.gc();
        
        // 等待一小段时间让GC完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 收集GC后的指标
        collectMetrics();
        PerformanceMetrics afterGC = getCurrentMetrics();
        
        if (afterGC != null) {
            long afterHeap = afterGC.getHeapMemory().getUsed();
            long freedMemory = beforeHeap - afterHeap;
            
            ArisLogger.info("GC analysis completed - Memory after GC: {} MB, Freed: {} MB", 
                       afterHeap / 1024 / 1024, freedMemory / 1024 / 1024);
            
            if (freedMemory > 10 * 1024 * 1024) { // 超过10MB
                addAlert(PerformanceAlert.AlertLevel.INFO,
                        String.format("GC释放了 %d MB 内存", freedMemory / 1024 / 1024));
            }
        }
    }
    
    // 配置方法
    public void setCpuThreshold(double cpuThreshold) { this.cpuThreshold = cpuThreshold; }
    public void setMemoryThreshold(double memoryThreshold) { this.memoryThreshold = memoryThreshold; }
    public void setMaxThreadCount(int maxThreadCount) { this.maxThreadCount = maxThreadCount; }
    public void setMinEfficiency(double minEfficiency) { this.minEfficiency = minEfficiency; }
    
    public boolean isRunning() { return isRunning; }
}