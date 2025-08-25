package com.xiaoxiao.arissweeping.performance;

import com.xiaoxiao.arissweeping.performance.PerformanceMetrics.MetricsSnapshot;
import com.xiaoxiao.arissweeping.performance.PerformanceMetrics.StrategyMetrics;
import com.xiaoxiao.arissweeping.performance.PerformanceMonitor.PerformanceSnapshot;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * 性能报告生成器
 * 负责生成各种格式的性能分析报告
 */
public class PerformanceReporter {
    
    private static final Logger logger = Logger.getLogger(PerformanceReporter.class.getName());
    
    private final PerformanceMetrics metrics;
    private final PerformanceMonitor monitor;
    
    // 报告配置
    private String reportDirectory = "reports";
    private boolean enableHtmlReports = true;
    private boolean enableCsvReports = true;
    private boolean enableJsonReports = false;
    
    // 日期格式化器
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    
    public PerformanceReporter(PerformanceMetrics metrics, PerformanceMonitor monitor) {
        this.metrics = metrics;
        this.monitor = monitor;
        
        // 确保报告目录存在
        createReportDirectory();
    }
    
    /**
     * 生成完整的性能报告
     */
    public ReportResult generateFullReport() {
        return generateFullReport("performance_report_" + fileNameFormat.format(new Date()));
    }
    
    /**
     * 生成完整的性能报告
     */
    public ReportResult generateFullReport(String reportName) {
        try {
            MetricsSnapshot snapshot = metrics.getSnapshot();
            List<PerformanceSnapshot> history = monitor.getPerformanceHistory();
            
            Map<String, String> generatedFiles = new HashMap<>();
            
            // 生成HTML报告
            if (enableHtmlReports) {
                String htmlFile = generateHtmlReport(reportName, snapshot, history);
                generatedFiles.put("html", htmlFile);
            }
            
            // 生成CSV报告
            if (enableCsvReports) {
                String csvFile = generateCsvReport(reportName, snapshot, history);
                generatedFiles.put("csv", csvFile);
            }
            
            // 生成JSON报告
            if (enableJsonReports) {
                String jsonFile = generateJsonReport(reportName, snapshot, history);
                generatedFiles.put("json", jsonFile);
            }
            
            logger.info("Performance report generated successfully: " + reportName);
            return new ReportResult(true, "Report generated successfully", generatedFiles);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to generate performance report", e);
            return new ReportResult(false, "Failed to generate report: " + e.getMessage(), Collections.emptyMap());
        }
    }
    
