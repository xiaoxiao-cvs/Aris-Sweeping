package com.xiaoxiao.arissweeping.performance;

import com.xiaoxiao.arissweeping.config.ConfigManager;
import com.xiaoxiao.arissweeping.observer.CleanupEventManager;
import com.xiaoxiao.arissweeping.performance.PerformanceAnalyzer.AnalysisResult;
import com.xiaoxiao.arissweeping.performance.PerformanceReporter.ReportResult;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 性能管理器
 * 统一管理性能监控、分析和报告功能
 */
public class PerformanceManager {
    
    private static final Logger logger = Logger.getLogger(PerformanceManager.class.getName());
    
    private final PerformanceMetrics metrics;
    private final PerformanceMonitor monitor;
    private final PerformanceAnalyzer analyzer;
    private final PerformanceReporter reporter;
    
    private final ScheduledExecutorService scheduler;
    
    // 配置参数
    private boolean autoAnalysisEnabled = true;
    private boolean autoReportEnabled = false;
    private long analysisInterval = 300; // 5分钟
    private long reportInterval = 3600; // 1小时
    
    private volatile boolean running = false;
    
    public PerformanceManager(ConfigManager configManager, CleanupEventManager eventManager) {
        this.metrics = new PerformanceMetrics();
        this.monitor = new PerformanceMonitor(metrics, eventManager, configManager);
        this.analyzer = new PerformanceAnalyzer(metrics, monitor);
        this.reporter = new PerformanceReporter(metrics, monitor);
        
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "PerformanceManager-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
        
        loadConfiguration(configManager);
    }
    
    /**
     * 启动性能管理器
     */
    public void start() {
        if (running) {
            logger.warning("Performance manager is already running");
            return;
        }
        
        running = true;
        
        // 启动性能监控
        monitor.startMonitoring();
        
        // 启动自动分析
        if (autoAnalysisEnabled) {
            scheduler.scheduleAtFixedRate(this::performAutoAnalysis, 
                analysisInterval, analysisInterval, TimeUnit.SECONDS);
            logger.info("Auto analysis scheduled every " + analysisInterval + " seconds");
        }
        
        // 启动自动报告
        if (autoReportEnabled) {
            scheduler.scheduleAtFixedRate(this::performAutoReport, 
                reportInterval, reportInterval, TimeUnit.SECONDS);
            logger.info("Auto report scheduled every " + reportInterval + " seconds");
        }
        
        logger.info("Performance manager started successfully");
    }
    
    /**
     * 停止性能管理器
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        running = false;
        
        // 停止监控
        monitor.stopMonitoring();
        
        // 关闭调度器
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("Performance manager stopped");
    }
    
    /**
     * 执行性能分析
     */
    public AnalysisResult performAnalysis() {
        try {
            AnalysisResult result = analyzer.performFullAnalysis();
            
            // 记录分析结果
            if (result.hasHighSeverityIssues()) {
                logger.warning("Performance analysis found " + 
                    result.getIssueCount(PerformanceAnalyzer.IssueSeverity.HIGH) + " high severity issues");
            }
            
            return result;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to perform performance analysis", e);
            throw new RuntimeException("Performance analysis failed", e);
        }
    }
    
    /**
     * 生成性能报告
     */
    public ReportResult generateReport() {
        return generateReport(null);
    }
    
    /**
     * 生成性能报告
     */
    public ReportResult generateReport(String reportName) {
        try {
            ReportResult result = reportName != null ? 
                reporter.generateFullReport(reportName) : 
                reporter.generateFullReport();
            
            if (result.isSuccess()) {
                logger.info("Performance report generated: " + result.getGeneratedFiles());
            } else {
                logger.warning("Failed to generate performance report: " + result.getMessage());
            }
            
            return result;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to generate performance report", e);
            throw new RuntimeException("Report generation failed", e);
        }
    }
    
    /**
     * 获取性能摘要
     */
    public String getPerformanceSummary() {
        return reporter.generateSummaryReport();
    }
    
    /**
     * 重置性能指标
     */
    public void resetMetrics() {
        metrics.reset();
        logger.info("Performance metrics reset");
    }
    
    /**
     * 更新配置
     */
    public void updateConfiguration(ConfigManager configManager) {
        loadConfiguration(configManager);
        
        // 更新子组件配置
        monitor.updateConfiguration(configManager);
        
        // 重新配置报告器
        configureReporter(configManager);
        
        // 重新配置分析器
        configureAnalyzer(configManager);
        
        logger.info("Performance manager configuration updated");
    }
    
