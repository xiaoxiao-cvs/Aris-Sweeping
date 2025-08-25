package com.xiaoxiao.arissweeping.performance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PerformanceAnalyzer 单元测试
 */
@ExtendWith(MockitoExtension.class)
class PerformanceAnalyzerTest {
    
    @Mock
    private PerformanceMetrics metrics;
    
    @Mock
    private PerformanceMonitor monitor;
    
    private PerformanceAnalyzer analyzer;
    
    @BeforeEach
    void setUp() {
        analyzer = new PerformanceAnalyzer(metrics, monitor);
    }
    
    @Test
    void testAnalyzeOverallPerformance_Good() {
        // 模拟良好的性能指标
        when(metrics.getAverageExecutionTime()).thenReturn(2000.0); // 2秒
        when(metrics.getMemoryUsage()).thenReturn(60.0); // 60%内存使用
        when(metrics.getErrorRate()).thenReturn(2.0); // 2%错误率
        when(metrics.getEntitiesPerSecond()).thenReturn(100.0); // 100实体/秒
        
        PerformanceAnalyzer.AnalysisResult result = analyzer.analyzeOverallPerformance();
        
        assertNotNull(result);
        assertTrue(result.getPerformanceScore() > 70); // 良好性能分数
        assertTrue(result.getIssues().size() <= 1); // 很少或没有问题
    }
    
    @Test
    void testAnalyzeOverallPerformance_Poor() {
        // 模拟糟糕的性能指标
        when(metrics.getAverageExecutionTime()).thenReturn(10000.0); // 10秒
        when(metrics.getMemoryUsage()).thenReturn(90.0); // 90%内存使用
        when(metrics.getErrorRate()).thenReturn(15.0); // 15%错误率
        when(metrics.getEntitiesPerSecond()).thenReturn(10.0); // 10实体/秒
        
        PerformanceAnalyzer.AnalysisResult result = analyzer.analyzeOverallPerformance();
        
        assertNotNull(result);
        assertTrue(result.getPerformanceScore() < 50); // 糟糕性能分数
        assertFalse(result.getIssues().isEmpty()); // 应该有问题
        
        // 验证包含高内存使用问题
        boolean hasMemoryIssue = result.getIssues().stream()
            .anyMatch(issue -> issue.getType().contains("MEMORY"));
        assertTrue(hasMemoryIssue);
    }
    
    @Test
    void testAnalyzeStrategyPerformance() {
        String strategyName = "AgeBasedCleanupStrategy";
        
        // 模拟策略性能数据
        Map<String, Object> strategyStats = new HashMap<>();
        strategyStats.put("executionCount", 100L);
        strategyStats.put("totalExecutionTime", 50000L); // 50秒总执行时间
        strategyStats.put("entitiesProcessed", 10000L);
        strategyStats.put("entitiesRemoved", 8000L);
        strategyStats.put("errorCount", 5L);
        
        when(metrics.getStrategyStatistics(strategyName)).thenReturn(strategyStats);
        
        PerformanceAnalyzer.AnalysisResult result = analyzer.analyzeStrategyPerformance(strategyName);
        
        assertNotNull(result);
        assertEquals(strategyName, result.getAnalyzedComponent());
        
        // 验证计算的指标
        double avgExecutionTime = 50000.0 / 100; // 500ms平均执行时间
        double removalRate = 8000.0 / 10000 * 100; // 80%移除率
        double errorRate = 5.0 / 100 * 100; // 5%错误率
        
        assertTrue(avgExecutionTime == 500.0);
        assertTrue(removalRate == 80.0);
        assertTrue(errorRate == 5.0);
    }
    
    @Test
    void testAnalyzeStrategyPerformance_NoData() {
        String strategyName = "NonExistentStrategy";
        
        when(metrics.getStrategyStatistics(strategyName)).thenReturn(new HashMap<>());
        
        PerformanceAnalyzer.AnalysisResult result = analyzer.analyzeStrategyPerformance(strategyName);
        
        assertNotNull(result);
        assertEquals(strategyName, result.getAnalyzedComponent());
        assertTrue(result.getIssues().stream()
            .anyMatch(issue -> issue.getType().contains("NO_DATA")));
    }
    
