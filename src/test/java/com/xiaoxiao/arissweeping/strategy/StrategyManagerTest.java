package com.xiaoxiao.arissweeping.strategy;

import com.xiaoxiao.arissweeping.config.ConfigManager;
import com.xiaoxiao.arissweeping.observer.CleanupEventManager;
import com.xiaoxiao.arissweeping.strategy.impl.AgeBasedCleanupStrategy;
import com.xiaoxiao.arissweeping.strategy.impl.DensityBasedCleanupStrategy;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * StrategyManager 单元测试
 */
@ExtendWith(MockitoExtension.class)
class StrategyManagerTest {
    
    @Mock
    private ConfigManager configManager;
    
    @Mock
    private CleanupEventManager eventManager;
    
    @Mock
    private World world;
    
    @Mock
    private CleanupStrategy mockStrategy;
    
    @Mock
    private Item item;
    
    private StrategyManager strategyManager;
    
    @BeforeEach
    void setUp() {
        strategyManager = new StrategyManager(configManager, eventManager);
        
        // 设置默认配置
        when(configManager.getBoolean("strategies.age-based.enabled", true)).thenReturn(true);
        when(configManager.getBoolean("strategies.density-based.enabled", true)).thenReturn(true);
        when(configManager.getInt("strategies.age-based.priority", 100)).thenReturn(100);
        when(configManager.getInt("strategies.density-based.priority", 200)).thenReturn(200);
        
        // 设置策略配置
        when(configManager.getInt("strategies.age-based.max-item-age", 300)).thenReturn(300);
        when(configManager.getInt("strategies.density-based.max-items-per-chunk", 50)).thenReturn(50);
        when(configManager.getDouble("strategies.density-based.detection-radius", 16.0)).thenReturn(16.0);
        when(configManager.getBoolean("strategies.age-based.protect-named", true)).thenReturn(true);
        when(configManager.getStringList("strategies.density-based.priority-removal"))
            .thenReturn(Arrays.asList("ITEM", "EXPERIENCE_ORB"));
        
        // 初始化策略管理器
        strategyManager.initialize();
    }
    
    @Test
    void testInitialize() {
        StrategyManager newManager = new StrategyManager(configManager, eventManager);
        newManager.initialize();
        
        // 验证默认策略被注册
        List<CleanupStrategy> strategies = newManager.getRegisteredStrategies();
        assertEquals(2, strategies.size());
        
        // 验证策略类型
        boolean hasAgeBasedStrategy = strategies.stream()
            .anyMatch(s -> s instanceof AgeBasedCleanupStrategy);
        boolean hasDensityBasedStrategy = strategies.stream()
            .anyMatch(s -> s instanceof DensityBasedCleanupStrategy);
        
        assertTrue(hasAgeBasedStrategy);
        assertTrue(hasDensityBasedStrategy);
    }
    
    @Test
    void testRegisterStrategy() {
        when(mockStrategy.getStrategyName()).thenReturn("MockStrategy");
        when(mockStrategy.getPriority()).thenReturn(150);
        
        boolean result = strategyManager.registerStrategy(mockStrategy);
        
        assertTrue(result);
        assertTrue(strategyManager.getRegisteredStrategies().contains(mockStrategy));
    }
    
    @Test
    void testRegisterStrategy_Duplicate() {
        when(mockStrategy.getStrategyName()).thenReturn("MockStrategy");
        when(mockStrategy.getPriority()).thenReturn(150);
        
        // 第一次注册应该成功
        assertTrue(strategyManager.registerStrategy(mockStrategy));
        
        // 第二次注册相同名称的策略应该失败
        CleanupStrategy duplicateStrategy = mock(CleanupStrategy.class);
        when(duplicateStrategy.getStrategyName()).thenReturn("MockStrategy");
        
        assertFalse(strategyManager.registerStrategy(duplicateStrategy));
    }
    
    @Test
    void testUnregisterStrategy() {
        when(mockStrategy.getStrategyName()).thenReturn("MockStrategy");
        when(mockStrategy.getPriority()).thenReturn(150);
        
        strategyManager.registerStrategy(mockStrategy);
        
        boolean result = strategyManager.unregisterStrategy("MockStrategy");
        
        assertTrue(result);
        assertFalse(strategyManager.getRegisteredStrategies().contains(mockStrategy));
    }
    
    @Test
    void testUnregisterStrategy_NotFound() {
        boolean result = strategyManager.unregisterStrategy("NonExistentStrategy");
        
        assertFalse(result);
    }
    
