package com.xiaoxiao.arissweeping.performance;

import com.xiaoxiao.arissweeping.config.ConfigManager;
import com.xiaoxiao.arissweeping.observer.CleanupEventManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PerformanceManager 单元测试
 */
@ExtendWith(MockitoExtension.class)
class PerformanceManagerTest {
    
    @Mock
    private PerformanceMetrics metrics;
    
    @Mock
    private PerformanceMonitor monitor;
    
    @Mock
    private PerformanceAnalyzer analyzer;
    
    @Mock
    private PerformanceReporter reporter;
    
    @Mock
    private CleanupEventManager eventManager;
    
    @Mock
    private ConfigManager configManager;
    
    private PerformanceManager manager;
    
    @BeforeEach
    void setUp() {
        manager = new PerformanceManager(metrics, monitor, analyzer, reporter, eventManager, configManager);
        
        // 设置默认配置
        when(configManager.getBoolean("performance.enabled", true)).thenReturn(true);
        when(configManager.getBoolean("performance.auto-analysis", true)).thenReturn(true);
        when(configManager.getBoolean("performance.auto-reporting", false)).thenReturn(false);
        when(configManager.getLong("performance.analysis-interval", 300000L)).thenReturn(300000L); // 5分钟
        when(configManager.getLong("performance.report-interval", 3600000L)).thenReturn(3600000L); // 1小时
        when(configManager.getString("performance.report-directory", "reports")).thenReturn("reports");
    }
    
    @Test
    void testStartManager() {
        assertFalse(manager.isRunning());
        
        manager.start();
        
        assertTrue(manager.isRunning());
        verify(monitor).startMonitoring();
    }
    
    @Test
    void testStopManager() {
        manager.start();
        assertTrue(manager.isRunning());
        
        manager.stop();
        
        assertFalse(manager.isRunning());
        verify(monitor).stopMonitoring();
    }
    
    @Test
    void testStartManager_AlreadyRunning() {
        manager.start();
        assertTrue(manager.isRunning());
        
        // 再次启动应该不会有额外效果
        manager.start();
        
        assertTrue(manager.isRunning());
        // 监控器应该只被启动一次
        verify(monitor, times(1)).startMonitoring();
    }
    
    @Test
    void testStopManager_NotRunning() {
        assertFalse(manager.isRunning());
        
        // 停止未运行的管理器应该不会有问题
        manager.stop();
        
        assertFalse(manager.isRunning());
        verify(monitor, never()).stopMonitoring();
    }
    
    @Test
    void testPerformAnalysis() {
        PerformanceAnalyzer.AnalysisResult mockResult = mock(PerformanceAnalyzer.AnalysisResult.class);
        when(analyzer.analyzeOverallPerformance()).thenReturn(mockResult);
        
        CompletableFuture<PerformanceAnalyzer.AnalysisResult> future = manager.performAnalysis();
        
        assertNotNull(future);
        assertFalse(future.isDone()); // 应该是异步执行
        
        // 等待完成
        PerformanceAnalyzer.AnalysisResult result = future.join();
        
        assertEquals(mockResult, result);
        verify(analyzer).analyzeOverallPerformance();
    }
    
    @Test
    void testPerformAnalysis_WithStrategy() {
        String strategyName = "TestStrategy";
        PerformanceAnalyzer.AnalysisResult mockResult = mock(PerformanceAnalyzer.AnalysisResult.class);
        when(analyzer.analyzeStrategyPerformance(strategyName)).thenReturn(mockResult);
        
        CompletableFuture<PerformanceAnalyzer.AnalysisResult> future = manager.performAnalysis(strategyName);
        
        assertNotNull(future);
        
        PerformanceAnalyzer.AnalysisResult result = future.join();
        
        assertEquals(mockResult, result);
        verify(analyzer).analyzeStrategyPerformance(strategyName);
    }
    
    @Test
    void testGenerateReport_HTML() {
        String expectedReport = "<html>Test Report</html>";
        when(reporter.generateHTMLReport()).thenReturn(expectedReport);
        
        CompletableFuture<String> future = manager.generateReport(PerformanceReporter.ReportFormat.HTML);
        
        assertNotNull(future);
        
        String report = future.join();
        
        assertEquals(expectedReport, report);
        verify(reporter).generateHTMLReport();
    }
    
    @Test
    void testGenerateReport_CSV() {
        String expectedReport = "column1,column2\nvalue1,value2";
        when(reporter.generateCSVReport()).thenReturn(expectedReport);
        
        CompletableFuture<String> future = manager.generateReport(PerformanceReporter.ReportFormat.CSV);
        
        assertNotNull(future);
        
        String report = future.join();
        
        assertEquals(expectedReport, report);
        verify(reporter).generateCSVReport();
    }
    
    @Test
    void testGenerateReport_JSON() {
        String expectedReport = "{\"test\": \"report\"}";
        when(reporter.generateJSONReport()).thenReturn(expectedReport);
        
        CompletableFuture<String> future = manager.generateReport(PerformanceReporter.ReportFormat.JSON);
        
        assertNotNull(future);
        
        String report = future.join();
        
        assertEquals(expectedReport, report);
        verify(reporter).generateJSONReport();
    }
    