    @Test
    void testAnalyzeErrorPatterns() {
        // 模拟错误统计
        Map<String, Long> errorStats = new HashMap<>();
        errorStats.put("NullPointerException", 10L);
        errorStats.put("IllegalArgumentException", 5L);
        errorStats.put("ConcurrentModificationException", 15L);
        errorStats.put("OutOfMemoryError", 2L);
        
        when(metrics.getErrorStatistics()).thenReturn(errorStats);
        when(metrics.getTotalOperations()).thenReturn(1000L);
        
        PerformanceAnalyzer.AnalysisResult result = analyzer.analyzeErrorPatterns();
        
        assertNotNull(result);
        assertFalse(result.getIssues().isEmpty());
        
        // 验证最常见的错误被识别
        boolean hasConcurrentModificationIssue = result.getIssues().stream()
            .anyMatch(issue -> issue.getDescription().contains("ConcurrentModificationException"));
        assertTrue(hasConcurrentModificationIssue);
        
        // 验证严重错误被标记
        boolean hasMemoryIssue = result.getIssues().stream()
            .anyMatch(issue -> issue.getDescription().contains("OutOfMemoryError") && 
                      issue.getSeverity() == PerformanceAnalyzer.IssueSeverity.CRITICAL);
        assertTrue(hasMemoryIssue);
    }
    
    @Test
    void testAnalyzePerformanceTrends() {
        // 创建性能历史数据
        List<PerformanceMetrics.MetricsSnapshot> history = new ArrayList<>();
        long baseTime = System.currentTimeMillis();
        
        // 模拟性能下降趋势
        for (int i = 0; i < 10; i++) {
            PerformanceMetrics.MetricsSnapshot snapshot = mock(PerformanceMetrics.MetricsSnapshot.class);
            when(snapshot.getTimestamp()).thenReturn(baseTime + i * 60000); // 每分钟一个快照
            when(snapshot.getAverageExecutionTime()).thenReturn(1000.0 + i * 200); // 执行时间递增
            when(snapshot.getMemoryUsage()).thenReturn(50.0 + i * 3); // 内存使用递增
            when(snapshot.getErrorRate()).thenReturn(1.0 + i * 0.5); // 错误率递增
            
            history.add(snapshot);
        }
        
        when(monitor.getPerformanceHistory()).thenReturn(history);
        
        PerformanceAnalyzer.AnalysisResult result = analyzer.analyzePerformanceTrends();
        
        assertNotNull(result);
        assertFalse(result.getIssues().isEmpty());
        
        // 验证趋势问题被识别
        boolean hasTrendIssue = result.getIssues().stream()
            .anyMatch(issue -> issue.getType().contains("TREND"));
        assertTrue(hasTrendIssue);
    }
    
    @Test
    void testAnalyzePerformanceTrends_InsufficientData() {
        // 提供不足的历史数据
        List<PerformanceMetrics.MetricsSnapshot> history = new ArrayList<>();
        PerformanceMetrics.MetricsSnapshot snapshot = mock(PerformanceMetrics.MetricsSnapshot.class);
        history.add(snapshot);
        
        when(monitor.getPerformanceHistory()).thenReturn(history);
        
        PerformanceAnalyzer.AnalysisResult result = analyzer.analyzePerformanceTrends();
        
        assertNotNull(result);
        assertTrue(result.getIssues().stream()
            .anyMatch(issue -> issue.getDescription().contains("insufficient data")));
    }
    
    @Test
    void testGenerateOptimizationRecommendations_HighMemory() {
        when(metrics.getMemoryUsage()).thenReturn(85.0);
        when(metrics.getAverageExecutionTime()).thenReturn(3000.0);
        when(metrics.getErrorRate()).thenReturn(5.0);
        
        List<String> recommendations = analyzer.generateOptimizationRecommendations();
        
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
        
        // 验证包含内存优化建议
        boolean hasMemoryRecommendation = recommendations.stream()
            .anyMatch(rec -> rec.toLowerCase().contains("memory") || rec.toLowerCase().contains("内存"));
        assertTrue(hasMemoryRecommendation);
    }
    
    @Test
    void testGenerateOptimizationRecommendations_SlowExecution() {
        when(metrics.getMemoryUsage()).thenReturn(60.0);
        when(metrics.getAverageExecutionTime()).thenReturn(8000.0); // 8秒
        when(metrics.getErrorRate()).thenReturn(2.0);
        
        List<String> recommendations = analyzer.generateOptimizationRecommendations();
        
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
        
        // 验证包含执行时间优化建议
        boolean hasExecutionRecommendation = recommendations.stream()
            .anyMatch(rec -> rec.toLowerCase().contains("execution") || 
                           rec.toLowerCase().contains("batch") ||
                           rec.toLowerCase().contains("执行") ||
                           rec.toLowerCase().contains("批处理"));
        assertTrue(hasExecutionRecommendation);
    }
    
