package com.arisweeping.data;

import com.arisweeping.core.ArisLogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 统计数据收集器
 * 负责收集、存储和管理模组运行统计信息
 */
public class StatisticsCollector {
    
    private static final String STATS_FILE = "statistics.json";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
    
    private static StatisticsCollector instance;
    private static Path statsPath;
    
    // 统计数据存储
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final Map<String, Object> metrics = new ConcurrentHashMap<>();
    private final List<StatisticsRecord> history = Collections.synchronizedList(new ArrayList<>());
    
    // 统计记录类
    public static class StatisticsRecord {
        private final String timestamp;
        private final String event;
        private final String category;
        private final Map<String, Object> data;
        
        public StatisticsRecord(String event, String category, Map<String, Object> data) {
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            this.event = event;
            this.category = category;
            this.data = new HashMap<>(data != null ? data : Collections.emptyMap());
        }
        
        // Getters
        public String getTimestamp() { return timestamp; }
        public String getEvent() { return event; }
        public String getCategory() { return category; }
        public Map<String, Object> getData() { return new HashMap<>(data); }
    }
    
    // 统计数据快照类
    public static class StatisticsSnapshot {
        private final String timestamp;
        private final Map<String, Long> counters;
        private final Map<String, Object> metrics;
        private final int historySize;
        
        public StatisticsSnapshot(Map<String, AtomicLong> counters, Map<String, Object> metrics, int historySize) {
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            this.counters = new HashMap<>();
            counters.forEach((key, value) -> this.counters.put(key, value.get()));
            this.metrics = new HashMap<>(metrics);
            this.historySize = historySize;
        }
        
        // Getters
        public String getTimestamp() { return timestamp; }
        public Map<String, Long> getCounters() { return new HashMap<>(counters); }
        public Map<String, Object> getMetrics() { return new HashMap<>(metrics); }
        public int getHistorySize() { return historySize; }
    }
    
