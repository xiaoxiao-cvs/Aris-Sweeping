package com.xiaoxiao.arissweeping.performance;

import com.xiaoxiao.arissweeping.performance.PerformanceMetrics.MetricsSnapshot;
import com.xiaoxiao.arissweeping.performance.PerformanceMetrics.StrategyMetrics;
import com.xiaoxiao.arissweeping.performance.PerformanceMonitor.PerformanceSnapshot;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 性能分析器
 * 负责分析性能数据，识别瓶颈，提供优化建议
 */
public class PerformanceAnalyzer {
    
    private static final Logger logger = Logger.getLogger(PerformanceAnalyzer.class.getName());
    
    private final PerformanceMetrics metrics;
    private final PerformanceMonitor monitor;
    
    // 分析阈值配置
    private double highExecutionTimeThreshold = 100.0; // 毫秒
    private double lowEfficiencyThreshold = 0.5; // 50%
    private double highErrorRateThreshold = 0.05; // 5%
    private double highMemoryUsageThreshold = 0.8; // 80%
    private int minSampleSize = 10; // 最小样本数量
    
    public PerformanceAnalyzer(PerformanceMetrics metrics, PerformanceMonitor monitor) {
        this.metrics = metrics;
        this.monitor = monitor;
    }
    
    /**
     * 执行完整的性能分析
     */
    public AnalysisResult performFullAnalysis() {
        MetricsSnapshot snapshot = metrics.getSnapshot();
        List<PerformanceSnapshot> history = monitor.getPerformanceHistory();
        
        AnalysisResult result = new AnalysisResult();
        
        // 分析整体性能
        analyzeOverallPerformance(snapshot, result);
        
        // 分析策略性能
        analyzeStrategyPerformance(snapshot, result);
        
        // 分析错误模式
        analyzeErrorPatterns(snapshot, result);
        
        // 分析性能趋势
        analyzePerformanceTrends(history, result);
        
        // 生成优化建议
        generateOptimizationRecommendations(snapshot, history, result);
        
        // 计算性能评分
        calculatePerformanceScore(snapshot, result);
        
        logger.info("Performance analysis completed. Score: " + result.getOverallScore());
        return result;
    }
    
    /**
     * 分析整体性能
     */
    private void analyzeOverallPerformance(MetricsSnapshot snapshot, AnalysisResult result) {
        // 执行时间分析
        if (snapshot.getAverageExecutionTime() > highExecutionTimeThreshold) {
            result.addIssue(new PerformanceIssue(
                IssueSeverity.HIGH,
                "执行时间过长",
                String.format("平均执行时间 %.2f ms 超过阈值 %.2f ms", 
                    snapshot.getAverageExecutionTime(), highExecutionTimeThreshold),
                "考虑优化算法或增加批处理大小"
            ));
        }
        
        // 效率分析
        double efficiency = snapshot.getTotalEntitiesRemoved() > 0 ? 
            (double) snapshot.getTotalEntitiesRemoved() / snapshot.getTotalEntitiesProcessed() : 0;
        
        if (efficiency < lowEfficiencyThreshold) {
            result.addIssue(new PerformanceIssue(
                IssueSeverity.MEDIUM,
                "清理效率低",
                String.format("实体移除率 %.2f%% 低于预期 %.2f%%", 
                    efficiency * 100, lowEfficiencyThreshold * 100),
                "检查清理策略配置，可能需要调整清理条件"
            ));
        }
        
        // 错误率分析
        if (snapshot.getErrorRate() > highErrorRateThreshold) {
            result.addIssue(new PerformanceIssue(
                IssueSeverity.HIGH,
                "错误率过高",
                String.format("错误率 %.2f%% 超过阈值 %.2f%%", 
                    snapshot.getErrorRate(), highErrorRateThreshold * 100),
                "检查日志以识别错误原因，可能需要修复代码或调整配置"
            ));
        }
        
        // 处理速度分析
        if (snapshot.getEntitiesPerSecond() < 10.0) {
            result.addIssue(new PerformanceIssue(
                IssueSeverity.MEDIUM,
                "处理速度慢",
                String.format("实体处理速度 %.2f 个/秒 较低", snapshot.getEntitiesPerSecond()),
                "考虑优化实体检查逻辑或增加并发处理"
            ));
        }
    }
    