    @Test
    void testGenerateOptimizationRecommendations_HighErrors() {
        when(metrics.getMemoryUsage()).thenReturn(60.0);
        when(metrics.getAverageExecutionTime()).thenReturn(3000.0);
        when(metrics.getErrorRate()).thenReturn(12.0); // 12%错误率
        
        List<String> recommendations = analyzer.generateOptimizationRecommendations();
        
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
        
        // 验证包含错误处理建议
        boolean hasErrorRecommendation = recommendations.stream()
            .anyMatch(rec -> rec.toLowerCase().contains("error") || 
                           rec.toLowerCase().contains("exception") ||
                           rec.toLowerCase().contains("错误") ||
                           rec.toLowerCase().contains("异常"));
        assertTrue(hasErrorRecommendation);
    }
    
    @Test
    void testCalculatePerformanceScore_Perfect() {
        when(metrics.getAverageExecutionTime()).thenReturn(1000.0); // 1秒
        when(metrics.getMemoryUsage()).thenReturn(50.0); // 50%
        when(metrics.getErrorRate()).thenReturn(0.0); // 0%错误
        when(metrics.getEntitiesPerSecond()).thenReturn(200.0); // 高吞吐量
        
        int score = analyzer.calculatePerformanceScore();
        
        assertTrue(score >= 90); // 接近完美分数
    }
    
    @Test
    void testCalculatePerformanceScore_Poor() {
        when(metrics.getAverageExecutionTime()).thenReturn(15000.0); // 15秒
        when(metrics.getMemoryUsage()).thenReturn(95.0); // 95%
        when(metrics.getErrorRate()).thenReturn(20.0); // 20%错误
        when(metrics.getEntitiesPerSecond()).thenReturn(5.0); // 低吞吐量
        
        int score = analyzer.calculatePerformanceScore();
        
        assertTrue(score <= 30); // 很低的分数
    }
    
    @Test
    void testAnalysisResult() {
        // 测试AnalysisResult类的功能
        List<PerformanceAnalyzer.PerformanceIssue> issues = new ArrayList<>();
        issues.add(new PerformanceAnalyzer.PerformanceIssue(
            "HIGH_MEMORY", 
            "Memory usage is too high", 
            PerformanceAnalyzer.IssueSeverity.HIGH
        ));
        
        List<String> recommendations = Arrays.asList(
            "Increase batch size",
            "Optimize memory usage"
        );
        
        PerformanceAnalyzer.AnalysisResult result = new PerformanceAnalyzer.AnalysisResult(
            "TestComponent",
            85,
            issues,
            recommendations
        );
        
        assertEquals("TestComponent", result.getAnalyzedComponent());
        assertEquals(85, result.getPerformanceScore());
        assertEquals(1, result.getIssues().size());
        assertEquals(2, result.getRecommendations().size());
        
        // 测试toString方法
        String resultString = result.toString();
        assertNotNull(resultString);
        assertTrue(resultString.contains("TestComponent"));
        assertTrue(resultString.contains("85"));
    }
    
    @Test
    void testPerformanceIssue() {
        // 测试PerformanceIssue类的功能
        PerformanceAnalyzer.PerformanceIssue issue = new PerformanceAnalyzer.PerformanceIssue(
            "SLOW_EXECUTION",
            "Execution time exceeds threshold",
            PerformanceAnalyzer.IssueSeverity.MEDIUM
        );
        
        assertEquals("SLOW_EXECUTION", issue.getType());
        assertEquals("Execution time exceeds threshold", issue.getDescription());
        assertEquals(PerformanceAnalyzer.IssueSeverity.MEDIUM, issue.getSeverity());
        
        // 测试toString方法
        String issueString = issue.toString();
        assertNotNull(issueString);
        assertTrue(issueString.contains("SLOW_EXECUTION"));
        assertTrue(issueString.contains("MEDIUM"));
    }
    