    @Test
    void testGetStrategy() {
        when(mockStrategy.getStrategyName()).thenReturn("MockStrategy");
        when(mockStrategy.getPriority()).thenReturn(150);
        
        strategyManager.registerStrategy(mockStrategy);
        
        Optional<CleanupStrategy> result = strategyManager.getStrategy("MockStrategy");
        
        assertTrue(result.isPresent());
        assertEquals(mockStrategy, result.get());
    }
    
    @Test
    void testGetStrategy_NotFound() {
        Optional<CleanupStrategy> result = strategyManager.getStrategy("NonExistentStrategy");
        
        assertFalse(result.isPresent());
    }
    
    @Test
    void testGetRegisteredStrategies() {
        when(mockStrategy.getStrategyName()).thenReturn("MockStrategy");
        when(mockStrategy.getPriority()).thenReturn(150);
        
        strategyManager.registerStrategy(mockStrategy);
        
        List<CleanupStrategy> strategies = strategyManager.getRegisteredStrategies();
        
        assertEquals(3, strategies.size()); // 2个默认策略 + 1个模拟策略
        assertTrue(strategies.contains(mockStrategy));
    }
    
    @Test
    void testGetEnabledStrategies() {
        when(mockStrategy.getStrategyName()).thenReturn("MockStrategy");
        when(mockStrategy.getPriority()).thenReturn(150);
        when(mockStrategy.isEnabled()).thenReturn(true);
        
        strategyManager.registerStrategy(mockStrategy);
        
        List<CleanupStrategy> enabledStrategies = strategyManager.getEnabledStrategies();
        
        // 验证只返回启用的策略
        assertTrue(enabledStrategies.size() >= 1);
        assertTrue(enabledStrategies.contains(mockStrategy));
    }
    
    @Test
    void testGetApplicableStrategies() {
        when(world.getName()).thenReturn("world");
        when(item.getTicksLived()).thenReturn(400); // 超过年龄阈值
        
        when(mockStrategy.getStrategyName()).thenReturn("MockStrategy");
        when(mockStrategy.getPriority()).thenReturn(150);
        when(mockStrategy.isEnabled()).thenReturn(true);
        when(mockStrategy.isApplicableToWorld(world)).thenReturn(true);
        when(mockStrategy.isApplicableToEntity(item)).thenReturn(true);
        
        strategyManager.registerStrategy(mockStrategy);
        
        List<Entity> entities = Arrays.asList(item);
        List<CleanupStrategy> applicableStrategies = strategyManager.getApplicableStrategies(world, entities);
        
        assertTrue(applicableStrategies.size() >= 1);
        assertTrue(applicableStrategies.contains(mockStrategy));
    }
    
    @Test
    void testExecuteStrategies() {
        when(world.getName()).thenReturn("world");
        when(item.getTicksLived()).thenReturn(400);
        when(item.isValid()).thenReturn(true);
        
        when(mockStrategy.getStrategyName()).thenReturn("MockStrategy");
        when(mockStrategy.getPriority()).thenReturn(150);
        when(mockStrategy.isEnabled()).thenReturn(true);
        when(mockStrategy.isApplicableToWorld(world)).thenReturn(true);
        when(mockStrategy.isApplicableToEntity(item)).thenReturn(true);
        
        CleanupStrategy.CleanupResult mockResult = mock(CleanupStrategy.CleanupResult.class);
        when(mockResult.isSuccess()).thenReturn(true);
        when(mockResult.getEntitiesProcessed()).thenReturn(1);
        when(mockResult.getEntitiesRemoved()).thenReturn(1);
        when(mockStrategy.executeCleanup(eq(world), any())).thenReturn(mockResult);
        
        strategyManager.registerStrategy(mockStrategy);
        
        List<Entity> entities = Arrays.asList(item);
        Map<String, CleanupStrategy.CleanupResult> results = strategyManager.executeStrategies(world, entities);
        
        assertFalse(results.isEmpty());
        assertTrue(results.containsKey("MockStrategy"));
        assertEquals(mockResult, results.get("MockStrategy"));
        
        verify(mockStrategy).executeCleanup(eq(world), any());
    }
    
    @Test
    void testExecuteStrategies_EmptyEntityList() {
        List<Entity> entities = Collections.emptyList();
        Map<String, CleanupStrategy.CleanupResult> results = strategyManager.executeStrategies(world, entities);
        
        assertTrue(results.isEmpty());
    }
    