    /**
     * 分析策略性能
     */
    private void analyzeStrategyPerformance(MetricsSnapshot snapshot, AnalysisResult result) {
        Map<String, StrategyMetrics> strategies = snapshot.getStrategyMetrics();
        
        if (strategies.isEmpty()) {
            result.addIssue(new PerformanceIssue(
                IssueSeverity.LOW,
                "无策略数据",
                "没有策略执行数据可供分析",
                "确保策略正常运行并收集性能数据"
            ));
            return;
        }
        
        // 找出性能最差的策略
        String slowestStrategy = null;
        double maxExecutionTime = 0;
        
        String leastEffectiveStrategy = null;
        double minSuccessRate = 100.0;
        
        for (Map.Entry<String, StrategyMetrics> entry : strategies.entrySet()) {
            String strategyName = entry.getKey();
            StrategyMetrics sm = entry.getValue();
            
            // 检查执行时间
            if (sm.getAverageExecutionTime() > maxExecutionTime) {
                maxExecutionTime = sm.getAverageExecutionTime();
                slowestStrategy = strategyName;
            }
            
            // 检查成功率
            if (sm.getSuccessRate() < minSuccessRate && sm.getExecutionCount() >= minSampleSize) {
                minSuccessRate = sm.getSuccessRate();
                leastEffectiveStrategy = strategyName;
            }
            
            // 检查个别策略的问题
            if (sm.getAverageExecutionTime() > highExecutionTimeThreshold) {
                result.addIssue(new PerformanceIssue(
                    IssueSeverity.MEDIUM,
                    "策略执行时间过长",
                    String.format("策略 '%s' 平均执行时间 %.2f ms 过长", 
                        strategyName, sm.getAverageExecutionTime()),
                    "优化该策略的实现或调整其配置参数"
                ));
            }
            
            if (sm.getSuccessRate() < 90.0 && sm.getExecutionCount() >= minSampleSize) {
                result.addIssue(new PerformanceIssue(
                    IssueSeverity.MEDIUM,
                    "策略成功率低",
                    String.format("策略 '%s' 成功率 %.2f%% 较低", 
                        strategyName, sm.getSuccessRate()),
                    "检查该策略的错误日志，可能需要修复实现问题"
                ));
            }
        }
        
        // 添加策略比较分析
        if (slowestStrategy != null && maxExecutionTime > highExecutionTimeThreshold) {
            result.addInsight("最慢策略: " + slowestStrategy + 
                " (平均执行时间: " + String.format("%.2f", maxExecutionTime) + " ms)");
        }
        
        if (leastEffectiveStrategy != null && minSuccessRate < 95.0) {
            result.addInsight("效果最差策略: " + leastEffectiveStrategy + 
                " (成功率: " + String.format("%.2f", minSuccessRate) + "%)");
        }
    }
    
    /**
     * 分析错误模式
     */
    private void analyzeErrorPatterns(MetricsSnapshot snapshot, AnalysisResult result) {
        Map<String, AtomicLong> errors = snapshot.getErrorsByType();
        
        if (errors.isEmpty()) {
            result.addInsight("无错误记录 - 系统运行稳定");
            return;
        }
        
        // 找出最常见的错误
        String mostCommonError = null;
        long maxErrorCount = 0;
        long totalErrors = 0;
        
        for (Map.Entry<String, AtomicLong> entry : errors.entrySet()) {
            long count = entry.getValue().get();
            totalErrors += count;
            
            if (count > maxErrorCount) {
                maxErrorCount = count;
                mostCommonError = entry.getKey();
            }
        }
        
        if (mostCommonError != null) {
            double errorPercentage = (double) maxErrorCount / totalErrors * 100;
            
            if (errorPercentage > 50.0) {
                result.addIssue(new PerformanceIssue(
                    IssueSeverity.HIGH,
                    "主要错误类型",
                    String.format("错误类型 '%s' 占总错误的 %.1f%% (%d次)", 
                        mostCommonError, errorPercentage, maxErrorCount),
                    "重点解决这个主要错误类型以显著改善系统稳定性"
                ));
            }
        }
        
        // 分析错误多样性
        if (errors.size() > 5) {
            result.addIssue(new PerformanceIssue(
                IssueSeverity.MEDIUM,
                "错误类型多样",
                String.format("发现 %d 种不同的错误类型", errors.size()),
                "系统可能存在多个问题，建议进行全面的代码审查"
            ));
        }
    }
    