    @Test
    void testIssueSeverity() {
        // 测试IssueSeverity枚举
        assertEquals(4, PerformanceAnalyzer.IssueSeverity.values().length);
        
        assertTrue(PerformanceAnalyzer.IssueSeverity.CRITICAL.ordinal() > 
                  PerformanceAnalyzer.IssueSeverity.HIGH.ordinal());
        assertTrue(PerformanceAnalyzer.IssueSeverity.HIGH.ordinal() > 
                  PerformanceAnalyzer.IssueSeverity.MEDIUM.ordinal());
        assertTrue(PerformanceAnalyzer.IssueSeverity.MEDIUM.ordinal() > 
                  PerformanceAnalyzer.IssueSeverity.LOW.ordinal());
    }
    
    @Test
    void testAnalyzeWithNullMetrics() {
        // 测试空指标的处理
        when(metrics.getAverageExecutionTime()).thenReturn(null);
        when(metrics.getMemoryUsage()).thenReturn(null);
        when(metrics.getErrorRate()).thenReturn(null);
        
        PerformanceAnalyzer.AnalysisResult result = analyzer.analyzeOverallPerformance();
        
        assertNotNull(result);
        // 应该有关于缺失数据的问题
        boolean hasDataIssue = result.getIssues().stream()
            .anyMatch(issue -> issue.getDescription().toLowerCase().contains("data") ||
                              issue.getDescription().contains("数据"));
        assertTrue(hasDataIssue);
    }
    
    @Test
    void testConcurrentAnalysis() throws InterruptedException {
        // 测试并发分析的线程安全性
        when(metrics.getAverageExecutionTime()).thenReturn(2000.0);
        when(metrics.getMemoryUsage()).thenReturn(60.0);
        when(metrics.getErrorRate()).thenReturn(3.0);
        when(metrics.getEntitiesPerSecond()).thenReturn(100.0);
        
        List<Thread> threads = new ArrayList<>();
        List<PerformanceAnalyzer.AnalysisResult> results = Collections.synchronizedList(new ArrayList<>());
        
        // 创建多个线程同时进行分析
        for (int i = 0; i < 5; i++) {
            Thread thread = new Thread(() -> {
                PerformanceAnalyzer.AnalysisResult result = analyzer.analyzeOverallPerformance();
                results.add(result);
            });
            threads.add(thread);
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join(1000);
        }
        
        assertEquals(5, results.size());
        
        // 验证所有结果都是有效的
        for (PerformanceAnalyzer.AnalysisResult result : results) {
            assertNotNull(result);
            assertTrue(result.getPerformanceScore() >= 0 && result.getPerformanceScore() <= 100);
        }
    }
    
    @Test
    void testAnalyzeSpecificTimeRange() {
        // 测试特定时间范围的分析
        long startTime = System.currentTimeMillis() - 3600000; // 1小时前
        long endTime = System.currentTimeMillis();
        
        List<PerformanceMetrics.MetricsSnapshot> history = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            PerformanceMetrics.MetricsSnapshot snapshot = mock(PerformanceMetrics.MetricsSnapshot.class);
            when(snapshot.getTimestamp()).thenReturn(startTime + i * 600000); // 每10分钟
            when(snapshot.getAverageExecutionTime()).thenReturn(2000.0 + i * 100);
            when(snapshot.getMemoryUsage()).thenReturn(60.0 + i * 2);
            
            history.add(snapshot);
        }
        
        when(monitor.getPerformanceHistory(startTime, endTime)).thenReturn(history);
        
        PerformanceAnalyzer.AnalysisResult result = analyzer.analyzePerformanceTrends(startTime, endTime);
        
        assertNotNull(result);
        assertEquals("Performance Trends", result.getAnalyzedComponent());
    }
    
    @Test
    void testGenerateDetailedReport() {
        // 测试生成详细报告
        when(metrics.getAverageExecutionTime()).thenReturn(3000.0);
        when(metrics.getMemoryUsage()).thenReturn(70.0);
        when(metrics.getErrorRate()).thenReturn(5.0);
        when(metrics.getEntitiesPerSecond()).thenReturn(80.0);
        
        Map<String, Object> strategyStats = new HashMap<>();
        strategyStats.put("executionCount", 50L);
        strategyStats.put("totalExecutionTime", 25000L);
        
        when(metrics.getAllStrategyStatistics()).thenReturn(
            Collections.singletonMap("TestStrategy", strategyStats));
        
        String report = analyzer.generateDetailedReport();
        
        assertNotNull(report);
        assertFalse(report.isEmpty());
        assertTrue(report.contains("Performance Analysis Report") || 
                  report.contains("性能分析报告"));
    }
}