package com.arisweeping.async;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * 线程安全的计数器工具类
 * 提供高性能的原子操作和统计数据收集功能
 */
public class ThreadSafeCounter {
    
    /**
     * 计数器统计信息
     */
    public static class CounterStatistics {
        private final String name;
        private final long totalCount;
        private final double averageRate; // 每秒平均计数
        private final long peakRate; // 峰值计数率
        private final Instant creationTime;
        private final Instant lastUpdateTime;
        
        public CounterStatistics(String name, long totalCount, double averageRate, 
                               long peakRate, Instant creationTime, Instant lastUpdateTime) {
            this.name = name;
            this.totalCount = totalCount;
            this.averageRate = averageRate;
            this.peakRate = peakRate;
            this.creationTime = creationTime;
            this.lastUpdateTime = lastUpdateTime;
        }
        
        // Getters
        public String getName() { return name; }
        public long getTotalCount() { return totalCount; }
        public double getAverageRate() { return averageRate; }
        public long getPeakRate() { return peakRate; }
        public Instant getCreationTime() { return creationTime; }
        public Instant getLastUpdateTime() { return lastUpdateTime; }
        
        @Override
        public String toString() {
            return String.format("CounterStatistics{name='%s', total=%d, avgRate=%.2f/s, peak=%d/s}", 
                               name, totalCount, averageRate, peakRate);
        }
    }
    
    /**
     * 命名计数器
     */
    public static class NamedCounter {
        private final String name;
        private final LongAdder counter;
        private final AtomicLong lastUpdateTime;
        private final Instant creationTime;
        private final ReentrantReadWriteLock lock;
        
        // 用于计算速率的滑动窗口
        private final Map<Long, Long> rateWindow;
        private static final int RATE_WINDOW_SECONDS = 60;
        
        public NamedCounter(String name) {
            this.name = name;
            this.counter = new LongAdder();
            this.lastUpdateTime = new AtomicLong(System.currentTimeMillis());
            this.creationTime = Instant.now();
            this.lock = new ReentrantReadWriteLock();
            this.rateWindow = new ConcurrentHashMap<>();
        }
        
        /**
         * 增加计数
         */
        public void increment() {
            add(1);
        }
        
        /**
         * 增加指定值
         */
        public void add(long value) {
            counter.add(value);
            long now = System.currentTimeMillis();
            lastUpdateTime.set(now);
            
            // 更新速率窗口
            updateRateWindow(now, value);
        }
        
        /**
         * 获取当前计数
         */
        public long get() {
            return counter.sum();
        }
        