    /**
     * 分析性能趋势
     */
    private void analyzePerformanceTrends(List<PerformanceSnapshot> history, AnalysisResult result) {
        if (history.size() < 5) {
            result.addInsight("历史数据不足，无法进行趋势分析");
            return;
        }
        
        // 分析最近的趋势（最后10个数据点）
        List<PerformanceSnapshot> recent = history.stream()
            .skip(Math.max(0, history.size() - 10))
            .collect(Collectors.toList());
        
        // 内存使用趋势
        double memoryTrend = calculateTrend(recent, PerformanceSnapshot::getMemoryUsageRatio);
        if (memoryTrend > 0.1) {
            result.addIssue(new PerformanceIssue(
                IssueSeverity.MEDIUM,
                "内存使用上升",
                "内存使用率呈上升趋势",
                "监控内存泄漏，考虑增加垃圾回收频率或优化内存使用"
            ));
        }
        
        // 执行时间趋势
        double timeTrend = calculateTrend(recent, PerformanceSnapshot::getAverageExecutionTime);
        if (timeTrend > 5.0) {
            result.addIssue(new PerformanceIssue(
                IssueSeverity.MEDIUM,
                "执行时间增长",
                "平均执行时间呈上升趋势",
                "系统性能可能在下降，检查是否有性能回归"
            ));
        }
        
        // 错误率趋势
        double errorTrend = calculateTrend(recent, PerformanceSnapshot::getErrorRate);
        if (errorTrend > 0.01) {
            result.addIssue(new PerformanceIssue(
                IssueSeverity.HIGH,
                "错误率上升",
                "错误率呈上升趋势",
                "系统稳定性可能在下降，需要立即调查"
            ));
        }
        
        // 处理速度趋势
        double speedTrend = calculateTrend(recent, PerformanceSnapshot::getEntitiesPerSecond);
        if (speedTrend < -1.0) {
            result.addIssue(new PerformanceIssue(
                IssueSeverity.MEDIUM,
                "处理速度下降",
                "实体处理速度呈下降趋势",
                "检查是否有性能瓶颈或资源竞争"
            ));
        }
    }
    
    /**
     * 生成优化建议
     */
    private void generateOptimizationRecommendations(MetricsSnapshot snapshot, 
                                                   List<PerformanceSnapshot> history, 
                                                   AnalysisResult result) {
        List<String> recommendations = new ArrayList<>();
        
        // 基于当前性能指标的建议
        if (snapshot.getAverageExecutionTime() > 50.0) {
            recommendations.add("考虑实现异步处理以减少执行时间");
            recommendations.add("优化数据库查询和实体检索逻辑");
        }
        
        if (snapshot.getEntitiesPerSecond() < 20.0) {
            recommendations.add("增加批处理大小以提高吞吐量");
            recommendations.add("考虑使用多线程并行处理");
        }
        
        if (snapshot.getErrorRate() > 0.02) {
            recommendations.add("加强输入验证和错误处理");
            recommendations.add("实现更好的异常恢复机制");
        }
        
        // 基于策略性能的建议
        Map<String, StrategyMetrics> strategies = snapshot.getStrategyMetrics();
        if (!strategies.isEmpty()) {
            double avgExecutionTime = strategies.values().stream()
                .mapToDouble(StrategyMetrics::getAverageExecutionTime)
                .average().orElse(0.0);
            
            if (avgExecutionTime > 30.0) {
                recommendations.add("优化策略实现，减少不必要的计算");
                recommendations.add("考虑缓存策略结果以避免重复计算");
            }
        }
        
        // 基于历史趋势的建议
        if (!history.isEmpty()) {
            PerformanceSnapshot latest = history.get(history.size() - 1);
            if (latest.getMemoryUsageRatio() > highMemoryUsageThreshold) {
                recommendations.add("监控内存使用，考虑增加堆内存或优化内存管理");
                recommendations.add("实现更频繁的垃圾回收或对象池");
            }
        }
        
        // 通用优化建议
        recommendations.add("定期监控性能指标并设置告警");
        recommendations.add("考虑实现性能基准测试以跟踪改进效果");
        recommendations.add("优化配置参数以适应当前服务器环境");
        
        result.setRecommendations(recommendations);
    }
    