    /**
     * 生成HTML报告
     */
    private String generateHtmlReport(String reportName, MetricsSnapshot snapshot, List<PerformanceSnapshot> history) throws IOException {
        String fileName = reportName + ".html";
        Path filePath = Paths.get(reportDirectory, fileName);
        
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(filePath))) {
            writer.println("<!DOCTYPE html>");
            writer.println("<html lang='zh-CN'>");
            writer.println("<head>");
            writer.println("    <meta charset='UTF-8'>");
            writer.println("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>");
            writer.println("    <title>Aris-Sweeping 性能报告</title>");
            writer.println("    <style>");
            writer.println(getHtmlStyles());
            writer.println("    </style>");
            writer.println("</head>");
            writer.println("<body>");
            
            // 报告头部
            writer.println("    <div class='header'>");
            writer.println("        <h1>Aris-Sweeping 性能报告</h1>");
            writer.println("        <p>生成时间: " + dateFormat.format(new Date()) + "</p>");
            writer.println("        <p>运行时间: " + formatDuration(snapshot.getUptime()) + "</p>");
            writer.println("    </div>");
            
            // 概览部分
            writer.println("    <div class='section'>");
            writer.println("        <h2>性能概览</h2>");
            writer.println("        <div class='metrics-grid'>");
            writeMetricCard(writer, "处理实体总数", String.valueOf(snapshot.getTotalEntitiesProcessed()), "个");
            writeMetricCard(writer, "移除实体总数", String.valueOf(snapshot.getTotalEntitiesRemoved()), "个");
            writeMetricCard(writer, "处理区块总数", String.valueOf(snapshot.getTotalChunksProcessed()), "个");
            writeMetricCard(writer, "处理世界总数", String.valueOf(snapshot.getTotalWorldsProcessed()), "个");
            writeMetricCard(writer, "平均执行时间", String.format("%.2f", snapshot.getAverageExecutionTime()), "毫秒");
            writeMetricCard(writer, "实体处理速度", String.format("%.2f", snapshot.getEntitiesPerSecond()), "个/秒");
            writeMetricCard(writer, "移除率", String.format("%.2f", snapshot.getRemovalRate()), "%");
            writeMetricCard(writer, "错误率", String.format("%.2f", snapshot.getErrorRate()), "%");
            writer.println("        </div>");
            writer.println("    </div>");
            
            // 策略性能部分
            writer.println("    <div class='section'>");
            writer.println("        <h2>策略性能统计</h2>");
            writeStrategyTable(writer, snapshot.getStrategyMetrics());
            writer.println("    </div>");
            
            // 错误统计部分
            if (!snapshot.getErrorsByType().isEmpty()) {
                writer.println("    <div class='section'>");
                writer.println("        <h2>错误统计</h2>");
                writeErrorTable(writer, snapshot.getErrorsByType());
                writer.println("    </div>");
            }
            
            // 性能历史部分
            if (!history.isEmpty()) {
                writer.println("    <div class='section'>");
                writer.println("        <h2>性能历史趋势</h2>");
                writePerformanceChart(writer, history);
                writer.println("    </div>");
            }
            
            writer.println("</body>");
            writer.println("</html>");
        }
        
        return filePath.toString();
    }
    
    /**
     * 生成CSV报告
     */
    private String generateCsvReport(String reportName, MetricsSnapshot snapshot, List<PerformanceSnapshot> history) throws IOException {
        String fileName = reportName + ".csv";
        Path filePath = Paths.get(reportDirectory, fileName);
        
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(filePath))) {
            // 写入概览数据
            writer.println("# Aris-Sweeping 性能报告 - " + dateFormat.format(new Date()));
            writer.println("# 运行时间: " + formatDuration(snapshot.getUptime()));
            writer.println();
            
            writer.println("指标,数值,单位");
            writer.println("处理实体总数," + snapshot.getTotalEntitiesProcessed() + ",个");
            writer.println("移除实体总数," + snapshot.getTotalEntitiesRemoved() + ",个");
            writer.println("处理区块总数," + snapshot.getTotalChunksProcessed() + ",个");
            writer.println("处理世界总数," + snapshot.getTotalWorldsProcessed() + ",个");
            writer.println("平均执行时间," + String.format("%.2f", snapshot.getAverageExecutionTime()) + ",毫秒");
            writer.println("实体处理速度," + String.format("%.2f", snapshot.getEntitiesPerSecond()) + ",个/秒");
            writer.println("移除率," + String.format("%.2f", snapshot.getRemovalRate()) + ",%");
            writer.println("错误率," + String.format("%.2f", snapshot.getErrorRate()) + ",%");
            writer.println();
            
            // 写入策略数据
            writer.println("# 策略性能统计");
            writer.println("策略名称,执行次数,处理实体数,移除实体数,总执行时间(毫秒),平均执行时间(毫秒),成功率(%),实体处理速度(个/秒)");
            for (Map.Entry<String, StrategyMetrics> entry : snapshot.getStrategyMetrics().entrySet()) {
                StrategyMetrics sm = entry.getValue();
                writer.printf("%s,%d,%d,%d,%d,%.2f,%.2f,%.2f%n",
                    entry.getKey(),
                    sm.getExecutionCount(),
                    sm.getEntitiesProcessed(),
                    sm.getEntitiesRemoved(),
                    sm.getTotalExecutionTime(),
                    sm.getAverageExecutionTime(),
                    sm.getSuccessRate(),
                    sm.getEntitiesPerSecond());
            }
            writer.println();
            
            // 写入错误统计
            if (!snapshot.getErrorsByType().isEmpty()) {
                writer.println("# 错误统计");
                writer.println("错误类型,发生次数");
                for (Map.Entry<String, AtomicLong> entry : snapshot.getErrorsByType().entrySet()) {
                    writer.println(entry.getKey() + "," + entry.getValue().get());
                }
                writer.println();
            }
            
            // 写入历史数据
            if (!history.isEmpty()) {
                writer.println("# 性能历史数据");
                writer.println("时间戳,内存使用率(%),平均执行时间(毫秒),实体处理速度(个/秒),错误率(%)");
                for (PerformanceSnapshot ps : history) {
                    writer.printf("%s,%.2f,%.2f,%.2f,%.2f%n",
                        dateFormat.format(new Date(ps.getTimestamp())),
                        ps.getMemoryUsageRatio() * 100,
                        ps.getAverageExecutionTime(),
                        ps.getEntitiesPerSecond(),
                        ps.getErrorRate());
                }
            }
        }
        
        return filePath.toString();
    }
    
    /**
     * 生成JSON报告
     */
    private String generateJsonReport(String reportName, MetricsSnapshot snapshot, List<PerformanceSnapshot> history) throws IOException {
        String fileName = reportName + ".json";
        Path filePath = Paths.get(reportDirectory, fileName);
        
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(filePath))) {
            writer.println("{");
            writer.println("  \"reportInfo\": {");
            writer.println("    \"generatedAt\": \"" + dateFormat.format(new Date()) + "\",");
            writer.println("    \"uptime\": " + snapshot.getUptime() + ",");
            writer.println("    \"uptimeFormatted\": \"" + formatDuration(snapshot.getUptime()) + "\"");
            writer.println("  },");
            
            writer.println("  \"overview\": {");
            writer.println("    \"totalEntitiesProcessed\": " + snapshot.getTotalEntitiesProcessed() + ",");
            writer.println("    \"totalEntitiesRemoved\": " + snapshot.getTotalEntitiesRemoved() + ",");
            writer.println("    \"totalChunksProcessed\": " + snapshot.getTotalChunksProcessed() + ",");
            writer.println("    \"totalWorldsProcessed\": " + snapshot.getTotalWorldsProcessed() + ",");
            writer.println("    \"averageExecutionTime\": " + snapshot.getAverageExecutionTime() + ",");
            writer.println("    \"entitiesPerSecond\": " + snapshot.getEntitiesPerSecond() + ",");
            writer.println("    \"removalRate\": " + snapshot.getRemovalRate() + ",");
            writer.println("    \"errorRate\": " + snapshot.getErrorRate());
            writer.println("  },");
            
            // 策略数据
            writer.println("  \"strategies\": {");
            boolean first = true;
            for (Map.Entry<String, StrategyMetrics> entry : snapshot.getStrategyMetrics().entrySet()) {
                if (!first) writer.println(",");
                first = false;
                
                StrategyMetrics sm = entry.getValue();
                writer.println("    \"" + entry.getKey() + "\": {");
                writer.println("      \"executionCount\": " + sm.getExecutionCount() + ",");
                writer.println("      \"entitiesProcessed\": " + sm.getEntitiesProcessed() + ",");
                writer.println("      \"entitiesRemoved\": " + sm.getEntitiesRemoved() + ",");
                writer.println("      \"totalExecutionTime\": " + sm.getTotalExecutionTime() + ",");
                writer.println("      \"averageExecutionTime\": " + sm.getAverageExecutionTime() + ",");
                writer.println("      \"successRate\": " + sm.getSuccessRate() + ",");
                writer.println("      \"entitiesPerSecond\": " + sm.getEntitiesPerSecond());
                writer.print("    }");
            }
            writer.println();
            writer.println("  },");
            
            // 错误数据
            writer.println("  \"errors\": {");
            first = true;
            for (Map.Entry<String, AtomicLong> entry : snapshot.getErrorsByType().entrySet()) {
                if (!first) writer.println(",");
                first = false;
                writer.print("    \"" + entry.getKey() + "\": " + entry.getValue().get());
            }
            writer.println();
            writer.println("  },");
            
            // 历史数据
            writer.println("  \"history\": [");
            first = true;
            for (PerformanceSnapshot ps : history) {
                if (!first) writer.println(",");
                first = false;
                writer.println("    {");
                writer.println("      \"timestamp\": " + ps.getTimestamp() + ",");
                writer.println("      \"memoryUsageRatio\": " + ps.getMemoryUsageRatio() + ",");
                writer.println("      \"averageExecutionTime\": " + ps.getAverageExecutionTime() + ",");
                writer.println("      \"entitiesPerSecond\": " + ps.getEntitiesPerSecond() + ",");
                writer.println("      \"errorRate\": " + ps.getErrorRate());
                writer.print("    }");
            }
            writer.println();
            writer.println("  ]");
            writer.println("}");
        }
        
        return filePath.toString();
    }
    
    /**
     * 生成简要报告
     */
    public String generateSummaryReport() {
        MetricsSnapshot snapshot = metrics.getSnapshot();
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== Aris-Sweeping 性能摘要 ===").append("\n");
        sb.append("生成时间: ").append(dateFormat.format(new Date())).append("\n");
        sb.append("运行时间: ").append(formatDuration(snapshot.getUptime())).append("\n");
        sb.append("\n");
        
        sb.append("处理实体总数: ").append(snapshot.getTotalEntitiesProcessed()).append("\n");
        sb.append("移除实体总数: ").append(snapshot.getTotalEntitiesRemoved()).append("\n");
        sb.append("处理区块总数: ").append(snapshot.getTotalChunksProcessed()).append("\n");
        sb.append("处理世界总数: ").append(snapshot.getTotalWorldsProcessed()).append("\n");
        sb.append("\n");
        
        sb.append(String.format("平均执行时间: %.2f 毫秒\n", snapshot.getAverageExecutionTime()));
        sb.append(String.format("实体处理速度: %.2f 个/秒\n", snapshot.getEntitiesPerSecond()));
        sb.append(String.format("移除率: %.2f%%\n", snapshot.getRemovalRate()));
        sb.append(String.format("错误率: %.2f%%\n", snapshot.getErrorRate()));
        
        if (!snapshot.getStrategyMetrics().isEmpty()) {
            sb.append("\n=== 策略性能 ===").append("\n");
            for (Map.Entry<String, StrategyMetrics> entry : snapshot.getStrategyMetrics().entrySet()) {
                StrategyMetrics sm = entry.getValue();
                sb.append(String.format("%s: 执行%d次, 处理%d个实体, 成功率%.1f%%\n",
                    entry.getKey(), sm.getExecutionCount(), sm.getEntitiesProcessed(), sm.getSuccessRate()));
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 创建报告目录
     */
    private void createReportDirectory() {
        try {
            Path reportPath = Paths.get(reportDirectory);
            if (!Files.exists(reportPath)) {
                Files.createDirectories(reportPath);
                logger.info("Created report directory: " + reportPath.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create report directory", e);
        }
    }
    
    /**
     * 格式化持续时间
     */
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%d天 %d小时 %d分钟", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%d小时 %d分钟", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%d分钟 %d秒", minutes, seconds % 60);
        } else {
            return String.format("%d秒", seconds);
        }
    }
    
    // HTML报告辅助方法
    private String getHtmlStyles() {
        return "body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }\n" +
               ".header { background: #2c3e50; color: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; }\n" +
               ".section { background: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n" +
               ".metrics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; }\n" +
               ".metric-card { background: #ecf0f1; padding: 15px; border-radius: 6px; text-align: center; }\n" +
               ".metric-value { font-size: 24px; font-weight: bold; color: #2c3e50; }\n" +
               ".metric-label { font-size: 14px; color: #7f8c8d; margin-top: 5px; }\n" +
               "table { width: 100%; border-collapse: collapse; margin-top: 10px; }\n" +
               "th, td { padding: 10px; text-align: left; border-bottom: 1px solid #ddd; }\n" +
               "th { background-color: #34495e; color: white; }\n" +
               "tr:nth-child(even) { background-color: #f2f2f2; }";
    }
    
    private void writeMetricCard(PrintWriter writer, String label, String value, String unit) {
        writer.println("            <div class='metric-card'>");
        writer.println("                <div class='metric-value'>" + value + " <span style='font-size: 16px;'>" + unit + "</span></div>");
        writer.println("                <div class='metric-label'>" + label + "</div>");
        writer.println("            </div>");
    }
    
    private void writeStrategyTable(PrintWriter writer, Map<String, StrategyMetrics> strategies) {
        writer.println("        <table>");
        writer.println("            <tr><th>策略名称</th><th>执行次数</th><th>处理实体数</th><th>移除实体数</th><th>平均执行时间(ms)</th><th>成功率(%)</th><th>处理速度(个/秒)</th></tr>");
        
        for (Map.Entry<String, StrategyMetrics> entry : strategies.entrySet()) {
            StrategyMetrics sm = entry.getValue();
            writer.printf("            <tr><td>%s</td><td>%d</td><td>%d</td><td>%d</td><td>%.2f</td><td>%.2f</td><td>%.2f</td></tr>%n",
                entry.getKey(), sm.getExecutionCount(), sm.getEntitiesProcessed(), sm.getEntitiesRemoved(),
                sm.getAverageExecutionTime(), sm.getSuccessRate(), sm.getEntitiesPerSecond());
        }
        
        writer.println("        </table>");
    }
    
    private void writeErrorTable(PrintWriter writer, Map<String, AtomicLong> errors) {
        writer.println("        <table>");
        writer.println("            <tr><th>错误类型</th><th>发生次数</th></tr>");
        
        for (Map.Entry<String, AtomicLong> entry : errors.entrySet()) {
            writer.printf("            <tr><td>%s</td><td>%d</td></tr>%n",
                entry.getKey(), entry.getValue().get());
        }
        
        writer.println("        </table>");
    }
    
    private void writePerformanceChart(PrintWriter writer, List<PerformanceSnapshot> history) {
        writer.println("        <p>性能历史数据（最近 " + history.size() + " 个数据点）</p>");
        writer.println("        <table>");
        writer.println("            <tr><th>时间</th><th>内存使用率(%)</th><th>平均执行时间(ms)</th><th>处理速度(个/秒)</th><th>错误率(%)</th></tr>");
        
        // 只显示最近的20个数据点
        List<PerformanceSnapshot> recent = history.stream()
            .skip(Math.max(0, history.size() - 20))
            .collect(Collectors.toList());
        
        for (PerformanceSnapshot ps : recent) {
            writer.printf("            <tr><td>%s</td><td>%.2f</td><td>%.2f</td><td>%.2f</td><td>%.2f</td></tr>%n",
                dateFormat.format(new Date(ps.getTimestamp())),
                ps.getMemoryUsageRatio() * 100,
                ps.getAverageExecutionTime(),
                ps.getEntitiesPerSecond(),
                ps.getErrorRate());
        }
        
        writer.println("        </table>");
    }
    
    // Getters and Setters
    public String getReportDirectory() { return reportDirectory; }
    public void setReportDirectory(String reportDirectory) { 
        this.reportDirectory = reportDirectory;
        createReportDirectory();
    }
    
    public boolean isEnableHtmlReports() { return enableHtmlReports; }
    public void setEnableHtmlReports(boolean enableHtmlReports) { this.enableHtmlReports = enableHtmlReports; }
    
    public boolean isEnableCsvReports() { return enableCsvReports; }
    public void setEnableCsvReports(boolean enableCsvReports) { this.enableCsvReports = enableCsvReports; }
    
    public boolean isEnableJsonReports() { return enableJsonReports; }
    public void setEnableJsonReports(boolean enableJsonReports) { this.enableJsonReports = enableJsonReports; }
    
    /**
     * 报告生成结果
     */
    public static class ReportResult {
        private final boolean success;
        private final String message;
        private final Map<String, String> generatedFiles;
        
        public ReportResult(boolean success, String message, Map<String, String> generatedFiles) {
            this.success = success;
            this.message = message;
            this.generatedFiles = generatedFiles;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Map<String, String> getGeneratedFiles() { return generatedFiles; }
    }
}