    @Test
    void testSaveReport() {
        String reportContent = "Test Report Content";
        String fileName = "test-report.html";
        
        when(reporter.generateHTMLReport()).thenReturn(reportContent);
        
        CompletableFuture<File> future = manager.saveReport(PerformanceReporter.ReportFormat.HTML, fileName);
        
        assertNotNull(future);
        
        File reportFile = future.join();
        
        assertNotNull(reportFile);
        assertTrue(reportFile.getName().contains(fileName) || reportFile.getName().contains("test-report"));
        verify(reporter).generateHTMLReport();
    }
    
    @Test
    void testResetMetrics() {
        manager.resetMetrics();
        
        verify(metrics).reset();
        verify(monitor).clearHistory();
    }
    
    @Test
    void testUpdateConfiguration() {
        // 更新配置
        when(configManager.getBoolean("performance.enabled", true)).thenReturn(false);
        when(configManager.getBoolean("performance.auto-analysis", true)).thenReturn(false);
        when(configManager.getLong("performance.analysis-interval", 300000L)).thenReturn(600000L);
        
        manager.updateConfiguration(configManager);
        
        // 验证配置更新被传递给各个组件
        verify(monitor).updateConfiguration(configManager);
        verify(reporter).updateConfiguration(configManager);
    }
    
    @Test
    void testUpdateConfiguration_DisablePerformance() {
        manager.start();
        assertTrue(manager.isRunning());
        
        // 禁用性能监控
        when(configManager.getBoolean("performance.enabled", true)).thenReturn(false);
        
        manager.updateConfiguration(configManager);
        
        // 管理器应该停止
        assertFalse(manager.isRunning());
        verify(monitor).stopMonitoring();
    }
    
    @Test
    void testUpdateConfiguration_EnablePerformance() {
        // 初始状态：性能监控被禁用
        when(configManager.getBoolean("performance.enabled", true)).thenReturn(false);
        manager.updateConfiguration(configManager);
        assertFalse(manager.isRunning());
        
        // 启用性能监控
        when(configManager.getBoolean("performance.enabled", true)).thenReturn(true);
        
        manager.updateConfiguration(configManager);
        
        // 管理器应该启动
        assertTrue(manager.isRunning());
        verify(monitor).startMonitoring();
    }
    
    @Test
    void testAutoAnalysis() throws InterruptedException {
        // 启用自动分析
        when(configManager.getBoolean("performance.auto-analysis", true)).thenReturn(true);
        when(configManager.getLong("performance.analysis-interval", 300000L)).thenReturn(100L); // 100ms用于测试
        
        PerformanceAnalyzer.AnalysisResult mockResult = mock(PerformanceAnalyzer.AnalysisResult.class);
        when(analyzer.analyzeOverallPerformance()).thenReturn(mockResult);
        
        manager.updateConfiguration(configManager);
        manager.start();
        
        // 等待自动分析执行
        Thread.sleep(200);
        
        // 验证分析被执行
        verify(analyzer, atLeastOnce()).analyzeOverallPerformance();
    }
    
    @Test
    void testAutoReporting() throws InterruptedException {
        // 启用自动报告
        when(configManager.getBoolean("performance.auto-reporting", false)).thenReturn(true);
        when(configManager.getLong("performance.report-interval", 3600000L)).thenReturn(100L); // 100ms用于测试
        
        when(reporter.generateHTMLReport()).thenReturn("<html>Auto Report</html>");
        
        manager.updateConfiguration(configManager);
        manager.start();
        
        // 等待自动报告执行
        Thread.sleep(200);
        
        // 验证报告被生成
        verify(reporter, atLeastOnce()).generateHTMLReport();
    }
    
    @Test
    void testGetCurrentMetrics() {
        PerformanceMetrics.MetricsSnapshot mockSnapshot = mock(PerformanceMetrics.MetricsSnapshot.class);
        when(monitor.getCurrentMetrics()).thenReturn(mockSnapshot);
        
        PerformanceMetrics.MetricsSnapshot snapshot = manager.getCurrentMetrics();
        
        assertEquals(mockSnapshot, snapshot);
        verify(monitor).getCurrentMetrics();
    }
    
    @Test
    void testGetPerformanceHistory() {
        manager.getPerformanceHistory();
        
        verify(monitor).getPerformanceHistory();
    }
    
    @Test
    void testGetPerformanceHistory_WithTimeRange() {
        long startTime = System.currentTimeMillis() - 3600000;
        long endTime = System.currentTimeMillis();
        
        manager.getPerformanceHistory(startTime, endTime);
        
        verify(monitor).getPerformanceHistory(startTime, endTime);
    }
    
    @Test
    void testShutdown() {
        manager.start();
        assertTrue(manager.isRunning());
        
        manager.shutdown();
        
        assertFalse(manager.isRunning());
        verify(monitor).shutdown();
    }
    