    private StatisticsCollector() {
        initializeCounters();
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized StatisticsCollector getInstance() {
        if (instance == null) {
            instance = new StatisticsCollector();
        }
        return instance;
    }
    
    /**
     * 初始化统计收集器
     */
    public static void initialize() {
        try {
            // 获取统计文件路径
            Path configDir = Paths.get("config", "aris-sweeping");
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            
            statsPath = configDir.resolve(STATS_FILE);
            
            // 获取实例并加载历史数据
            StatisticsCollector collector = getInstance();
            collector.loadStatistics();
            
            ArisLogger.info("统计收集器初始化完成，文件路径: " + statsPath.toString());
            
        } catch (IOException e) {
            ArisLogger.error("初始化统计收集器失败", e);
            throw new RuntimeException("统计收集器初始化失败", e);
        }
    }
    
    /**
     * 初始化计数器
     */
    private void initializeCounters() {
        // 清理统计
        counters.put("items_cleaned", new AtomicLong(0));
        counters.put("animals_cleaned", new AtomicLong(0));
        counters.put("total_entities_processed", new AtomicLong(0));
        
        // 任务统计
        counters.put("tasks_executed", new AtomicLong(0));
        counters.put("tasks_successful", new AtomicLong(0));
        counters.put("tasks_failed", new AtomicLong(0));
        
        // 撤销统计
        counters.put("undo_operations", new AtomicLong(0));
        counters.put("undo_successful", new AtomicLong(0));
        counters.put("undo_failed", new AtomicLong(0));
        
        // 性能统计
        counters.put("chunk_processed", new AtomicLong(0));
        counters.put("async_tasks_completed", new AtomicLong(0));
    }
    
    /**
     * 增加计数器
     */
    public void incrementCounter(String counterName) {
        incrementCounter(counterName, 1);
    }
    
    /**
     * 增加计数器指定数值
     */
    public void incrementCounter(String counterName, long value) {
        counters.computeIfAbsent(counterName, k -> new AtomicLong(0)).addAndGet(value);
    }
    
    /**
     * 获取计数器值
     */
    public long getCounterValue(String counterName) {
        AtomicLong counter = counters.get(counterName);
        return counter != null ? counter.get() : 0;
    }
    
    /**
     * 设置度量值
     */
    public void setMetric(String metricName, Object value) {
        metrics.put(metricName, value);
    }
    
    /**
     * 获取度量值
     */
    public Object getMetric(String metricName) {
        return metrics.get(metricName);
    }
    
    /**
     * 记录事件
     */
    public void recordEvent(String event, String category) {
        recordEvent(event, category, null);
    }
    
    /**
     * 记录事件，带数据
     */
    public void recordEvent(String event, String category, Map<String, Object> data) {
        StatisticsRecord record = new StatisticsRecord(event, category, data);
        history.add(record);
        
        // 限制历史记录数量，防止内存溢出
        if (history.size() > 1000) {
            synchronized (history) {
                while (history.size() > 800) { // 保留最近800条记录
                    history.remove(0);
                }
            }
        }
        
        ArisLogger.debug("记录统计事件: " + event + " (类别: " + category + ")");
    }
    
    /**
     * 获取统计快照
     */
    public StatisticsSnapshot getSnapshot() {
        return new StatisticsSnapshot(counters, metrics, history.size());
    }
    
    /**
     * 获取历史记录
     */
    public List<StatisticsRecord> getHistory() {
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }
    
    /**
     * 获取最近的历史记录
     */
    public List<StatisticsRecord> getRecentHistory(int limit) {
        synchronized (history) {
            int size = history.size();
            int startIndex = Math.max(0, size - limit);
            return new ArrayList<>(history.subList(startIndex, size));
        }
    }
    
    /**
     * 保存统计数据到文件
     */
    public boolean saveStatistics() {
        if (statsPath == null) {
            ArisLogger.error("统计文件路径未初始化");
            return false;
        }
        
        try {
            StatisticsSnapshot snapshot = getSnapshot();
            
            // 创建临时文件
            Path tempPath = Paths.get(statsPath.toString() + ".tmp");
            
            // 将统计数据转换为JSON并写入临时文件
            String jsonContent = GSON.toJson(snapshot);
            try (FileWriter writer = new FileWriter(tempPath.toFile(), StandardCharsets.UTF_8)) {
                writer.write(jsonContent);
                writer.flush();
            }
            
            // 原子性替换原文件
            Files.move(tempPath, statsPath);
            
            ArisLogger.debug("统计数据已保存到: " + statsPath.toString());
            return true;
            
        } catch (IOException e) {
            ArisLogger.error("保存统计数据失败", e);
            return false;
        }
    }
    
    /**
     * 从文件加载统计数据
     */
    private void loadStatistics() {
        if (statsPath == null || !Files.exists(statsPath)) {
            ArisLogger.info("统计文件不存在，使用默认统计数据");
            return;
        }
        
        try {
            // 读取统计文件
            String jsonContent;
            try (FileReader reader = new FileReader(statsPath.toFile(), StandardCharsets.UTF_8)) {
                StringBuilder sb = new StringBuilder();
                char[] buffer = new char[1024];
                int length;
                while ((length = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, length);
                }
                jsonContent = sb.toString();
            }
            
            // 解析JSON到统计快照
            StatisticsSnapshot snapshot = GSON.fromJson(jsonContent, StatisticsSnapshot.class);
            
            if (snapshot != null) {
                // 恢复计数器
                snapshot.getCounters().forEach((key, value) -> {
                    counters.computeIfAbsent(key, k -> new AtomicLong(0)).set(value);
                });
                
                // 恢复度量值
                metrics.putAll(snapshot.getMetrics());
                
                ArisLogger.info("成功加载统计数据，包含 " + snapshot.getCounters().size() + " 个计数器");
            }
            
        } catch (JsonParseException e) {
            ArisLogger.error("统计文件JSON解析失败", e);
        } catch (IOException e) {
            ArisLogger.error("加载统计文件失败", e);
        }
    }
    
    /**
     * 重置所有统计数据
     */
    public void resetStatistics() {
        counters.clear();
        metrics.clear();
        synchronized (history) {
            history.clear();
        }
        initializeCounters();
        
        recordEvent("statistics_reset", "system");
        ArisLogger.info("统计数据已重置");
    }
    
    /**
     * 获取统计摘要
     */
    public Map<String, Object> getStatisticsSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        // 基本统计
        summary.put("items_cleaned", getCounterValue("items_cleaned"));
        summary.put("animals_cleaned", getCounterValue("animals_cleaned"));
        summary.put("total_entities_processed", getCounterValue("total_entities_processed"));
        
        // 任务统计
        summary.put("tasks_executed", getCounterValue("tasks_executed"));
        summary.put("tasks_successful", getCounterValue("tasks_successful"));
        summary.put("tasks_failed", getCounterValue("tasks_failed"));
        
        // 计算成功率
        long total = getCounterValue("tasks_executed");
        long successful = getCounterValue("tasks_successful");
        if (total > 0) {
            double successRate = (double) successful / total * 100;
            summary.put("task_success_rate", Math.round(successRate * 100.0) / 100.0);
        } else {
            summary.put("task_success_rate", 0.0);
        }
        
        // 撤销统计
        summary.put("undo_operations", getCounterValue("undo_operations"));
        
        // 历史记录数量
        summary.put("history_records", history.size());
        
        return summary;
    }
    
    /**
     * 定期保存统计数据（由外部调度器调用）
     */
    public void periodicSave() {
        try {
            saveStatistics();
        } catch (Exception e) {
            ArisLogger.error("定期保存统计数据失败", e);
        }
    }
}