    /**
     * 执行自动分析
     */
    private void performAutoAnalysis() {
        try {
            AnalysisResult result = analyzer.performFullAnalysis();
            
            // 如果发现高严重性问题，记录警告
            if (result.hasHighSeverityIssues()) {
                logger.warning("Auto analysis detected performance issues. Score: " + 
                    String.format("%.1f", result.getOverallScore()) + " (" + result.getScoreGrade() + ")");
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Auto analysis failed", e);
        }
    }
    
    /**
     * 执行自动报告
     */
    private void performAutoReport() {
        try {
            ReportResult result = reporter.generateFullReport();
            
            if (result.isSuccess()) {
                logger.info("Auto report generated successfully");
            } else {
                logger.warning("Auto report generation failed: " + result.getMessage());
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Auto report generation failed", e);
        }
    }
    
    /**
     * 加载配置
     */
    private void loadConfiguration(ConfigManager configManager) {
        try {
            // 性能管理器配置
            autoAnalysisEnabled = configManager.getBoolean("performance.auto-analysis.enabled", true);
            autoReportEnabled = configManager.getBoolean("performance.auto-report.enabled", false);
            analysisInterval = configManager.getLong("performance.auto-analysis.interval", 300L);
            reportInterval = configManager.getLong("performance.auto-report.interval", 3600L);
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to load performance configuration, using defaults", e);
        }
    }
    
    /**
     * 配置报告器
     */
    private void configureReporter(ConfigManager configManager) {
        try {
            String reportDir = configManager.getString("performance.report.directory", "reports");
            boolean enableHtml = configManager.getBoolean("performance.report.html.enabled", true);
            boolean enableCsv = configManager.getBoolean("performance.report.csv.enabled", true);
            boolean enableJson = configManager.getBoolean("performance.report.json.enabled", false);
            
            reporter.setReportDirectory(reportDir);
            reporter.setEnableHtmlReports(enableHtml);
            reporter.setEnableCsvReports(enableCsv);
            reporter.setEnableJsonReports(enableJson);
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to configure reporter, using defaults", e);
        }
    }
    
    /**
     * 配置分析器
     */
    private void configureAnalyzer(ConfigManager configManager) {
        try {
            double execTimeThreshold = configManager.getDouble("performance.analysis.execution-time-threshold", 100.0);
            double efficiencyThreshold = configManager.getDouble("performance.analysis.efficiency-threshold", 0.5);
            double errorRateThreshold = configManager.getDouble("performance.analysis.error-rate-threshold", 0.05);
            double memoryThreshold = configManager.getDouble("performance.analysis.memory-threshold", 0.8);
            int minSampleSize = configManager.getInt("performance.analysis.min-sample-size", 10);
            
            analyzer.setHighExecutionTimeThreshold(execTimeThreshold);
            analyzer.setLowEfficiencyThreshold(efficiencyThreshold);
            analyzer.setHighErrorRateThreshold(errorRateThreshold);
            analyzer.setHighMemoryUsageThreshold(memoryThreshold);
            analyzer.setMinSampleSize(minSampleSize);
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to configure analyzer, using defaults", e);
        }
    }
    
    // Getters
    public PerformanceMetrics getMetrics() { return metrics; }
    public PerformanceMonitor getMonitor() { return monitor; }
    public PerformanceAnalyzer getAnalyzer() { return analyzer; }
    public PerformanceReporter getReporter() { return reporter; }
    
    public boolean isRunning() { return running; }
    public boolean isAutoAnalysisEnabled() { return autoAnalysisEnabled; }
    public boolean isAutoReportEnabled() { return autoReportEnabled; }
    public long getAnalysisInterval() { return analysisInterval; }
    public long getReportInterval() { return reportInterval; }
    
    // Setters
    public void setAutoAnalysisEnabled(boolean enabled) {
        this.autoAnalysisEnabled = enabled;
        if (running) {
            // 重新启动以应用新配置
            stop();
            start();
        }
    }
    
    public void setAutoReportEnabled(boolean enabled) {
        this.autoReportEnabled = enabled;
        if (running) {
            // 重新启动以应用新配置
            stop();
            start();
        }
    }
    
    public void setAnalysisInterval(long intervalSeconds) {
        this.analysisInterval = intervalSeconds;
        if (running && autoAnalysisEnabled) {
            // 重新启动以应用新配置
            stop();
            start();
        }
    }
    
    public void setReportInterval(long intervalSeconds) {
        this.reportInterval = intervalSeconds;
        if (running && autoReportEnabled) {
            // 重新启动以应用新配置
            stop();
            start();
        }
    }
    
    /**
     * 获取性能状态信息
     */
    public PerformanceStatus getStatus() {
        return new PerformanceStatus(
            running,
            monitor.isMonitoring(),
            autoAnalysisEnabled,
            autoReportEnabled,
            metrics.getSnapshot().getUptime(),
            monitor.getPerformanceHistory().size()
        );
    }
    
    /**
     * 性能状态信息
     */
    public static class PerformanceStatus {
        private final boolean managerRunning;
        private final boolean monitorRunning;
        private final boolean autoAnalysisEnabled;
        private final boolean autoReportEnabled;
        private final long uptime;
        private final int historySize;
        
        public PerformanceStatus(boolean managerRunning, boolean monitorRunning, 
                               boolean autoAnalysisEnabled, boolean autoReportEnabled,
                               long uptime, int historySize) {
            this.managerRunning = managerRunning;
            this.monitorRunning = monitorRunning;
            this.autoAnalysisEnabled = autoAnalysisEnabled;
            this.autoReportEnabled = autoReportEnabled;
            this.uptime = uptime;
            this.historySize = historySize;
        }
        
        // Getters
        public boolean isManagerRunning() { return managerRunning; }
        public boolean isMonitorRunning() { return monitorRunning; }
        public boolean isAutoAnalysisEnabled() { return autoAnalysisEnabled; }
        public boolean isAutoReportEnabled() { return autoReportEnabled; }
        public long getUptime() { return uptime; }
        public int getHistorySize() { return historySize; }
        
        @Override
        public String toString() {
            return String.format("PerformanceStatus{manager=%s, monitor=%s, autoAnalysis=%s, autoReport=%s, uptime=%d, history=%d}",
                managerRunning, monitorRunning, autoAnalysisEnabled, autoReportEnabled, uptime, historySize);
        }
    }
}