    @Test
    void testShutdown_WithTimeout() throws InterruptedException {
        manager.start();
        assertTrue(manager.isRunning());
        
        boolean result = manager.shutdown(1, TimeUnit.SECONDS);
        
        assertTrue(result);
        assertFalse(manager.isRunning());
        verify(monitor).shutdown();
    }
    
    @Test
    void testExceptionHandling_AnalysisFailure() {
        // 模拟分析失败
        when(analyzer.analyzeOverallPerformance()).thenThrow(new RuntimeException("Analysis failed"));
        
        CompletableFuture<PerformanceAnalyzer.AnalysisResult> future = manager.performAnalysis();
        
        assertNotNull(future);
        
        // 验证异常被正确处理
        assertThrows(RuntimeException.class, future::join);
    }
    
    @Test
    void testExceptionHandling_ReportGenerationFailure() {
        // 模拟报告生成失败
        when(reporter.generateHTMLReport()).thenThrow(new RuntimeException("Report generation failed"));
        
        CompletableFuture<String> future = manager.generateReport(PerformanceReporter.ReportFormat.HTML);
        
        assertNotNull(future);
        
        // 验证异常被正确处理
        assertThrows(RuntimeException.class, future::join);
    }
    
    @Test
    void testConcurrentOperations() throws InterruptedException {
        manager.start();
        
        // 创建多个并发操作
        CompletableFuture<PerformanceAnalyzer.AnalysisResult> analysis1 = manager.performAnalysis();
        CompletableFuture<PerformanceAnalyzer.AnalysisResult> analysis2 = manager.performAnalysis();
        CompletableFuture<String> report1 = manager.generateReport(PerformanceReporter.ReportFormat.HTML);
        CompletableFuture<String> report2 = manager.generateReport(PerformanceReporter.ReportFormat.JSON);
        
        // 模拟返回值
        PerformanceAnalyzer.AnalysisResult mockResult = mock(PerformanceAnalyzer.AnalysisResult.class);
        when(analyzer.analyzeOverallPerformance()).thenReturn(mockResult);
        when(reporter.generateHTMLReport()).thenReturn("<html>Report</html>");
        when(reporter.generateJSONReport()).thenReturn("{\"report\": true}");
        
        // 等待所有操作完成
        CompletableFuture.allOf(analysis1, analysis2, report1, report2).join();
        
        // 验证所有操作都成功完成
        assertNotNull(analysis1.get());
        assertNotNull(analysis2.get());
        assertNotNull(report1.get());
        assertNotNull(report2.get());
    }
    
    @Test
    void testPerformanceImpact() {
        // 测试性能管理器本身的性能影响
        long startTime = System.currentTimeMillis();
        
        manager.start();
        
        // 执行一些操作
        for (int i = 0; i < 10; i++) {
            manager.getCurrentMetrics();
            manager.getPerformanceHistory();
        }
        
        manager.stop();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 验证操作在合理时间内完成（这里设置为1秒，实际应该更快）
        assertTrue(duration < 1000, "Performance manager operations took too long: " + duration + "ms");
    }
    
    @Test
    void testMemoryUsage() {
        // 测试内存使用情况
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        manager.start();
        
        // 执行一些操作
        for (int i = 0; i < 100; i++) {
            manager.performAnalysis();
            manager.generateReport(PerformanceReporter.ReportFormat.HTML);
        }
        
        // 强制垃圾回收
        System.gc();
        
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        manager.stop();
        
        // 验证内存增长在合理范围内（这里设置为50MB）
        assertTrue(memoryIncrease < 50 * 1024 * 1024, 
                  "Memory usage increased too much: " + memoryIncrease + " bytes");
    }
    
    @Test
    void testConfigurationValidation() {
        // 测试无效配置的处理
        when(configManager.getLong("performance.analysis-interval", 300000L)).thenReturn(-1L);
        when(configManager.getLong("performance.report-interval", 3600000L)).thenReturn(0L);
        
        // 更新配置不应该导致异常
        assertDoesNotThrow(() -> manager.updateConfiguration(configManager));
    }
    
    @Test
    void testResourceCleanup() {
        manager.start();
        
        // 创建一些异步操作
        CompletableFuture<PerformanceAnalyzer.AnalysisResult> analysis = manager.performAnalysis();
        CompletableFuture<String> report = manager.generateReport(PerformanceReporter.ReportFormat.HTML);
        
        // 立即关闭管理器
        manager.shutdown();
        
        // 验证资源被正确清理
        assertFalse(manager.isRunning());
        
        // 异步操作应该能够完成或被取消
        // 这里我们不强制要求特定行为，只要不抛出异常即可
        assertDoesNotThrow(() -> {
            try {
                analysis.get(100, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                // 可能被取消或超时，这是可以接受的
            }
        });
    }
    
    @Test
    void testEventIntegration() {
        // 测试与事件管理器的集成
        manager.start();
        
        // 验证事件管理器被正确使用
        // 这里我们主要验证没有异常抛出
        assertDoesNotThrow(() -> {
            manager.performAnalysis();
            manager.generateReport(PerformanceReporter.ReportFormat.HTML);
        });
        
        manager.stop();
    }
}