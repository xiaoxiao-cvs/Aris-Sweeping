package com.xiaoxiao.arissweeping.performance;

import com.xiaoxiao.arissweeping.observer.CleanupEvent;
import com.xiaoxiao.arissweeping.observer.CleanupEventManager;
import com.xiaoxiao.arissweeping.config.ConfigManager;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 性能监控器
 * 负责实时监控系统性能并在性能问题时发出警告
 */
public class PerformanceMonitor {
    
    private static final Logger logger = Logger.getLogger(PerformanceMonitor.class.getName());
    
    private final PerformanceMetrics metrics;
    private final CleanupEventManager eventManager;
    private final ConfigManager configManager;
    
    // 监控配置
    private volatile boolean enabled = true;
    private volatile long monitoringInterval = 30000; // 30秒
    private volatile double memoryWarningThreshold = 0.8; // 80%
    private volatile double memoryErrorThreshold = 0.9; // 90%
    private volatile long executionTimeWarningThreshold = 5000; // 5秒
    private volatile long executionTimeErrorThreshold = 10000; // 10秒
    private volatile double errorRateWarningThreshold = 5.0; // 5%
    private volatile double errorRateErrorThreshold = 10.0; // 10%
    
    // 监控状态
    private final ScheduledExecutorService monitoringExecutor;
    private ScheduledFuture<?> monitoringTask;
    private final Map<String, Long> lastWarningTimes = new ConcurrentHashMap<>();
    private final long warningCooldown = 300000; // 5分钟冷却时间
    
    // 性能历史记录
    private final Queue<PerformanceSnapshot> performanceHistory = new ConcurrentLinkedQueue<>();
    private final int maxHistorySize = 100;
    
    // 阈值监控
    private final Map<String, ThresholdMonitor> thresholdMonitors = new ConcurrentHashMap<>();
    