    /**
     * 计算性能评分
     */
    private void calculatePerformanceScore(MetricsSnapshot snapshot, AnalysisResult result) {
        double score = 100.0;
        
        // 执行时间评分 (30%)
        double timeScore = Math.max(0, 100 - (snapshot.getAverageExecutionTime() / highExecutionTimeThreshold) * 30);
        
        // 效率评分 (25%)
        double efficiency = snapshot.getTotalEntitiesRemoved() > 0 ? 
            (double) snapshot.getTotalEntitiesRemoved() / snapshot.getTotalEntitiesProcessed() : 0;
        double efficiencyScore = Math.min(100, efficiency / lowEfficiencyThreshold * 25);
        
        // 错误率评分 (25%)
        double errorScore = Math.max(0, 25 - (snapshot.getErrorRate() / highErrorRateThreshold) * 25);
        
        // 处理速度评分 (20%)
        double speedScore = Math.min(20, snapshot.getEntitiesPerSecond() / 50.0 * 20);
        
        score = timeScore + efficiencyScore + errorScore + speedScore;
        
        // 根据问题严重程度调整评分
        for (PerformanceIssue issue : result.getIssues()) {
            switch (issue.getSeverity()) {
                case HIGH:
                    score -= 10;
                    break;
                case MEDIUM:
                    score -= 5;
                    break;
                case LOW:
                    score -= 2;
                    break;
            }
        }
        
        result.setOverallScore(Math.max(0, Math.min(100, score)));
        
        // 设置评分等级
        if (score >= 90) {
            result.setScoreGrade("优秀");
        } else if (score >= 80) {
            result.setScoreGrade("良好");
        } else if (score >= 70) {
            result.setScoreGrade("一般");
        } else if (score >= 60) {
            result.setScoreGrade("较差");
        } else {
            result.setScoreGrade("差");
        }
    }
    
    /**
     * 计算趋势（简单线性回归斜率）
     */
    private double calculateTrend(List<PerformanceSnapshot> data, 
                                java.util.function.ToDoubleFunction<PerformanceSnapshot> valueExtractor) {
        if (data.size() < 2) return 0.0;
        
        double n = data.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < data.size(); i++) {
            double x = i;
            double y = valueExtractor.applyAsDouble(data.get(i));
            
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        
        // 计算斜率
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        return slope;
    }
    
    // Getters and Setters
    public double getHighExecutionTimeThreshold() { return highExecutionTimeThreshold; }
    public void setHighExecutionTimeThreshold(double threshold) { this.highExecutionTimeThreshold = threshold; }
    
    public double getLowEfficiencyThreshold() { return lowEfficiencyThreshold; }
    public void setLowEfficiencyThreshold(double threshold) { this.lowEfficiencyThreshold = threshold; }
    
    public double getHighErrorRateThreshold() { return highErrorRateThreshold; }
    public void setHighErrorRateThreshold(double threshold) { this.highErrorRateThreshold = threshold; }
    
    public double getHighMemoryUsageThreshold() { return highMemoryUsageThreshold; }
    public void setHighMemoryUsageThreshold(double threshold) { this.highMemoryUsageThreshold = threshold; }
    
    public int getMinSampleSize() { return minSampleSize; }
    public void setMinSampleSize(int size) { this.minSampleSize = size; }
    
    /**
     * 分析结果
     */
    public static class AnalysisResult {
        private final List<PerformanceIssue> issues = new ArrayList<>();
        private final List<String> insights = new ArrayList<>();
        private List<String> recommendations = new ArrayList<>();
        private double overallScore = 0.0;
        private String scoreGrade = "未评分";
        private final long analysisTime = System.currentTimeMillis();
        
        public void addIssue(PerformanceIssue issue) {
            issues.add(issue);
        }
        
        public void addInsight(String insight) {
            insights.add(insight);
        }
        
        // Getters and Setters
        public List<PerformanceIssue> getIssues() { return new ArrayList<>(issues); }
        public List<String> getInsights() { return new ArrayList<>(insights); }
        public List<String> getRecommendations() { return new ArrayList<>(recommendations); }
        public void setRecommendations(List<String> recommendations) { this.recommendations = new ArrayList<>(recommendations); }
        
        public double getOverallScore() { return overallScore; }
        public void setOverallScore(double score) { this.overallScore = score; }
        
        public String getScoreGrade() { return scoreGrade; }
        public void setScoreGrade(String grade) { this.scoreGrade = grade; }
        
        public long getAnalysisTime() { return analysisTime; }
        
        public boolean hasHighSeverityIssues() {
            return issues.stream().anyMatch(issue -> issue.getSeverity() == IssueSeverity.HIGH);
        }
        
        public long getIssueCount(IssueSeverity severity) {
            return issues.stream().filter(issue -> issue.getSeverity() == severity).count();
        }
    }
    
    /**
     * 性能问题
     */
    public static class PerformanceIssue {
        private final IssueSeverity severity;
        private final String title;
        private final String description;
        private final String recommendation;
        private final long timestamp;
        
        public PerformanceIssue(IssueSeverity severity, String title, String description, String recommendation) {
            this.severity = severity;
            this.title = title;
            this.description = description;
            this.recommendation = recommendation;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getters
        public IssueSeverity getSeverity() { return severity; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getRecommendation() { return recommendation; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * 问题严重程度
     */
    public enum IssueSeverity {
        HIGH("高"), MEDIUM("中"), LOW("低");
        
        private final String displayName;
        
        IssueSeverity(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
}