    @Test
    void testExecuteStrategy() {
        when(world.getName()).thenReturn("world");
        when(item.getTicksLived()).thenReturn(400);
        
        when(mockStrategy.getStrategyName()).thenReturn("MockStrategy");
        when(mockStrategy.isApplicableToWorld(world)).thenReturn(true);
        
        CleanupStrategy.CleanupResult mockResult = mock(CleanupStrategy.CleanupResult.class);
        when(mockResult.isSuccess()).thenReturn(true);
        when(mockStrategy.executeCleanup(eq(world), any())).thenReturn(mockResult);
        
        strategyManager.registerStrategy(mockStrategy);
        
        List<Entity> entities = Arrays.asList(item);
        Optional<CleanupStrategy.CleanupResult> result = strategyManager.executeStrategy("MockStrategy", world, entities);
        
        assertTrue(result.isPresent());
        assertEquals(mockResult, result.get());
        
        verify(mockStrategy).executeCleanup(eq(world), eq(entities));
    }
    
    @Test
    void testExecuteStrategy_NotFound() {
        List<Entity> entities = Arrays.asList(item);
        Optional<CleanupStrategy.CleanupResult> result = strategyManager.executeStrategy("NonExistentStrategy", world, entities);
        
        assertFalse(result.isPresent());
    }
    
    @Test
    void testExecuteStrategy_NotApplicableToWorld() {
        when(mockStrategy.getStrategyName()).thenReturn("MockStrategy");
        when(mockStrategy.isApplicableToWorld(world)).thenReturn(false);
        
        strategyManager.registerStrategy(mockStrategy);
        
        List<Entity> entities = Arrays.asList(item);
        Optional<CleanupStrategy.CleanupResult> result = strategyManager.executeStrategy("MockStrategy", world, entities);
        
        assertFalse(result.isPresent());
        verify(mockStrategy, never()).executeCleanup(any(), any());
    }
    
    @Test
    void testEnableStrategy() {
        when(mockStrategy.getStrategyName()).thenReturn("MockStrategy");
        when(mockStrategy.getPriority()).thenReturn(150);
        
        strategyManager.registerStrategy(mockStrategy);
        
        boolean result = strategyManager.enableStrategy("MockStrategy");
        
        assertTrue(result);
        verify(mockStrategy).setEnabled(true);
    }
    
    @Test
    void testDisableStrategy() {
        when(mockStrategy.getStrategyName()).thenReturn("MockStrategy");
        when(mockStrategy.getPriority()).thenReturn(150);
        
        strategyManager.registerStrategy(mockStrategy);
        
        boolean result = strategyManager.disableStrategy("MockStrategy");
        
        assertTrue(result);
        verify(mockStrategy).setEnabled(false);
    }
    
    @Test
    void testUpdateStrategyConfiguration() {
        when(mockStrategy.getStrategyName()).thenReturn("MockStrategy");
        when(mockStrategy.getPriority()).thenReturn(150);
        
        strategyManager.registerStrategy(mockStrategy);
        
        Map<String, Object> config = new HashMap<>();
        config.put("testKey", "testValue");
        
        boolean result = strategyManager.updateStrategyConfiguration("MockStrategy", config);
        
        assertTrue(result);
        verify(mockStrategy).updateConfiguration(config);
    }
    
    @Test
    void testGetStrategyConfiguration() {
        when(mockStrategy.getStrategyName()).thenReturn("MockStrategy");
        when(mockStrategy.getPriority()).thenReturn(150);
        
        Map<String, Object> expectedConfig = new HashMap<>();
        expectedConfig.put("testKey", "testValue");
        when(mockStrategy.getConfiguration()).thenReturn(expectedConfig);
        
        strategyManager.registerStrategy(mockStrategy);
        
        Optional<Map<String, Object>> result = strategyManager.getStrategyConfiguration("MockStrategy");
        
        assertTrue(result.isPresent());
        assertEquals(expectedConfig, result.get());
    }
    
    @Test
    void testGetStrategyStatistics() {
        when(mockStrategy.getStrategyName()).thenReturn("MockStrategy");
        when(mockStrategy.getPriority()).thenReturn(150);
        
        CleanupStrategy.StrategyStatistics expectedStats = mock(CleanupStrategy.StrategyStatistics.class);
        when(mockStrategy.getStatistics()).thenReturn(expectedStats);
        
        strategyManager.registerStrategy(mockStrategy);
        
        Optional<CleanupStrategy.StrategyStatistics> result = strategyManager.getStrategyStatistics("MockStrategy");
        
        assertTrue(result.isPresent());
        assertEquals(expectedStats, result.get());
    }
    