        /**
         * 重置计数器
         */
        public void reset() {
            lock.writeLock().lock();
            try {
                counter.reset();
                lastUpdateTime.set(System.currentTimeMillis());
                rateWindow.clear();
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        /**
         * 获取统计信息
         */
        public CounterStatistics getStatistics() {
            lock.readLock().lock();
            try {
                long total = counter.sum();
                Instant lastUpdate = Instant.ofEpochMilli(lastUpdateTime.get());
                
                // 计算平均速率
                double averageRate = calculateAverageRate();
                
                // 计算峰值速率
                long peakRate = calculatePeakRate();
                
                return new CounterStatistics(name, total, averageRate, peakRate, 
                                           creationTime, lastUpdate);
            } finally {
                lock.readLock().unlock();
            }
        }
        
        private void updateRateWindow(long timestamp, long value) {
            long secondKey = timestamp / 1000;
            
            lock.writeLock().lock();
            try {
                rateWindow.merge(secondKey, value, Long::sum);
                
                // 清理过期数据
                long cutoffTime = secondKey - RATE_WINDOW_SECONDS;
                rateWindow.entrySet().removeIf(entry -> entry.getKey() < cutoffTime);
                
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        private double calculateAverageRate() {
            if (rateWindow.isEmpty()) return 0.0;
            
            long totalValue = rateWindow.values().stream().mapToLong(Long::longValue).sum();
            return (double) totalValue / Math.min(rateWindow.size(), RATE_WINDOW_SECONDS);
        }
        
        private long calculatePeakRate() {
            return rateWindow.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        }
    }
    
    // 全局计数器注册表
    private static final Map<String, NamedCounter> counters = new ConcurrentHashMap<>();
    
    /**
     * 获取或创建命名计数器
     */
    public static NamedCounter getCounter(String name) {
        return counters.computeIfAbsent(name, NamedCounter::new);
    }
    
    /**
     * 移除命名计数器
     */
    public static boolean removeCounter(String name) {
        return counters.remove(name) != null;
    }
    
    /**
     * 获取所有计数器名称
     */
    public static java.util.Set<String> getCounterNames() {
        return counters.keySet();
    }
    
    /**
     * 获取所有计数器的统计信息
     */
    public static Map<String, CounterStatistics> getAllStatistics() {
        Map<String, CounterStatistics> stats = new ConcurrentHashMap<>();
        counters.forEach((name, counter) -> stats.put(name, counter.getStatistics()));
        return stats;
    }
    
    /**
     * 清理所有计数器
     */
    public static void clearAll() {
        counters.clear();
    }
    
    /**
     * 重置所有计数器
     */
    public static void resetAll() {
        counters.values().forEach(NamedCounter::reset);
    }
    
    // 便捷方法
    
    /**
     * 快速增加计数
     */
    public static void increment(String counterName) {
        getCounter(counterName).increment();
    }
    
    /**
     * 快速增加指定值
     */
    public static void add(String counterName, long value) {
        getCounter(counterName).add(value);
    }
    
    /**
     * 快速获取计数
     */
    public static long get(String counterName) {
        return getCounter(counterName).get();
    }
    
    /**
     * 快速获取统计信息
     */
    public static CounterStatistics getStatistics(String counterName) {
        NamedCounter counter = counters.get(counterName);
        return counter != null ? counter.getStatistics() : null;
    }
    
    // 预定义的计数器常量
    public static final String ENTITIES_PROCESSED = "entities_processed";
    public static final String ENTITIES_REMOVED = "entities_removed";
    public static final String CHUNKS_PROCESSED = "chunks_processed";
    public static final String TASKS_EXECUTED = "tasks_executed";
    public static final String TASKS_FAILED = "tasks_failed";
    public static final String UNDO_OPERATIONS = "undo_operations";
    public static final String UNDO_SUCCESSES = "undo_successes";
    public static final String UNDO_FAILURES = "undo_failures";
    
    /**
     * 初始化预定义计数器
     */
    public static void initializeDefaultCounters() {
        // 确保预定义计数器存在
        getCounter(ENTITIES_PROCESSED);
        getCounter(ENTITIES_REMOVED);
        getCounter(CHUNKS_PROCESSED);
        getCounter(TASKS_EXECUTED);
        getCounter(TASKS_FAILED);
        getCounter(UNDO_OPERATIONS);
        getCounter(UNDO_SUCCESSES);
        getCounter(UNDO_FAILURES);
    }
    
    /**
     * 获取性能摘要报告
     */
    public static String getPerformanceSummary() {
        StringBuilder summary = new StringBuilder("=== ThreadSafeCounter Performance Summary ===\n");
        
        Map<String, CounterStatistics> allStats = getAllStatistics();
        if (allStats.isEmpty()) {
            summary.append("No counters registered.\n");
            return summary.toString();
        }
        
        // 按名称排序
        allStats.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    CounterStatistics stats = entry.getValue();
                    summary.append(String.format("%-20s: %8d total, %6.2f/s avg, %6d/s peak\n",
                            stats.getName(),
                            stats.getTotalCount(),
                            stats.getAverageRate(),
                            stats.getPeakRate()));
                });
        
        return summary.toString();
    }
    
    /**
     * 启动定期清理任务
     * 清理过期的速率窗口数据
     */
    public static void startCleanupTask(AsyncTaskManager asyncManager) {
        if (asyncManager != null) {
            asyncManager.scheduleAtFixedRate(() -> {
                // 让每个计数器自己清理过期数据
                counters.values().forEach(counter -> {
                    // 触发一次速率窗口更新以清理过期数据
                    counter.updateRateWindow(System.currentTimeMillis(), 0);
                });
            }, 60, 60, TimeUnit.SECONDS);
        }
    }
}