    public PerformanceMonitor(PerformanceMetrics metrics, CleanupEventManager eventManager, ConfigManager configManager) {
        this.metrics = metrics;
        this.eventManager = eventManager;
        this.configManager = configManager;
        this.monitoringExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PerformanceMonitor");
            t.setDaemon(true);
            return t;
        });
        
        initializeThresholdMonitors();
        loadConfiguration();
    }
    
    /**
     * 启动性能监控
     */
    public void start() {
        if (!enabled) {
            logger.info("Performance monitoring is disabled");
            return;
        }
        
        if (monitoringTask != null && !monitoringTask.isDone()) {
            logger.warning("Performance monitoring is already running");
            return;
        }
        
        logger.info("Starting performance monitoring with interval: " + monitoringInterval + "ms");
        
        monitoringTask = monitoringExecutor.scheduleAtFixedRate(
            this::performMonitoringCheck,
            monitoringInterval,
            monitoringInterval,
            TimeUnit.MILLISECONDS
        );
    }
    
    /**
     * 停止性能监控
     */
    public void stop() {
        if (monitoringTask != null) {
            monitoringTask.cancel(false);
            monitoringTask = null;
        }
        logger.info("Performance monitoring stopped");
    }
    
    /**
     * 检查是否正在监控
     */
    public boolean isMonitoring() {
        return monitoringTask != null && !monitoringTask.isDone();
    }
    
    /**
     * 关闭监控器
     */
    public void shutdown() {
        stop();
        monitoringExecutor.shutdown();
        try {
            if (!monitoringExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                monitoringExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            monitoringExecutor.shutdownNow();
        }
    }
    
    /**
     * 执行监控检查
     */
    private void performMonitoringCheck() {
        try {
            // 获取当前性能快照
            PerformanceSnapshot snapshot = createPerformanceSnapshot();
            
            // 添加到历史记录
            addToHistory(snapshot);
            
            // 检查各种性能指标
            checkMemoryUsage(snapshot);
            checkExecutionTime(snapshot);
            checkErrorRate(snapshot);
            checkThroughput(snapshot);
            checkCustomThresholds(snapshot);
            
            // 发布性能监控事件
            publishPerformanceEvent(snapshot);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during performance monitoring check", e);
        }
    }
    
    /**
     * 创建性能快照
     */
    private PerformanceSnapshot createPerformanceSnapshot() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        PerformanceMetrics.MetricsSnapshot metricsSnapshot = metrics.getSnapshot();
        
        return new PerformanceSnapshot(
            System.currentTimeMillis(),
            usedMemory,
            totalMemory,
            maxMemory,
            (double) usedMemory / maxMemory,
            metricsSnapshot.getAverageExecutionTime(),
            metricsSnapshot.getEntitiesPerSecond(),
            metricsSnapshot.getErrorRate(),
            metricsSnapshot.getRemovalRate(),
            metricsSnapshot
        );
    }
    
    /**
     * 检查内存使用情况
     */
    private void checkMemoryUsage(PerformanceSnapshot snapshot) {
        double memoryUsageRatio = snapshot.getMemoryUsageRatio();
        
        if (memoryUsageRatio >= memoryErrorThreshold) {
            sendWarning("MEMORY_ERROR", 
                String.format("Critical memory usage: %.1f%% (threshold: %.1f%%)", 
                    memoryUsageRatio * 100, memoryErrorThreshold * 100),
                WarningLevel.ERROR);
        } else if (memoryUsageRatio >= memoryWarningThreshold) {
            sendWarning("MEMORY_WARNING", 
                String.format("High memory usage: %.1f%% (threshold: %.1f%%)", 
                    memoryUsageRatio * 100, memoryWarningThreshold * 100),
                WarningLevel.WARNING);
        }
    }
    
    /**
     * 检查执行时间
     */
    private void checkExecutionTime(PerformanceSnapshot snapshot) {
        double avgExecutionTime = snapshot.getAverageExecutionTime();
        
        if (avgExecutionTime >= executionTimeErrorThreshold) {
            sendWarning("EXECUTION_TIME_ERROR", 
                String.format("Critical execution time: %.1fms (threshold: %dms)", 
                    avgExecutionTime, executionTimeErrorThreshold),
                WarningLevel.ERROR);
        } else if (avgExecutionTime >= executionTimeWarningThreshold) {
            sendWarning("EXECUTION_TIME_WARNING", 
                String.format("High execution time: %.1fms (threshold: %dms)", 
                    avgExecutionTime, executionTimeWarningThreshold),
                WarningLevel.WARNING);
        }
    }
    
    /**
     * 检查错误率
     */
    private void checkErrorRate(PerformanceSnapshot snapshot) {
        double errorRate = snapshot.getErrorRate();
        
        if (errorRate >= errorRateErrorThreshold) {
            sendWarning("ERROR_RATE_ERROR", 
                String.format("Critical error rate: %.1f%% (threshold: %.1f%%)", 
                    errorRate, errorRateErrorThreshold),
                WarningLevel.ERROR);
        } else if (errorRate >= errorRateWarningThreshold) {
            sendWarning("ERROR_RATE_WARNING", 
                String.format("High error rate: %.1f%% (threshold: %.1f%%)", 
                    errorRate, errorRateWarningThreshold),
                WarningLevel.WARNING);
        }
    }
    
    /**
     * 检查吞吐量
     */
    private void checkThroughput(PerformanceSnapshot snapshot) {
        double entitiesPerSecond = snapshot.getEntitiesPerSecond();
        
        // 如果吞吐量突然下降，可能表示性能问题
        if (performanceHistory.size() >= 3) {
            List<PerformanceSnapshot> recent = new ArrayList<>(performanceHistory);
            int size = recent.size();
            double recentAvg = recent.subList(Math.max(0, size - 3), size)
                .stream().mapToDouble(PerformanceSnapshot::getEntitiesPerSecond).average().orElse(0.0);
            
            if (recentAvg > 0 && entitiesPerSecond < recentAvg * 0.5) {
                sendWarning("THROUGHPUT_DROP", 
                    String.format("Throughput dropped significantly: %.1f entities/s (recent avg: %.1f)", 
                        entitiesPerSecond, recentAvg),
                    WarningLevel.WARNING);
            }
        }
    }
    
    /**
     * 检查自定义阈值
     */
    private void checkCustomThresholds(PerformanceSnapshot snapshot) {
        for (Map.Entry<String, ThresholdMonitor> entry : thresholdMonitors.entrySet()) {
            String name = entry.getKey();
            ThresholdMonitor monitor = entry.getValue();
            
            try {
                monitor.check(snapshot);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error checking threshold monitor: " + name, e);
            }
        }
    }
    
    /**
     * 发送警告
     */
    private void sendWarning(String warningType, String message, WarningLevel level) {
        // 检查冷却时间
        long currentTime = System.currentTimeMillis();
        Long lastWarningTime = lastWarningTimes.get(warningType);
        
        if (lastWarningTime != null && (currentTime - lastWarningTime) < warningCooldown) {
            return; // 还在冷却期内
        }
        
        lastWarningTimes.put(warningType, currentTime);
        
        // 记录日志
        Level logLevel = level == WarningLevel.ERROR ? Level.SEVERE : Level.WARNING;
        logger.log(logLevel, "Performance " + level.name().toLowerCase() + ": " + message);
        
        // 发布事件
        if (eventManager != null) {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("level", level.name());
            CleanupEvent.PerformanceWarningEvent event = new CleanupEvent.PerformanceWarningEvent(
                "PerformanceMonitor", warningType, message, 0.0, 0.0, metrics);
            eventManager.publishEvent(event);
        }
    }
    
    /**
     * 发布性能事件
     */
    private void publishPerformanceEvent(PerformanceSnapshot snapshot) {
        if (eventManager != null) {
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("snapshot", snapshot.getMetricsSnapshot());
            CleanupEvent.StatisticsUpdatedEvent event = new CleanupEvent.StatisticsUpdatedEvent(
                "PerformanceMonitor", "performance", statistics, monitoringInterval);
            eventManager.publishEvent(event);
        }
    }
    
    /**
     * 添加到历史记录
     */
    private void addToHistory(PerformanceSnapshot snapshot) {
        performanceHistory.offer(snapshot);
        
        // 保持历史记录大小
        while (performanceHistory.size() > maxHistorySize) {
            performanceHistory.poll();
        }
    }
    
    /**
     * 初始化阈值监控器
     */
    private void initializeThresholdMonitors() {
        // 可以在这里添加自定义的阈值监控器
    }
    
    /**
     * 加载配置
     */
    private void loadConfiguration() {
        if (configManager != null) {
            enabled = configManager.getBoolean("performance.monitoring.enabled", enabled);
            monitoringInterval = configManager.getLong("performance.monitoring.interval", monitoringInterval);
            memoryWarningThreshold = configManager.getDouble("performance.memory.warning-threshold", memoryWarningThreshold);
            memoryErrorThreshold = configManager.getDouble("performance.memory.error-threshold", memoryErrorThreshold);
            executionTimeWarningThreshold = configManager.getLong("performance.execution-time.warning-threshold", executionTimeWarningThreshold);
            executionTimeErrorThreshold = configManager.getLong("performance.execution-time.error-threshold", executionTimeErrorThreshold);
            errorRateWarningThreshold = configManager.getDouble("performance.error-rate.warning-threshold", errorRateWarningThreshold);
            errorRateErrorThreshold = configManager.getDouble("performance.error-rate.error-threshold", errorRateErrorThreshold);
        }
    }
    
    /**
     * 重新加载配置
     */
    public void reloadConfiguration() {
        loadConfiguration();
        
        // 如果监控间隔改变，重启监控任务
        if (monitoringTask != null && !monitoringTask.isDone()) {
            stop();
            start();
        }
    }
    
    /**
     * 启动监控（别名方法）
     */
    public void startMonitoring() {
        start();
    }
    
    /**
     * 停止监控（别名方法）
     */
    public void stopMonitoring() {
        stop();
    }
    
    /**
     * 更新配置
     */
    public void updateConfiguration(ConfigManager configManager) {
        if (configManager != null) {
            this.enabled = configManager.getBoolean("performance.monitoring.enabled", enabled);
            this.monitoringInterval = configManager.getLong("performance.monitoring.interval", monitoringInterval);
            this.memoryWarningThreshold = configManager.getDouble("performance.memory.warning-threshold", memoryWarningThreshold);
            this.memoryErrorThreshold = configManager.getDouble("performance.memory.error-threshold", memoryErrorThreshold);
            this.executionTimeWarningThreshold = configManager.getLong("performance.execution-time.warning-threshold", executionTimeWarningThreshold);
            this.executionTimeErrorThreshold = configManager.getLong("performance.execution-time.error-threshold", executionTimeErrorThreshold);
            this.errorRateWarningThreshold = configManager.getDouble("performance.error-rate.warning-threshold", errorRateWarningThreshold);
            this.errorRateErrorThreshold = configManager.getDouble("performance.error-rate.error-threshold", errorRateErrorThreshold);
            
            // 如果监控间隔改变，重启监控任务
            if (monitoringTask != null && !monitoringTask.isDone()) {
                stop();
                start();
            }
        }
    }
    
    /**
     * 添加自定义阈值监控器
     */
    public void addThresholdMonitor(String name, ThresholdMonitor monitor) {
        thresholdMonitors.put(name, monitor);
    }
    
    /**
     * 移除阈值监控器
     */
    public void removeThresholdMonitor(String name) {
        thresholdMonitors.remove(name);
    }
    
    /**
     * 获取性能历史记录
     */
    public List<PerformanceSnapshot> getPerformanceHistory() {
        return new ArrayList<>(performanceHistory);
    }
    
    /**
     * 获取当前性能快照
     */
    public PerformanceSnapshot getCurrentSnapshot() {
        return createPerformanceSnapshot();
    }
    
    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public long getMonitoringInterval() { return monitoringInterval; }
    public void setMonitoringInterval(long monitoringInterval) { this.monitoringInterval = monitoringInterval; }
    
    public double getMemoryWarningThreshold() { return memoryWarningThreshold; }
    public void setMemoryWarningThreshold(double memoryWarningThreshold) { this.memoryWarningThreshold = memoryWarningThreshold; }
    
    public double getMemoryErrorThreshold() { return memoryErrorThreshold; }
    public void setMemoryErrorThreshold(double memoryErrorThreshold) { this.memoryErrorThreshold = memoryErrorThreshold; }
    
    public long getExecutionTimeWarningThreshold() { return executionTimeWarningThreshold; }
    public void setExecutionTimeWarningThreshold(long executionTimeWarningThreshold) { this.executionTimeWarningThreshold = executionTimeWarningThreshold; }
    
    public long getExecutionTimeErrorThreshold() { return executionTimeErrorThreshold; }
    public void setExecutionTimeErrorThreshold(long executionTimeErrorThreshold) { this.executionTimeErrorThreshold = executionTimeErrorThreshold; }
    
    public double getErrorRateWarningThreshold() { return errorRateWarningThreshold; }
    public void setErrorRateWarningThreshold(double errorRateWarningThreshold) { this.errorRateWarningThreshold = errorRateWarningThreshold; }
    
    public double getErrorRateErrorThreshold() { return errorRateErrorThreshold; }
    public void setErrorRateErrorThreshold(double errorRateErrorThreshold) { this.errorRateErrorThreshold = errorRateErrorThreshold; }
    
    /**
     * 警告级别枚举
     */
    public enum WarningLevel {
        WARNING, ERROR
    }
    
    /**
     * 阈值监控器接口
     */
    public interface ThresholdMonitor {
        void check(PerformanceSnapshot snapshot);
    }
    
    /**
     * 性能快照类
     */
    public static class PerformanceSnapshot {
        private final long timestamp;
        private final long usedMemory;
        private final long totalMemory;
        private final long maxMemory;
        private final double memoryUsageRatio;
        private final double averageExecutionTime;
        private final double entitiesPerSecond;
        private final double errorRate;
        private final double removalRate;
        private final PerformanceMetrics.MetricsSnapshot metricsSnapshot;
        
        public PerformanceSnapshot(long timestamp, long usedMemory, long totalMemory, long maxMemory,
                                 double memoryUsageRatio, double averageExecutionTime, double entitiesPerSecond,
                                 double errorRate, double removalRate, PerformanceMetrics.MetricsSnapshot metricsSnapshot) {
            this.timestamp = timestamp;
            this.usedMemory = usedMemory;
            this.totalMemory = totalMemory;
            this.maxMemory = maxMemory;
            this.memoryUsageRatio = memoryUsageRatio;
            this.averageExecutionTime = averageExecutionTime;
            this.entitiesPerSecond = entitiesPerSecond;
            this.errorRate = errorRate;
            this.removalRate = removalRate;
            this.metricsSnapshot = metricsSnapshot;
        }
        
        // Getters
        public long getTimestamp() { return timestamp; }
        public long getUsedMemory() { return usedMemory; }
        public long getTotalMemory() { return totalMemory; }
        public long getMaxMemory() { return maxMemory; }
        public double getMemoryUsageRatio() { return memoryUsageRatio; }
        public double getAverageExecutionTime() { return averageExecutionTime; }
        public double getEntitiesPerSecond() { return entitiesPerSecond; }
        public double getErrorRate() { return errorRate; }
        public double getRemovalRate() { return removalRate; }
        public PerformanceMetrics.MetricsSnapshot getMetricsSnapshot() { return metricsSnapshot; }
    }
}