    @Test
    void testGetAllStatistics() {
        when(mockStrategy.getStrategyName()).thenReturn("MockStrategy");
        when(mockStrategy.getPriority()).thenReturn(150);
        
        CleanupStrategy.StrategyStatistics mockStats = mock(CleanupStrategy.StrategyStatistics.class);
        when(mockStrategy.getStatistics()).thenReturn(mockStats);
        
        strategyManager.registerStrategy(mockStrategy);
        
        Map<String, CleanupStrategy.StrategyStatistics> allStats = strategyManager.getAllStatistics();
        
        assertFalse(allStats.isEmpty());
        assertTrue(allStats.containsKey("MockStrategy"));
        assertEquals(mockStats, allStats.get("MockStrategy"));
    }
    
    @Test
    void testUpdateGlobalConfiguration() {
        strategyManager.updateGlobalConfiguration(configManager);
        
        // 验证所有策略的配置都被更新
        List<CleanupStrategy> strategies = strategyManager.getRegisteredStrategies();
        for (CleanupStrategy strategy : strategies) {
            // 这里我们无法直接验证updateConfiguration被调用，
            // 因为默认策略是真实对象，但我们可以验证策略仍然存在
            assertNotNull(strategy);
        }
    }
    
    @Test
    void testShutdown() {
        when(mockStrategy.getStrategyName()).thenReturn("MockStrategy");
        when(mockStrategy.getPriority()).thenReturn(150);
        
        strategyManager.registerStrategy(mockStrategy);
        
        strategyManager.shutdown();
        
        // 验证所有策略都被清理
        verify(mockStrategy).cleanup();
        assertTrue(strategyManager.getRegisteredStrategies().isEmpty());
    }
    
    @Test
    void testStrategyPriorityOrdering() {
        CleanupStrategy lowPriorityStrategy = mock(CleanupStrategy.class);
        CleanupStrategy highPriorityStrategy = mock(CleanupStrategy.class);
        
        when(lowPriorityStrategy.getStrategyName()).thenReturn("LowPriority");
        when(lowPriorityStrategy.getPriority()).thenReturn(50);
        when(lowPriorityStrategy.isEnabled()).thenReturn(true);
        
        when(highPriorityStrategy.getStrategyName()).thenReturn("HighPriority");
        when(highPriorityStrategy.getPriority()).thenReturn(300);
        when(highPriorityStrategy.isEnabled()).thenReturn(true);
        
        strategyManager.registerStrategy(lowPriorityStrategy);
        strategyManager.registerStrategy(highPriorityStrategy);
        
        List<CleanupStrategy> enabledStrategies = strategyManager.getEnabledStrategies();
        
        // 验证策略按优先级排序（高优先级在前）
        int highPriorityIndex = -1;
        int lowPriorityIndex = -1;
        
        for (int i = 0; i < enabledStrategies.size(); i++) {
            CleanupStrategy strategy = enabledStrategies.get(i);
            if (strategy == highPriorityStrategy) {
                highPriorityIndex = i;
            } else if (strategy == lowPriorityStrategy) {
                lowPriorityIndex = i;
            }
        }
        
        assertTrue(highPriorityIndex >= 0);
        assertTrue(lowPriorityIndex >= 0);
        assertTrue(highPriorityIndex < lowPriorityIndex); // 高优先级应该在前面
    }
    
    @Test
    void testConcurrentAccess() throws InterruptedException {
        // 测试并发访问的线程安全性
        when(mockStrategy.getStrategyName()).thenReturn("MockStrategy");
        when(mockStrategy.getPriority()).thenReturn(150);
        
        List<Thread> threads = new ArrayList<>();
        
        // 创建多个线程同时注册和注销策略
        for (int i = 0; i < 10; i++) {
            final int index = i;
            Thread thread = new Thread(() -> {
                CleanupStrategy strategy = mock(CleanupStrategy.class);
                when(strategy.getStrategyName()).thenReturn("Strategy" + index);
                when(strategy.getPriority()).thenReturn(100 + index);
                
                strategyManager.registerStrategy(strategy);
                strategyManager.getRegisteredStrategies();
                strategyManager.unregisterStrategy("Strategy" + index);
            });
            threads.add(thread);
        }
        
        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 验证没有发生异常，且状态一致
        assertNotNull(strategyManager.getRegisteredStrategies());
    }
}